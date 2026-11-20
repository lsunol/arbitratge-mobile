@file:OptIn(Arb1TestInstrumentation::class)

package cat.futbolbotons.arbitratge.credential.vectors

import cat.futbolbotons.arbitratge.credential.Arb1ChainCases
import cat.futbolbotons.arbitratge.credential.Arb1TestInstrumentation
import cat.futbolbotons.arbitratge.credential.Base64Url
import cat.futbolbotons.arbitratge.credential.CredentialIssuer
import cat.futbolbotons.arbitratge.credential.CredentialVerifier
import cat.futbolbotons.arbitratge.credential.IssueRequest
import cat.futbolbotons.arbitratge.credential.IssueResult
import cat.futbolbotons.arbitratge.credential.JcaEs256Sealer
import cat.futbolbotons.arbitratge.credential.MiniJson
import cat.futbolbotons.arbitratge.credential.P256
import cat.futbolbotons.arbitratge.credential.TrustedKeys
import cat.futbolbotons.arbitratge.credential.VerificationCode
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.time.LocalDate

/**
 * Generador dels vectors públics ARB1 (`02` §3.7.11). Ús (vegeu `test-vectors/README.md`):
 *
 *     ./gradlew :core-credential:generateTestVectors
 *
 * - Llegeix les claus de prova fixes de `test-vectors/test-keys.json` (públiques per disseny).
 *   Només amb `--init-keys` i si el fitxer no existeix en crea de noves (un sol cop a la vida
 *   del projecte; canviar-les obliga a regenerar-ho tot).
 * - Escriu `trusted-keys-test.json` i `cases.json`. L'aleatorietat d'ECDSA surt d'un
 *   `SHA1PRNG` amb llavor fixa: amb el mateix JDK, regenerar no canvia cap byte.
 * - Abans d'escriure, verifica cada cas amb [CredentialVerifier] i falla si algun codi no és
 *   l'esperat. Només imprimeix recomptes.
 */
fun main(args: Array<String>) {
    val dir = File(args.firstOrNull { !it.startsWith("--") } ?: error("cal la carpeta test-vectors"))
    val initKeys = "--init-keys" in args
    Generator(dir, initKeys).run()
}

private class TestKey(val name: String, val kid: String?, val status: String?, val pkcs8: ByteArray, val spki: ByteArray)

private class Case(
    val id: String,
    val description: String,
    val input: String,
    val code: VerificationCode,
    val fields: Map<String, Any>? = null,
)

private class Generator(private val dir: File, private val initKeys: Boolean) {
    private val rng = SecureRandom.getInstance("SHA1PRNG").apply { setSeed("arb1-test-vectors-v1".toByteArray()) }
    private val cases = ArrayList<Case>()
    private lateinit var test: TestKey
    private lateinit var testx: TestKey
    private lateinit var other: TestKey
    private lateinit var trusted: TrustedKeys

    private val iat = "2026-11-20"
    private val nomV01 = "Anna Exemple Puig"
    private val nomV02 = "N\u00faria Gal\u00b7la P\u00e9rez" // NFC, «l·l» amb U+00B7

    fun run() {
        loadOrCreateKeys()
        writeTrustedKeys()
        trusted = TrustedKeys.parseList(File(dir, "trusted-keys-test.json").readText())
        check(trusted.discarded.isEmpty()) { "llista de prova incoherent" }
        buildCases()
        selfCheck()
        writeCases()
        val byCode = cases.groupingBy { it.code }.eachCount()
        println("Vectors ARB1: ${cases.size} casos; " + VerificationCode.entries.joinToString { "${it.name}=${byCode[it] ?: 0}" })
    }

    // ------------------------------------------------------------------ claus

    private fun loadOrCreateKeys() {
        val file = File(dir, "test-keys.json")
        if (!file.exists()) {
            check(initKeys) { "falta ${file.path}: les claus de prova són fixes (usa --init-keys només el primer cop)" }
            val gen = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1"), SecureRandom()) }
            val entries = listOf(Triple("test", "test", "active"), Triple("testx", "testx", "compromised"), Triple("other", null, null))
                .map { (name, kid, status) ->
                    val kp = gen.generateKeyPair()
                    linkedMapOf<String, Any?>(
                        "name" to name,
                        "kid" to kid,
                        "status" to status,
                        "pkcs8" to Base64Url.encode(kp.private.encoded),
                        "spki" to Base64Url.encode(kp.public.encoded),
                    ).filterValues { it != null }
                }
            val doc = linkedMapOf(
                "warning" to "CLAUS DE PROVA, PUBLIQUES PER DISSENY (02 3.7.9, 05 5.5). Mai a produccio ni a trusted-keys/.",
                "keys" to entries,
            )
            file.writeText(JsonOut.value(doc) + "\n")
            println("Claus de prova creades a ${file.name} (3).")
        }
        val doc = MiniJson.parse(file.readText()) as Map<*, *>
        val keys = (doc["keys"] as List<*>).map { it as Map<*, *> }.map {
            val pkcs8 = Base64Url.decodeCanonical(it["pkcs8"] as String)!!
            val spki = Base64Url.decodeCanonical(it["spki"] as String)!!
            check(P256.spkiFromPkcs8(pkcs8)!!.contentEquals(spki)) { "privada i pública no corresponen (${it["name"]})" }
            TestKey(it["name"] as String, it["kid"] as String?, it["status"] as String?, pkcs8, spki)
        }
        test = keys.single { it.name == "test" }
        testx = keys.single { it.name == "testx" }
        other = keys.single { it.name == "other" }
    }

