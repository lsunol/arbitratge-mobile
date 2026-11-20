package cat.futbolbotons.arbitratge.credential

/**
 * Constants del contracte ARB1 (`docs/design/02-credencial-y-amenazas.md` §3.7, canònic).
 *
 * Cap canvi d'aquests valors sense nova versió de format (`ARB2`) i ADR (§3.7.0).
 */
object Arb1 {
    const val PREFIX = "ARB1"
    const val VERSION = 1

    /** §3.7.6: entrada bruta abans de normalitzar (unitats UTF-16). */
    const val MAX_RAW_INPUT = 2048

    /** §3.7.6: token normalitzat. */
    const val MAX_TOKEN = 1024

    /** §3.7.1: payload-seg 2..683 caràcters; payload 1..512 bytes. */
    const val MIN_PAYLOAD_SEG = 2
    const val MAX_PAYLOAD_SEG = 683
    const val MIN_PAYLOAD_BYTES = 1
    const val MAX_PAYLOAD_BYTES = 512

    /** §3.7.1/§3.7.3: firma R‖S de 64 bytes = 86 caràcters base64url. */
    const val SIGNATURE_BYTES = 64
    const val SIGNATURE_SEG = 86

    /** §3.7.4: màxim de membres de l'objecte. */
    const val MAX_MEMBERS = 16

    /** §3.7.4: màxim de dígits d'un enter. */
    const val MAX_INT_DIGITS = 9

    /** §3.7.5.1: punts de codi de `nom`. */
    const val MAX_NOM_CODE_POINTS = 80

    /** §3.7.5: noms reservats a `v=1`. */
    val RESERVED_NAMES: Set<String> = setOf("exp", "nbf", "crit", "alg")

    /** §3.7.5.2: ordre recomanat de serialització. */
    val FIELD_ORDER: List<String> = listOf("v", "kid", "cid", "nom", "res", "exv", "iat", "orig", "bv", "app")

    const val RES_APTE = "APTE"
    const val ORIG_AUTOSERVEI = "autoservei"
}

/** Resultat visible d'ADR 0001 (els textos en català i les pantalles són de `04`). */
enum class VerificationOutcome(val label: String) {
    VALIDA("VÀLIDA"),
    NO_VALIDA("NO VÀLIDA"),
    VERSIO_DESCONEGUDA("VERSIÓ DESCONEGUDA"),
    FORMAT_INCORRECTE("FORMAT INCORRECTE"),
}

/** Codis de §3.7.8, en l'ordre dels passos. */
enum class VerificationCode(val outcome: VerificationOutcome) {
    OK(VerificationOutcome.VALIDA),
    IS_URL(VerificationOutcome.FORMAT_INCORRECTE),
    BAD_FORMAT(VerificationOutcome.FORMAT_INCORRECTE),
    UNKNOWN_VERSION(VerificationOutcome.VERSIO_DESCONEGUDA),
    BAD_PAYLOAD(VerificationOutcome.FORMAT_INCORRECTE),
    UNKNOWN_KID(VerificationOutcome.NO_VALIDA),
    KID_NOT_ACCEPTED(VerificationOutcome.NO_VALIDA),
    BAD_SIGNATURE(VerificationOutcome.NO_VALIDA),
    BAD_FIELDS(VerificationOutcome.FORMAT_INCORRECTE),
}

/**
 * Camps decodificats d'una credencial `v=1` (§3.7.5). Només són dades del segell si el codi és
 * [VerificationCode.OK].
 */
data class Arb1Credential(
    val v: Int,
    val kid: String,
    val cid: String,
    val nom: String,
    val res: String,
    val exv: String,
    val iat: String,
    val orig: String,
    val bv: String? = null,
    val app: String? = null,
) {
    /** §3.7.5: primers 8 hex de `cid` en majúscules com `XXXX-XXXX` (mateix al PDF i a la PWA). */
    val shortCode: String
        get() = cid.substring(0, 4).uppercase() + "-" + cid.substring(4, 8).uppercase()
}
