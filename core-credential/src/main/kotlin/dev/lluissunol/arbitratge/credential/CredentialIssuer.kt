package dev.lluissunol.arbitratge.credential

import java.time.LocalDate
import java.util.UUID

/**
 * Dades d'una emissió (§3.7.10).
 *
 * @param nomInput nom tal com l'ha confirmat l'usuari; l'emissor hi aplica [NameRules.normalize]
 *   (idempotent) i usa el resultat al token. La UI ha de mostrar i desar el `nom` retornat a
 *   [IssueResult.Issued.credential] (font única per a QR, PDF i pantalla).
 * @param iat data local del dispositiu en emetre (informativa, "data del dispositiu").
 * @param extensions membres informatius desconeguts per a `v=1` (§3.7.5); per defecte cap. No
 *   poden usar noms coneguts ni reservats.
 * @param ok encerts de l'examen (0..99), opcional. Cal donar-lo juntament amb [tot] (o cap dels
 *   dos) i `ok <= tot`.
 * @param tot total de preguntes de l'examen (1..99), opcional; vegeu [ok].
 * @param dur durada de l'examen en segons (0..86400), opcional i independent.
 */
data class IssueRequest(
    val nomInput: String,
    val exv: String,
    val iat: LocalDate,
    val bv: String? = null,
    val app: String? = null,
    val extensions: Map<String, String> = emptyMap(),
    val ok: Int? = null,
    val tot: Int? = null,
    val dur: Int? = null,
)

/** Error d'emissió: no s'emet cap credencial (ni a mitges). */
sealed interface IssueError {
    data class InvalidName(val reason: NameError) : IssueError
    data class InvalidField(val field: String) : IssueError
    data class ReservedName(val name: String) : IssueError
    data class PayloadTooLarge(val bytes: Int) : IssueError
    data class SealFailed(val message: String) : IssueError
    data class SelfVerificationFailed(val code: VerificationCode) : IssueError
}

sealed interface IssueResult {
    /** [token]: text exacte a desar; mai es re-signa ni es reconstrueix (§3.7.10, pas 6). */
    data class Issued(val token: String, val credential: Arb1Credential) : IssueResult
    data class Rejected(val error: IssueError) : IssueResult
}

/**
 * Emissor ARB1 (§3.7.10). L'autoverificació (pas 5) usa [CredentialVerifier] amb la llista de
 * confiança de l'app, que ha de contenir l'entrada del [kid].
 */