    private fun writeTrustedKeys() {
        val entries = listOf(test, testx).map {
            linkedMapOf(
                "kid" to it.kid!!,
                "alg" to "ES256",
                "spki" to Base64Url.encode(it.spki),
                "sha256_spki" to P256Hex.sha256(it.spki),
                "status" to it.status!!,
                "valid_from" to "2026-01-01",
            )
        }
        File(dir, "trusted-keys-test.json").writeText(JsonOut.value(entries) + "\n")
    }

    // ------------------------------------------------------------------ ajudes

    private fun sign(key: TestKey, input: ByteArray): ByteArray = JcaEs256Sealer(key.pkcs8, rng).seal(input)

    private fun tokenBytes(payload: ByteArray, key: TestKey = test, prefix: String = "ARB1"): String {
        val si = prefix + "." + Base64Url.encode(payload)
        return si + "." + Base64Url.encode(sign(key, si.toByteArray(Charsets.US_ASCII)))
    }

    private fun token(json: String, key: TestKey = test): String = tokenBytes(json.toByteArray(Charsets.UTF_8), key)

    private fun cid(n: Int): String {
        val h = P256Hex.sha256(("arb1-cid-$n").toByteArray())
        return h.substring(0, 8) + "-" + h.substring(8, 12) + "-4" + h.substring(13, 16) + "-" +
            "89ab"[h[16].digitToInt(16) and 3] + h.substring(17, 20) + "-" + h.substring(20, 32)
    }

    private fun q(s: String) = "\"" + s + "\""

    /** Membres amb text JSON cru (nom sense cometes, valor ja serialitzat). */
    private fun base(cid: String, nomRaw: String = nomV01, kid: String = "test") = mutableListOf(
        "v" to "1", "kid" to q(kid), "cid" to q(cid), "nom" to q(nomRaw), "res" to q("APTE"),
        "exv" to q("1"), "iat" to q(iat), "orig" to q("autoservei"),
    )

    private fun MutableList<Pair<String, String>>.set(name: String, raw: String) = apply {
        val i = indexOfFirst { it.first == name }
        if (i >= 0) this[i] = name to raw else add(name to raw)
    }

    private fun MutableList<Pair<String, String>>.drop(name: String) = apply { removeAll { it.first == name } }

    private fun json(m: List<Pair<String, String>>) = m.joinToString(",", "{", "}") { "\"${it.first}\":${it.second}" }

    private fun fields(cid: String, nom: String = nomV01, bv: String? = null, app: String? = null, kid: String = "test"): Map<String, Any> {
        val f = linkedMapOf<String, Any>("v" to 1, "kid" to kid, "cid" to cid, "nom" to nom, "res" to "APTE", "exv" to "1", "iat" to iat, "orig" to "autoservei")
        bv?.let { f["bv"] = it }
        app?.let { f["app"] = it }
        return f
    }

    /** [id] estable: el grup de §3.7.11 i, si el grup té variants, `-<variant>` (p. ex. `F04-lowercase`). */
    private fun add(id: String, description: String, input: String, code: VerificationCode, fields: Map<String, Any>? = null) {
        require(Regex("[A-Z][0-9]{2}(-[a-z0-9-]+)?").matches(id)) { "id invàlid: $id" }
        require(cases.none { it.id == id }) { "id duplicat: $id" }
        cases += Case(id, description, input, code, fields)
    }

    private fun issuer(cid: String) = CredentialIssuer(JcaEs256Sealer(test.pkcs8, rng), "test", trusted) { cid }

    private fun issue(cid: String, req: IssueRequest): String {
        val r = issuer(cid).issue(req)
        check(r is IssueResult.Issued) { "l'emissor ha rebutjat un cas vàlid: $r" }
        return r.token
    }

    private fun req(nom: String, bv: String? = null, app: String? = null, ext: Map<String, String> = emptyMap()) =
        IssueRequest(nom, "1", LocalDate.parse(iat), bv, app, ext)

    /** Petició V01–V05 compartida amb la prova de cadena G5 (font única: [Arb1ChainCases]). */
    private fun chain(id: String): IssueRequest = Arb1ChainCases.cases.single { it.id == id }.request

    private fun parts(token: String) = token.split('.')

    private fun sigOf(token: String) = Base64Url.decodeCanonical(parts(token)[2])!!

    private fun withSig(token: String, sig: ByteArray) = parts(token)[0] + "." + parts(token)[1] + "." + Base64Url.encode(sig)

    private fun payloadOf(token: String) = Base64Url.decodeCanonical(parts(token)[1])!!

    private fun withPayload(token: String, payload: ByteArray) = parts(token)[0] + "." + Base64Url.encode(payload) + "." + parts(token)[2]

