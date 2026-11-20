package dev.lluissunol.arbitratge.credential

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrimitivesTest {
    @Test
    fun base64UrlCanonical() {
        for (n in 0..40) {
            val b = ByteArray(n) { (it * 37 + 11).toByte() }
            val e = Base64Url.encode(b)
            assertTrue(e.none { it == '=' || it == '+' || it == '/' })
            assertContentEquals(b, Base64Url.decodeCanonical(e))
            assertEquals(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b), e)
        }
        assertNull(Base64Url.decodeCanonical("QR")) // bits sobrants ≠ 0 ("QQ" és el canònic)
        assertNotNull(Base64Url.decodeCanonical("QQ"))
        assertNull(Base64Url.decodeCanonical("QUJ")) // "QUI" canònic
        assertNull(Base64Url.decodeCanonical("QUJDR")) // longitud ≡ 1 mod 4
        assertNull(Base64Url.decodeCanonical("QQ=="))
        assertNull(Base64Url.decodeCanonical("a+b/"))
    }

    @Test
    fun strictUtf8() {
        assertEquals("aé€\uD83D\uDE00", StrictUtf8.decode("aé€\uD83D\uDE00".toByteArray()))
        fun bad(vararg b: Int) = assertNull(StrictUtf8.decode(ByteArray(b.size) { b[it].toByte() }), b.joinToString())
        bad(0xEF, 0xBB, 0xBF, 0x7B, 0x7D) // BOM
        bad(0xC0, 0x80)
        bad(0xC1, 0xBF)
        bad(0xE0, 0x9F, 0xBF)
        bad(0xED, 0xA0, 0x80)
        bad(0xF0, 0x8F, 0xBF, 0xBF)
        bad(0xF4, 0x90, 0x80, 0x80)
        bad(0xF5, 0x80, 0x80, 0x80)
        bad(0x80)
        bad(0xE2, 0x82)
        bad(0xFF)
        // U+FEFF no inicial és UTF-8 vàlid (el JSON o les regles de nom decideixen).
        assertEquals("a\uFEFF", StrictUtf8.decode(byteArrayOf(0x61, 0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())))
    }

    @Test
    fun restrictedJson() {
        val m = RestrictedJson.parseObject(" {\"a\" : \"x\\u00e9\\ud83d\\ude00\\/\" , \"b\":0,\"c\":999999999}\n")!!
        assertEquals(PayloadValue.Text("xé\uD83D\uDE00/"), m["a"])
        assertEquals(PayloadValue.Integer(0), m["b"])
        assertEquals(PayloadValue.Integer(999999999), m["c"])
        assertNotNull(RestrictedJson.parseObject("{}"))
        for (bad in listOf(
            "", "[]", "{", "{\"a\":1,}", "{\"a\":1}{}", "{\"a\":01}", "{\"a\":-1}", "{\"a\":1.0}", "{\"a\":1e2}",
            "{\"a\":1234567890}", "{\"a\":true}", "{\"a\":null}", "{\"a\":{}}", "{\"a\":[]}", "{\"a\":\"\\ud800\"}",
            "{\"a\":\"\\udc00\"}", "{\"a\":\"x\u0001\"}", "{\"a\":1,\"\\u0061\":2}", "{a:1}", "{\"a\":\"\\x\"}",
            "{\"a\":\"\\u00G0\"}", "\uFEFF{}", "{\"a\":1 2}",
        )) {
            assertNull(RestrictedJson.parseObject(bad), bad)
        }
        val m16 = (1..16).joinToString(",", "{", "}") { "\"k$it\":$it" }
        assertNotNull(RestrictedJson.parseObject(m16))
        val m17 = (1..17).joinToString(",", "{", "}") { "\"k$it\":$it" }
        assertNull(RestrictedJson.parseObject(m17))
    }

    @Test
    fun inputNormalization() {
        assertEquals("ARB1.a.b", CredentialVerifier.normalizeInput(" A\tR\nB\r1\u00a0.\u200ba\uFEFF.b "))
        assertNull(CredentialVerifier.normalizeInput(" \u00a0"))
        assertNull(CredentialVerifier.normalizeInput("A".repeat(2049)))
        // Res més: ni majúscules ni altres espais Unicode.
        assertEquals("arb1\u2003x", CredentialVerifier.normalizeInput("arb1\u2003x"))
    }

    @Test
    fun iatCalendar() {
        assertTrue(Fields.isValidIat("2024-02-29"))
        assertTrue(Fields.isValidIat("2000-02-29"))
        assertTrue(Fields.isValidIat("0000-02-29"))
        assertTrue(Fields.isValidIat("2026-12-31"))
        assertTrue(!Fields.isValidIat("1900-02-29"))
        assertTrue(!Fields.isValidIat("2026-04-31"))
        assertTrue(!Fields.isValidIat("2026-11-00"))
        assertTrue(!Fields.isValidIat("２０２６-11-20"))
    }

    @Test
    fun trustedKeysLoader() {
        val good = TestMaterial.trusted["test"]!!
        val spki = Base64Url.encode(good.spki)
        fun entry(kid: String = "test", alg: String = "ES256", s: String = spki, sha: String = good.sha256Spki, status: String = "active") =
            "{\"kid\":\"$kid\",\"alg\":\"$alg\",\"spki\":\"$s\",\"sha256_spki\":\"$sha\",\"status\":\"$status\",\"valid_from\":\"2026-01-01\"}"
        assertTrue(TrustedKeys.parseEntry(entry(), "test").isSuccess)
        assertTrue(TrustedKeys.parseEntry(entry(), "s1").isFailure, "kid != nom del fitxer")
        assertTrue(TrustedKeys.parseEntry(entry(kid = "Test")).isFailure)
        assertTrue(TrustedKeys.parseEntry(entry(alg = "ES384")).isFailure)
        assertTrue(TrustedKeys.parseEntry(entry(sha = "0".repeat(64))).isFailure)
        assertTrue(TrustedKeys.parseEntry(entry(sha = good.sha256Spki.uppercase())).isFailure)
        assertTrue(TrustedKeys.parseEntry(entry(status = "revoked")).isFailure)
        assertTrue(TrustedKeys.parseEntry(entry(status = "Active")).isFailure)
        assertTrue(TrustedKeys.parseEntry(entry(s = spki + "A")).isFailure)
        assertTrue(TrustedKeys.parseEntry(entry(status = "retired")).getOrThrow().status.accepted)
        // Punt fora de la corba ⇒ descartada encara que el hash quadri.
        val off = good.spki.copyOf().also { it[90] = (it[90].toInt() xor 1).toByte() }
        val offSha = P256.toHex(java.security.MessageDigest.getInstance("SHA-256").digest(off))
        assertTrue(TrustedKeys.parseEntry(entry(s = Base64Url.encode(off), sha = offSha)).isFailure)

        val list = TrustedKeys.parseList("[${entry()},${entry(status = "nope")},${entry(kid = "dup")},${entry(kid = "dup")}]")
        assertEquals(setOf("test"), list.kids)
        assertEquals(3, list.discarded.size)
    }

    @Test
    fun retiredKeyIsAcceptedByVerifier() {
        val t = TestMaterial.trusted["test"]!!
        val keys = TrustedKeys.of(listOf(TrustedKeys.entryFromSpki("test", t.spki, KeyStatus.RETIRED)))
        val issuer = CredentialIssuer(JcaEs256Sealer(TestMaterial.pkcs8("test")), "test", keys)
        val r = issuer.issue(IssueRequest("Anna", "1", java.time.LocalDate.of(2026, 1, 1)))
        assertTrue(r is IssueResult.Issued)
        // K02 (només release/G3): kid=test contra una llista sense "test" ⇒ UNKNOWN_KID.
        assertEquals(VerificationCode.UNKNOWN_KID, CredentialVerifier(TrustedKeys.EMPTY).verify(r.token).code)
    }
}