class CredentialIssuer private constructor(
    private val sealer: CredentialSealer,
    private val kid: String,
    trustedKeys: TrustedKeys,
    private val cidGenerator: () -> String,
    private val escaping: PayloadEscaping,
) {
    /** Emissor de producció: serialització recomanada de §3.7.5.2 (no ASCII en UTF-8 directe). */
    constructor(
        sealer: CredentialSealer,
        kid: String,
        trustedKeys: TrustedKeys,
        cidGenerator: () -> String = { UUID.randomUUID().toString() },
    ) : this(sealer, kid, trustedKeys, cidGenerator, PayloadEscaping.NONE)

    private val verifier = CredentialVerifier(trustedKeys)

    init {
        require(Fields.KID.matches(kid)) { "kid fora de patró" }
    }

    fun issue(request: IssueRequest): IssueResult {
        // 1. Nom i data.
        val nom = when (val n = NameRules.normalize(request.nomInput)) {
            is NameResult.Valid -> n.nom
            is NameResult.Invalid -> return reject(IssueError.InvalidName(n.error))
        }
        val iat = formatIat(request.iat) ?: return reject(IssueError.InvalidField("iat"))
        val cid = cidGenerator()
        if (!Fields.CID.matches(cid)) return reject(IssueError.InvalidField("cid"))
        if (!Fields.EXV.matches(request.exv)) return reject(IssueError.InvalidField("exv"))
        if (request.bv != null && !Fields.BV.matches(request.bv)) return reject(IssueError.InvalidField("bv"))
        if (request.app != null && !Fields.APP.matches(request.app)) return reject(IssueError.InvalidField("app"))
        if (request.ok != null && request.ok !in Arb1.OK_RANGE) return reject(IssueError.InvalidField("ok"))
        if (request.tot != null && request.tot !in Arb1.TOT_RANGE) return reject(IssueError.InvalidField("tot"))
        if (!Fields.isValidScore(request.ok, request.tot)) {
            return reject(IssueError.InvalidField(if (request.tot == null) "tot" else "ok"))
        }
        if (request.dur != null && request.dur !in Arb1.DUR_RANGE) return reject(IssueError.InvalidField("dur"))
        for ((name, value) in request.extensions) {
            if (name in Arb1.RESERVED_NAMES) return reject(IssueError.ReservedName(name))
            if (name in Arb1.FIELD_ORDER || !EXTENSION_NAME.matches(name)) return reject(IssueError.InvalidField(name))
            if (value.any { it.code < 0x20 } || NameRules.hasLoneSurrogate(value)) {
                return reject(IssueError.InvalidField(name))
            }
        }
        val fields = linkedMapOf<String, Any>(
            "v" to Arb1.VERSION, "kid" to kid, "cid" to cid, "nom" to nom, "res" to Arb1.RES_APTE,
            "exv" to request.exv, "iat" to iat, "orig" to Arb1.ORIG_AUTOSERVEI,
        )
        request.ok?.let { fields["ok"] = it }
        request.tot?.let { fields["tot"] = it }
        request.dur?.let { fields["dur"] = it }
        request.bv?.let { fields["bv"] = it }
        request.app?.let { fields["app"] = it }
        fields.putAll(request.extensions)
        if (fields.size > Arb1.MAX_MEMBERS) return reject(IssueError.InvalidField("extensions"))

        // 2. Serialització única a bytes UTF-8.
        val payload = StrictUtf8.encode(serialize(fields, escaping)) ?: return reject(IssueError.InvalidField("nom"))
        if (payload.size > Arb1.MAX_PAYLOAD_BYTES) return reject(IssueError.PayloadTooLarge(payload.size))

        // 3–4. Segell.
        val signedInput = Arb1.PREFIX + "." + Base64Url.encode(payload)
        val rawSig = try {
            sealer.seal(signedInput.toByteArray(Charsets.US_ASCII))
        } catch (e: SealException) {
            return reject(IssueError.SealFailed(e.message ?: "segell"))
        } catch (e: RuntimeException) {
            return reject(IssueError.SealFailed(e.javaClass.simpleName))
        }
        if (rawSig.size != Arb1.SIGNATURE_BYTES) return reject(IssueError.SealFailed("mida de firma"))
        val token = signedInput + "." + Base64Url.encode(rawSig)

        // 5. Autoverificació del text complet.
        val check = verifier.verify(token)
        val credential = check.credential
        if (check.code != VerificationCode.OK || credential == null) {
            return reject(IssueError.SelfVerificationFailed(check.code))
        }
        if (credential.nom != nom || credential.cid != cid || credential.iat != iat ||
            credential.ok != request.ok || credential.tot != request.tot || credential.dur != request.dur
        ) {
            return reject(IssueError.SelfVerificationFailed(check.code))
        }
        // 6. El text exacte és la credencial.
        return IssueResult.Issued(token, credential)
    }

    private fun reject(error: IssueError) = IssueResult.Rejected(error)

    companion object {
        private val EXTENSION_NAME = Regex("[a-z0-9_]{1,16}")
        private const val HEX = "0123456789abcdef"

        /**
         * **Només per a proves (cadena G5, vector V03).** Mateix emissor que el de producció
         * (mateixa validació, segell i autoverificació), però escriu cada caràcter no ASCII dels
         * textos com a escapament `\uXXXX` (hex en minúscules; fora del BMP, parell de substituts).
         * §3.7.5.2 ho admet (la serialització no afecta la verificació), però l'app no ho ha d'usar:
         * cal un `@OptIn(Arb1TestInstrumentation::class)` explícit.
         */
        @Arb1TestInstrumentation
        fun withNonAsciiEscapes(
            sealer: CredentialSealer,
            kid: String,
            trustedKeys: TrustedKeys,
            cidGenerator: () -> String = { UUID.randomUUID().toString() },
        ): CredentialIssuer =
            CredentialIssuer(sealer, kid, trustedKeys, cidGenerator, PayloadEscaping.NON_ASCII_U_ESCAPES)

        internal fun formatIat(date: LocalDate): String? {
            val y = date.year
            if (y !in 0..9999) return null
            val s = y.toString().padStart(4, '0') + "-" + date.monthValue.toString().padStart(2, '0') + "-" +
                date.dayOfMonth.toString().padStart(2, '0')
            return if (Fields.isValidIat(s)) s else null
        }

        /**
         * Serialització recomanada (§3.7.5.2): ordre d'inserció, sense espais, no ASCII en UTF-8
         * directe, només s'escapen `"` i `\`.
         */
        internal fun serialize(fields: Map<String, Any>, escaping: PayloadEscaping = PayloadEscaping.NONE): String {
            val sb = StringBuilder("{")
            var first = true
            for ((k, v) in fields) {
                if (!first) sb.append(',')
                first = false
                appendString(sb, k, escaping)
                sb.append(':')
                when (v) {
                    is Int -> sb.append(v)
                    is String -> appendString(sb, v, escaping)
                    else -> throw IllegalArgumentException("tipus no admès")
                }
            }
            return sb.append('}').toString()
        }

        private fun appendString(sb: StringBuilder, s: String, escaping: PayloadEscaping) {
            sb.append('"')
            for (c in s) {
                when {
                    c == '"' || c == '\\' -> sb.append('\\').append(c)
                    escaping == PayloadEscaping.NON_ASCII_U_ESCAPES && c.code > 0x7F ->
                        sb.append('\\').append('u').append(HEX[c.code shr 12 and 0xF]).append(HEX[c.code shr 8 and 0xF])
                            .append(HEX[c.code shr 4 and 0xF]).append(HEX[c.code and 0xF])
                    else -> sb.append(c)
                }
            }
            sb.append('"')
        }
    }
}

/** Serialització dels textos del payload. Interna; a producció sempre [NONE]. */
internal enum class PayloadEscaping { NONE, NON_ASCII_U_ESCAPES }

/**
 * Marca l'API d'instrumentació de proves de l'emissor (cadena G5 de `05` §5.8). Sense
 * `@OptIn(Arb1TestInstrumentation::class)` explícit és un error de compilació: el codi de l'app no
 * l'ha d'usar mai (només proves JVM i proves instrumentades / APK de test).
 */
@RequiresOptIn(
    message = "Instrumentació de proves ARB1 (G5): no s'ha d'usar a l'app de producció.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class Arb1TestInstrumentation