    /** Canvia un bit sobrant del darrer caràcter (codificació no canònica). */
    private fun nonCanonical(seg: String): String {
        require(seg.length % 4 != 0)
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val v = alphabet.indexOf(seg.last())
        return seg.dropLast(1) + alphabet[v or 1]
    }

    private fun bytesWithNom(cid: String, raw: ByteArray): ByteArray {
        val j = json(base(cid, "@@"))
        val i = j.indexOf("@@")
        return j.substring(0, i).toByteArray() + raw + j.substring(i + 2).toByteArray()
    }

    // ------------------------------------------------------------------ casos

    private fun buildCases() {
        // ---- Vàlids (OK)
        val cidV01 = cid(1)
        var v01: String
        do {
            v01 = issue(cidV01, chain("V01"))
        } while (P256.sOf(sigOf(v01)) > P256.N.shiftRight(1)) // s baix, perquè V09 tingui s alt
        add("V01", "Emissor de producció (kid=test): només obligatoris, nom ASCII", v01, VerificationCode.OK, fields(cidV01))

        val cidV02 = cid(2)
        val v02 = issue(cidV02, chain("V02"))
        add("V02", "Emissor: accents i l·l (U+00B7) en UTF-8 directe", v02, VerificationCode.OK, fields(cidV02, nomV02))

        val cidV03 = cid(3)
        add(
            "V03", "Mateix nom que V02 amb escapes \\u00XX al JSON (vector fix; bytes idèntics als de CredentialIssuer.withNonAsciiEscapes, que l'usa a G5)",
            token(json(base(cidV03, "N\\u00faria Gal\\u00b7la P\\u00e9rez"))), VerificationCode.OK, fields(cidV03, nomV02),
        )

        val cidV04 = cid(4)
        val nom80 = Arb1ChainCases.NOM_80
        add("V04", "Emissor: nom de 80 punts de codi amb un caràcter fora del BMP (U+20BB7)", issue(cidV04, chain("V04")), VerificationCode.OK, fields(cidV04, nom80))

        val cidV05 = cid(5)
        add(
            "V05", "Emissor: amb bv i app", issue(cidV05, chain("V05")),
            VerificationCode.OK, fields(cidV05, bv = "2026.11.1", app = "1.0.0+5"),
        )

        val cidV06 = cid(6)
        add("V06", "Emissor: amb membre desconegut (s'ignora)", issue(cidV06, req(nomV01, ext = mapOf("nota" to "prova"))), VerificationCode.OK, fields(cidV06))

        val cidV07 = cid(7)
        val v07json = "\r\n {\n  \"v\" : 1 ,\n\t\"kid\":\"test\" ,\"cid\" :\"$cidV07\",\r\n  \"nom\":\"$nomV01\", \"res\":\"APTE\",\"exv\":\"1\"," +
            "\"iat\":\"$iat\",\"orig\":\"autoservei\"\n}\t \n"
        add("V07", "JSON amb espais insignificants (espai, tab, LF, CR) dins i fora de l'objecte", token(v07json), VerificationCode.OK, fields(cidV07))

        val cidV08 = cid(8)
        var v08: String
        do {
            v08 = issue(cidV08, req(nomV01))
            val s = sigOf(v08)
        } while (s[0] != 0.toByte() && s[32] != 0.toByte())
        add("V08", "Emissor: r o s amb zeros a l'esquerra (INTEGER DER de 31 bytes o amb 0x00)", v08, VerificationCode.OK, fields(cidV08))

        val sig01 = sigOf(v01)
        val highS = P256.N - P256.sOf(sig01)
        add("V09", "s alt: n−s de V01 (sense requisit low-S)", withSig(v01, sig01.copyOfRange(0, 32) + P256.to32(highS)), VerificationCode.OK, fields(cidV01))

        val p = parts(v01)
        val v10 = " \t\n" + p[0] + "\r\n." + p[1].substring(0, 40) + "\u00a0" + p[1].substring(40, 90) + " \n" +
            p[1].substring(90) + "\u200b." + p[2].substring(0, 43) + "\t" + p[2].substring(43) + "\ufeff \r\n"
        add("V10", "V01 amb espais, tabuladors, salts, U+00A0, U+200B i U+FEFF intercalats i exteriors", v10, VerificationCode.OK, fields(cidV01))

        val cidV11 = cid(11)
        val nfd = "Nu\u0301ria Pe\u0301rez"
        add("V11", "nom en NFD signat per un emissor de prova (el verificador no comprova NFC)", token(json(base(cidV11, nfd))), VerificationCode.OK, fields(cidV11, nfd))

        // ---- Entrada i envoltori
        add("F01-empty", "Buit", "", VerificationCode.BAD_FORMAT)
        add("F01-spaces", "Només espais i salts", "  \t\r\n  ", VerificationCode.BAD_FORMAT)
        add("F01-removable-only", "Només U+00A0, U+200B i U+FEFF", "\u00a0\u200b\ufeff", VerificationCode.BAD_FORMAT)

        add("F02-https", "URL https://", "https://example.org/verificador#" + v01, VerificationCode.IS_URL)
        add("F02-http-upper", "URL HTTP:// en majúscules", "HTTP://EXAMPLE.ORG/" + v01, VerificationCode.IS_URL)

        add("F03-token-plus-spaces", "Entrada bruta > 2048 (V01 + espais fins a 2049)", v01 + " ".repeat(2049 - v01.length), VerificationCode.BAD_FORMAT)
        add("F03-2049-chars", "Entrada bruta de 2049 'A'", "A".repeat(2049), VerificationCode.BAD_FORMAT)

        val rest01 = p[1] + "." + p[2]
        add("F04-lowercase", "Prefix en minúscules arb1.", "arb1.$rest01", VerificationCode.BAD_FORMAT)
        add("F04-arb0", "Prefix ARB0.", "ARB0.$rest01", VerificationCode.BAD_FORMAT)
        add("F04-arb01", "Prefix ARB01.", "ARB01.$rest01", VerificationCode.BAD_FORMAT)
        add("F04-no-dot", "ARB1 sense punt", "ARB1", VerificationCode.BAD_FORMAT)
        add("F04-no-dot-with-rest", "ARB1 enganxat al payload sense punt", "ARB1$rest01", VerificationCode.BAD_FORMAT)

        add("F05-two-segments", "2 segments", p[0] + "." + p[1], VerificationCode.BAD_FORMAT)
        add("F05-four-segments", "4 segments", v01 + "." + p[2], VerificationCode.BAD_FORMAT)
        add("F05-trailing-dot", "4 segments (punt final)", "$v01.", VerificationCode.BAD_FORMAT)

        add("F06-padding-payload", "Payload amb farciment '='", p[0] + "." + p[1] + "=." + p[2], VerificationCode.BAD_FORMAT)
        add("F06-padding-signature", "Firma amb farciment '=='", "$v01==", VerificationCode.BAD_FORMAT)
        add("F06-plus", "'+' al payload (alfabet base64 estàndard)", p[0] + "." + "+" + p[1].substring(1) + "." + p[2], VerificationCode.BAD_FORMAT)
        add("F06-slash", "'/' a la firma (alfabet base64 estàndard)", p[0] + "." + p[1] + "." + "/" + p[2].substring(1), VerificationCode.BAD_FORMAT)
        add("F06-non-ascii", "Caràcter no ASCII (U+00E9) al payload", p[0] + "." + p[1].substring(0, 10) + "\u00e9" + p[1].substring(11) + "." + p[2], VerificationCode.BAD_FORMAT)

        val canonSource = listOf(v01, v02, v11Token()).first { payloadOf(it).size % 3 != 0 }
        val cp = parts(canonSource)
        add("F07-payload-noncanonical", "Payload base64url no canònic (bits sobrants ≠ 0)", cp[0] + "." + nonCanonical(cp[1]) + "." + cp[2], VerificationCode.BAD_FORMAT)
        add("F07-signature-noncanonical", "Firma base64url no canònica (bits sobrants ≠ 0)", p[0] + "." + p[1] + "." + nonCanonical(p[2]), VerificationCode.BAD_FORMAT)
        val seg1mod4 = if (p[1].length % 4 == 0) p[1].dropLast(3) else p[1].dropLast((p[1].length % 4) + 3).ifEmpty { p[1] }
        check(seg1mod4.length % 4 == 1)
        add("F07-length-1-mod-4", "Payload de longitud ≡ 1 (mod 4)", p[0] + "." + seg1mod4 + "." + p[2], VerificationCode.BAD_FORMAT)

        add("F08-63-bytes", "Firma de 63 bytes", withSig(v01, sig01.copyOf(63)), VerificationCode.BAD_FORMAT)
        add("F08-65-bytes", "Firma de 65 bytes", withSig(v01, sig01 + byteArrayOf(0)), VerificationCode.BAD_FORMAT)
        add("F08-der", "Firma en DER en lloc de R‖S", withSig(v01, P256.rawToDer(sig01)), VerificationCode.BAD_FORMAT)

        add("F09-empty-payload", "Payload buit", "ARB1.." + p[2], VerificationCode.BAD_FORMAT)
        val big = bytesPadded(cid(91), 513)
        add("F09-513-bytes", "Payload de 513 bytes (signat)", tokenBytes(big), VerificationCode.BAD_FORMAT)
        add("F09-token-over-1024", "Token > 1024 caràcters", "ARB1." + "A".repeat(1000) + "." + p[2], VerificationCode.BAD_FORMAT)

        // ---- Versió
        add("U01-arb2-valid-rest", "ARB2. + resta vàlida", "ARB2.$rest01", VerificationCode.UNKNOWN_VERSION)
        add("U01-arb2-xyz", "ARB2.xyz", "ARB2.xyz", VerificationCode.UNKNOWN_VERSION)
        add("U01-arb999", "ARB999.", "ARB999.", VerificationCode.UNKNOWN_VERSION)

        // ---- Payload (tots signats correctament amb test: l'ordre fa que no s'arribi a la firma)
        val cidP = cid(100)
        fun b(vararg x: Int) = ByteArray(x.size) { x[it].toByte() }
        listOf(
            Triple("overlong-c1", "overlong C1 81", b(0xC1, 0x81)),
            Triple("overlong-c0", "overlong C0 AF", b(0xC0, 0xAF)),
            Triple("overlong-e0", "overlong E0 80 AF", b(0xE0, 0x80, 0xAF)),
            Triple("encoded-surrogate", "substitut codificat ED A0 80 (U+D800)", b(0xED, 0xA0, 0x80)),
            Triple("cesu8-pair", "parell de substituts codificats (CESU-8)", b(0xED, 0xA0, 0xBD, 0xED, 0xB8, 0x80)),
            Triple("stray-continuation", "byte de continuació solt 0x80", b(0x80)),
            Triple("truncated", "seqüència truncada E2 82", b(0xE2, 0x82)),
            Triple("byte-ff", "byte 0xFF", b(0xFF)),
            Triple("above-10ffff", "> U+10FFFF (F4 90 80 80)", b(0xF4, 0x90, 0x80, 0x80)),
        ).forEach { (id, d, raw) -> add("P01-$id", "UTF-8 invàlid dins nom: $d", tokenBytes(bytesWithNom(cidP, raw)), VerificationCode.BAD_PAYLOAD) }

        add("P02", "BOM EF BB BF inicial", tokenBytes(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + json(base(cidP)).toByteArray()), VerificationCode.BAD_PAYLOAD)

        val good = json(base(cidP))
        listOf(
            Triple("array", "array", "[1]"),
            Triple("text", "text", "\"ARB\""),
            Triple("integer", "enter", "1"),
            Triple("unclosed", "sense '}' final", good.dropLast(1)),
            Triple("trailing-comma", "coma final", good.dropLast(1) + ",}"),
            Triple("trailing-garbage", "contingut després de l'objecte", "$good x"),
            Triple("two-objects", "dos objectes", "$good{}"),
            Triple("single-quotes", "cometes simples", good.replace("\"v\"", "'v'")),
            Triple("unquoted-name", "nom sense cometes", good.replace("\"v\"", "v")),
            Triple("raw-control", "control U+0007 sense escapar dins un text", good.replace(nomV01, "Anna\u0007Puig")),
            Triple("bad-escape", "escapament invàlid \\x41", good.replace(nomV01, "Anna\\x41")),
            Triple("feff-as-space", "U+FEFF com a espai entre membres", good.replace(",\"kid\"", ",\ufeff\"kid\"")),
        ).forEach { (id, d, j) -> add("P03-$id", "No objecte o mal format: $d", token(j), VerificationCode.BAD_PAYLOAD) }

        listOf(
            Triple("nested", "objecte niuat", "{\"a\":1}"), Triple("empty-object", "objecte buit", "{}"),
            Triple("array", "array", "[1]"), Triple("empty-array", "array buit", "[]"),
            Triple("true", "true", "true"), Triple("false", "false", "false"), Triple("null", "null", "null"),
            Triple("decimal", "decimal", "1.0"), Triple("exponent", "exponent", "1e0"), Triple("negative", "negatiu", "-1"),
            Triple("ten-digits", "enter de 10 dígits", "1234567890"), Triple("leading-zero", "zero a l'esquerra", "01"),
        ).forEach { (id, d, raw) -> add("P04-$id", "Valor no admès al membre x: $d", token(json(base(cidP).set("x", raw))), VerificationCode.BAD_PAYLOAD) }
        add("P04-v-decimal", "v decimal 1.0", token(json(base(cidP).set("v", "1.0"))), VerificationCode.BAD_PAYLOAD)
        add("P04-v-exponent", "v amb exponent 1e0", token(json(base(cidP).set("v", "1e0"))), VerificationCode.BAD_PAYLOAD)

        add("P05-nom", "nom duplicat", token(json(base(cidP) + ("nom" to q("Altra Persona")))), VerificationCode.BAD_PAYLOAD)
        add("P05-nom-escaped", "nom duplicat per escapament (n\\u006fm)", token(json(base(cidP) + ("n\\u006fm" to q("Altra Persona")))), VerificationCode.BAD_PAYLOAD)
        add("P05-kid-escaped", "kid duplicat per escapament (\\u006bid)", token(json(base(cidP) + ("\\u006bid" to q("test")))), VerificationCode.BAD_PAYLOAD)
        add("P05-v", "v duplicat", token(json(base(cidP) + ("v" to "1"))), VerificationCode.BAD_PAYLOAD)
        add("P05-unknown", "membre desconegut duplicat", token(json(base(cidP) + ("x" to q("a")) + ("x" to q("b")))), VerificationCode.BAD_PAYLOAD)

        listOf(
            Triple("lone-high", "\\ud800 aïllat", "Anna \\ud800 Puig"),
            Triple("lone-low", "\\udc00 aïllat", "Anna \\udc00 Puig"),
            Triple("high-then-bmp", "alt seguit d'un no substitut", "Anna \\ud800\\u0041 Puig"),
            Triple("reversed-pair", "parell invertit", "Anna \\udc00\\ud800 Puig"),
            Triple("high-at-end", "alt al final del text", "Anna Puig\\ud83d"),
        ).forEach { (id, d, raw) -> add("P06-$id", "Substitut aïllat a nom: $d", token(json(base(cidP, raw))), VerificationCode.BAD_PAYLOAD) }
        add("P06-member-name", "Substitut aïllat en un nom de membre", token(json(base(cidP) + ("x\\ud800" to q("a")))), VerificationCode.BAD_PAYLOAD)

        listOf(
            Triple("missing-v", "falta v", base(cidP).drop("v")),
            Triple("missing-kid", "falta kid", base(cidP).drop("kid")),
            Triple("v-text", "v com a text", base(cidP).set("v", q("1"))),
            Triple("v-2", "v=2 a ARB1", base(cidP).set("v", "2")),
            Triple("v-0", "v=0", base(cidP).set("v", "0")),
            Triple("kid-uppercase", "kid amb majúscules", base(cidP).set("kid", q("Test"))),
            Triple("kid-dash", "kid amb '-'", base(cidP).set("kid", q("te-st"))),
            Triple("kid-empty", "kid buit", base(cidP).set("kid", q(""))),
            Triple("kid-17-chars", "kid de 17 caràcters", base(cidP).set("kid", q("a".repeat(17)))),
            Triple("kid-integer", "kid enter", base(cidP).set("kid", "1")),
        ).forEach { (id, d, m) -> add("P07-$id", "v/kid: $d", token(json(m)), VerificationCode.BAD_PAYLOAD) }

        val m17 = base(cidP).apply { for (i in 1..9) add("x$i" to q("$i")) }
        check(m17.size == 17)
        add("P08", "17 membres", token(json(m17)), VerificationCode.BAD_PAYLOAD)

        val pay01 = payloadOf(v01)
        add("P09-last-byte", "Darrer byte del payload de V01 alterat ('}' → ']'), firma de V01", withPayload(v01, pay01.copyOf().also { it[it.size - 1] = ']'.code.toByte() }), VerificationCode.BAD_PAYLOAD)
        add("P09-first-byte", "Primer byte del payload de V01 alterat ('{' → '['), firma de V01", withPayload(v01, pay01.copyOf().also { it[0] = '['.code.toByte() }), VerificationCode.BAD_PAYLOAD)

        // ---- Claus
        val cidK = cid(200)
        add("K01-unknown", "kid inexistent (zz9), firma correcta amb test", token(json(base(cidK, kid = "zz9"))), VerificationCode.UNKNOWN_KID)
        add("K01-production-kid", "kid de producció (s1) contra la llista de prova", token(json(base(cidK, kid = "s1"))), VerificationCode.UNKNOWN_KID)
        add("K03", "kid=testx (compromised), signat amb la clau testx", token(json(base(cidK, kid = "testx")), testx), VerificationCode.KID_NOT_ACCEPTED)

        // ---- Firma
        add("S01", "Un caràcter de nom canviat (JSON vàlid), firma de V01", withPayload(v01, String(pay01, Charsets.UTF_8).replace(nomV01, "Anna Exemple Puif").toByteArray()), VerificationCode.BAD_SIGNATURE)
        add("S02-r-bit", "Un bit de r canviat", withSig(v01, sig01.copyOf().also { it[5] = (it[5].toInt() xor 0x01).toByte() }), VerificationCode.BAD_SIGNATURE)
        add("S02-s-bit", "Un bit de s canviat", withSig(v01, sig01.copyOf().also { it[40] = (it[40].toInt() xor 0x10).toByte() }), VerificationCode.BAD_SIGNATURE)
        add("S03", "Payload de V02 amb la firma de V01", parts(v02)[0] + "." + parts(v02)[1] + "." + p[2], VerificationCode.BAD_SIGNATURE)
        val zero = ByteArray(32)
        val sPart = sig01.copyOfRange(32, 64)
        val rPart = sig01.copyOfRange(0, 32)
        add("S04-r-zero", "r = 0", withSig(v01, zero + sPart), VerificationCode.BAD_SIGNATURE)
        add("S04-s-zero", "s = 0", withSig(v01, rPart + zero), VerificationCode.BAD_SIGNATURE)
        add("S04-r-n", "r = n", withSig(v01, P256.to32(P256.N) + sPart), VerificationCode.BAD_SIGNATURE)
        add("S04-r-max", "r = 2^256 − 1", withSig(v01, ByteArray(32) { 0xFF.toByte() } + sPart), VerificationCode.BAD_SIGNATURE)
        add("S04-s-n", "s = n", withSig(v01, rPart + P256.to32(P256.N)), VerificationCode.BAD_SIGNATURE)
        add("S04-r-plus-n", "r = r + n (si cap en 256 bits) o r + 1", withSig(v01, rPlusN(rPart) + sPart), VerificationCode.BAD_SIGNATURE)
        val cidS = cid(300)
        val s05payload = json(base(cidS)).toByteArray()
        add("S05", "Firma sobre els bytes JSON descodificats (contracte antic de 03 §6.2.2)", "ARB1." + Base64Url.encode(s05payload) + "." + Base64Url.encode(sign(test, s05payload)), VerificationCode.BAD_SIGNATURE)
        add("S06", "Signat amb una altra clau P-256 i kid=test", token(json(base(cidS)), other), VerificationCode.BAD_SIGNATURE)

        // ---- Camps (signats amb test)
        val cidC = cid(400)
        for (f in listOf("cid", "nom", "res", "exv", "iat", "orig")) {
            add("C01-missing-$f", "Falta $f", token(json(base(cidC).drop(f))), VerificationCode.BAD_FIELDS)
        }
        add("C02-lowercase", "res = \"apte\"", token(json(base(cidC).set("res", q("apte")))), VerificationCode.BAD_FIELDS)
        add("C02-apta", "res = \"APTA\"", token(json(base(cidC).set("res", q("APTA")))), VerificationCode.BAD_FIELDS)
        add("C02-integer", "res enter", token(json(base(cidC).set("res", "1"))), VerificationCode.BAD_FIELDS)
        add("C03-capitalized", "orig = \"Autoservei\"", token(json(base(cidC).set("orig", q("Autoservei")))), VerificationCode.BAD_FIELDS)
        add("C03-other", "orig = \"organitzador\"", token(json(base(cidC).set("orig", q("organitzador")))), VerificationCode.BAD_FIELDS)
        add("C04-uppercase", "cid en majúscules", token(json(base(cidC).set("cid", q(cidC.uppercase())))), VerificationCode.BAD_FIELDS)
        add("C04-version-1", "cid versió 1 (no v4)", token(json(base(cidC).set("cid", q(cidC.substring(0, 14) + "1" + cidC.substring(15))))), VerificationCode.BAD_FIELDS)
        add("C04-variant-c", "cid amb variant 'c'", token(json(base(cidC).set("cid", q(cidC.substring(0, 19) + "c" + cidC.substring(20))))), VerificationCode.BAD_FIELDS)
        add("C04-no-dashes", "cid sense guions", token(json(base(cidC).set("cid", q(cidC.replace("-", ""))))), VerificationCode.BAD_FIELDS)
        listOf(
            Triple("feb-30", "2026-02-30", q("2026-02-30")), Triple("with-time", "amb hora", q("2026-11-20T10:00:00")),
            Triple("integer", "enter", "20261120"), Triple("month-13", "mes 13", q("2026-13-01")),
            Triple("month-00", "mes 00", q("2026-00-10")), Triple("feb-29-2025", "29 de febrer no bixest (2025)", q("2025-02-29")),
            Triple("feb-29-1900", "1900-02-29 (no bixest)", q("1900-02-29")), Triple("no-padding", "sense zeros", q("2026-1-5")),
        ).forEach { (id, d, raw) -> add("C05-$id", "iat: $d", token(json(base(cidC).set("iat", raw))), VerificationCode.BAD_FIELDS) }
        listOf(
            Triple("empty", "buit", ""), Triple("leading-space", "espai inicial", " Anna Puig"),
            Triple("trailing-space", "espai final", "Anna Puig "), Triple("double-space", "doble espai", "Anna  Puig"),
            Triple("u0007", "U+0007 escapat", "Anna\\u0007Puig"), Triple("u202e", "U+202E en UTF-8", "Anna \u202e Puig"),
            Triple("u00a0", "U+00A0 escapat", "Anna\\u00a0Puig"), Triple("u200b", "U+200B en UTF-8", "Anna\u200bPuig"),
            Triple("ufeff", "U+FEFF en UTF-8", "Anna\ufeffPuig"), Triple("81-code-points", "81 punts de codi", "A".repeat(81)),
        ).forEach { (id, d, raw) -> add("C06-$id", "nom: $d", token(json(base(cidC, raw))), VerificationCode.BAD_FIELDS) }
        add("C06-integer", "nom enter", token(json(base(cidC).set("nom", "1"))), VerificationCode.BAD_FIELDS)
        listOf(
            Triple("exv-empty", "buit", q("")), Triple("exv-space", "amb espai", q("1 0")), Triple("exv-17-chars", "17 caràcters", q("1".repeat(17))),
            Triple("exv-plus", "amb '+'", q("1+2")), Triple("exv-integer", "enter", "1"),
            Triple("bv-slash", "amb '/'", q("2026/11")), Triple("bv-25-chars", "25 caràcters", q("1".repeat(25))), Triple("bv-integer", "enter", "1"),
            Triple("app-space", "amb espai", q("1.0 beta")), Triple("app-33-chars", "33 caràcters", q("1".repeat(33))), Triple("app-non-ascii", "no ASCII", q("1.0\u00e9")),
        ).forEach { (id, d, raw) ->
            val f = id.substringBefore('-')
            add("C07-$id", "$f: $d", token(json(base(cidC).set(f, raw))), VerificationCode.BAD_FIELDS)
        }
        listOf("exp" to "999999999", "nbf" to "0", "crit" to q("exp"), "alg" to q("none")).forEach { (n, raw) ->
            add("C08-$n", "Nom reservat $n present", token(json(base(cidC) + (n to raw))), VerificationCode.BAD_FIELDS)
        }

        // ---- Extres (no són a la llista mínima de §3.7.11; límits i precedències del mateix contracte)
        add("X01", "Entrada bruta d'exactament 2048 (V01 + espais)", v01 + " ".repeat(2048 - v01.length), VerificationCode.OK, fields(cidV01))
        val cidX = cid(500)
        add("X02", "Payload d'exactament 512 bytes", tokenBytes(bytesPadded(cidX, 512)), VerificationCode.OK, fields(cidX))
        val m16 = base(cidX).apply { for (i in 1..8) add("x$i" to q("$i")) }
        add("X03", "16 membres", token(json(m16)), VerificationCode.OK, fields(cidX))
        add("X04", "Prefix de 4 dígits ARB1000.", "ARB1000.$rest01", VerificationCode.BAD_FORMAT)
        add("X05", "Versió de 2 dígits ARB10.", "ARB10.$rest01", VerificationCode.UNKNOWN_VERSION)
        add("X06", "URL amb espais davant i majúscules barrejades", "  hTtPs://example.org/x", VerificationCode.IS_URL)
        add("X07", "Noms de membre amb escapes (\\u006eom = nom) i escapes \\/ i \\\"", token(json(base(cidX).drop("nom") + ("\\u006eom" to q("Anna O\\\"Exemple\\/Puig")))), VerificationCode.OK, fields(cidX, "Anna O\"Exemple/Puig"))
        add("X08", "iat 2000-02-29 (bixest per la regla dels 400 anys)", token(json(base(cidX).set("iat", q("2000-02-29")))), VerificationCode.OK, fields(cidX).toMutableMap().also { it["iat"] = "2000-02-29" })
        add("X09", "kid=testx signat amb una clau d'una altra (l'estat decideix abans que la firma)", token(json(base(cidK, kid = "testx")), test), VerificationCode.KID_NOT_ACCEPTED)
        add("X10", "Membre desconegut enter de 9 dígits i 0", token(json(base(cidX) + ("x" to "999999999") + ("y" to "0"))), VerificationCode.OK, fields(cidX))
        add("X11", "Prefix ARB1 seguit d'URL: no és URL (no comença per http)", "ARB1.https://x", VerificationCode.BAD_FORMAT)
    }

    private fun v11Token(): String = token(json(base(cid(11), "Nu\u0301ria Pe\u0301rez")))

    private fun rPlusN(r: ByteArray): ByteArray {
        val sum = BigInteger(1, r) + P256.N
        return if (sum.bitLength() <= 256) P256.to32(sum) else P256.to32(BigInteger(1, r) + BigInteger.ONE)
    }

    /** Payload vàlid (a part de la mida) farcit amb un membre desconegut fins a [size] bytes. */
    private fun bytesPadded(cid: String, size: Int): ByteArray {
        val baseLen = json(base(cid) + ("pad" to q(""))).toByteArray().size
        val bytes = json(base(cid) + ("pad" to q("x".repeat(size - baseLen)))).toByteArray()
        check(bytes.size == size)
        return bytes
    }

    // ------------------------------------------------------------------ comprovació i sortida

    private fun selfCheck() {
        val verifier = CredentialVerifier(trusted)
        for (c in cases) {
            val r = verifier.verify(c.input)
            check(r.code == c.code) { "cas ${c.id} (${c.description}): esperat ${c.code}, obtingut ${r.code}" }
            if (c.code == VerificationCode.OK) {
                val cr = r.credential!!
                val got = linkedMapOf<String, Any>("v" to cr.v, "kid" to cr.kid, "cid" to cr.cid, "nom" to cr.nom, "res" to cr.res, "exv" to cr.exv, "iat" to cr.iat, "orig" to cr.orig)
                cr.bv?.let { got["bv"] = it }
                cr.app?.let { got["app"] = it }
                check(got == c.fields) { "cas ${c.id}: camps diferents" }
            }
        }
    }

    private fun writeCases() {
        val out = cases.map { c ->
            linkedMapOf<String, Any>(
                "id" to c.id,
                "group" to c.id.substringBefore('-'),
                "description" to c.description,
                "input" to c.input,
                "code" to c.code.name,
                "result" to c.code.outcome.label,
            ).also { m ->
                c.fields?.let {
                    m["fields"] = it
                    m["shortCode"] = (it["cid"] as String).let { cid -> cid.substring(0, 4).uppercase() + "-" + cid.substring(4, 8).uppercase() }
                }
            }
        }
        val doc = linkedMapOf(
            "format" to "arb1-test-vectors/2",
            "contract" to "docs/design/02-credencial-y-amenazas.md 3.7 (ARB1); cases 3.7.11",
            "trustedKeys" to "trusted-keys-test.json",
            "generator" to "./gradlew :core-credential:generateTestVectors",
            "cases" to out,
        )
        File(dir, "cases.json").writeText(JsonOut.value(doc) + "\n")
    }
}

private object P256Hex {
    fun sha256(b: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") {
        val v = it.toInt() and 0xFF
        "0123456789abcdef"[v shr 4].toString() + "0123456789abcdef"[v and 15]
    }
}
