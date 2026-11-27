package dev.lluissunol.arbitratge.credential

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CredentialIssuerTest {
    private val cid = "3f0c1e9a-5b7d-4c2e-9a10-6d8b2f4e7c01"
    private val iat = LocalDate.of(2026, 11, 20)
    private val issuer = CredentialIssuer(JcaEs256Sealer(TestMaterial.pkcs8("test")), "test", TestMaterial.trusted) { cid }
    private val verifier = CredentialVerifier(TestMaterial.trusted)

    private fun issued(r: IssueResult): IssueResult.Issued = assertIs<IssueResult.Issued>(r, r.toString())

    private fun rejected(r: IssueResult): IssueError = assertIs<IssueResult.Rejected>(r, r.toString()).error

    @Test
    fun v01RoundTripAndRecommendedSerialization() {
        val r = issued(issuer.issue(IssueRequest("Anna Exemple Puig", "1", iat)))
        val parts = r.token.split('.')
        assertEquals("ARB1", parts[0])
        assertEquals(86, parts[2].length)
        val json = String(Base64Url.decodeCanonical(parts[1])!!, Charsets.UTF_8)
        assertEquals(
            "{\"v\":1,\"kid\":\"test\",\"cid\":\"$cid\",\"nom\":\"Anna Exemple Puig\",\"res\":\"APTE\",\"exv\":\"1\"," +
                "\"iat\":\"2026-11-20\",\"orig\":\"autoservei\"}",
            json,
        )
        assertEquals(153, json.toByteArray().size) // 151 de l'exemple de §3.7.5.2 amb kid "s1" + 2
        val v = verifier.verify(r.token)
        assertEquals(VerificationCode.OK, v.code)
        assertEquals(r.credential, v.credential)
        assertEquals("3F0C-1E9A", r.credential.shortCode)
    }

    @Test
    fun v02AccentsAndMiddleDot() {
        val nom = "N\u00faria Gal\u00b7la P\u00e9rez"
        val r = issued(issuer.issue(IssueRequest(nom, "1", iat)))
        assertEquals(nom, r.credential.nom)
        assertEquals(VerificationCode.OK, verifier.verify(r.token).code)
        // UTF-8 directe, sense escapes.
        assertTrue(String(Base64Url.decodeCanonical(r.token.split('.')[1])!!, Charsets.UTF_8).contains(nom))
    }

    @Test
    fun v04EightyCodePointsWithNonBmp() {
        val nom = "\uD842\uDFB7\u7530 " + "a".repeat(77)
        assertEquals(80, nom.codePointCount(0, nom.length))
        val r = issued(issuer.issue(IssueRequest(nom, "1", iat)))
        assertEquals(nom, r.credential.nom)
        assertEquals(VerificationCode.OK, verifier.verify(r.token).code)
        assertEquals(
            IssueError.InvalidName(NameError.TOO_LONG),
            rejected(issuer.issue(IssueRequest(nom + "b", "1", iat))),
        )
    }

    @Test
    fun v05WithBvAndApp() {
        val r = issued(issuer.issue(IssueRequest("Anna Exemple Puig", "1", iat, bv = "2026.11.1", app = "1.0.0+5")))
        assertEquals("2026.11.1", r.credential.bv)
        assertEquals("1.0.0+5", r.credential.app)
        assertTrue(r.token.split('.')[1].let { String(Base64Url.decodeCanonical(it)!!) }.endsWith(",\"bv\":\"2026.11.1\",\"app\":\"1.0.0+5\"}"))
        assertEquals(VerificationCode.OK, verifier.verify(r.token).code)
    }

    @Test
    fun nameNormalizationNfdSpacesAndNbsp() {
        val input = " \tNu\u0301ria\u00a0\u00a0 Pe\u0301rez\u3000Gal\u00b7la \n"
        val r = issued(issuer.issue(IssueRequest(input, "1", iat)))
        assertEquals("N\u00faria P\u00e9rez Gal\u00b7la", r.credential.nom)
        assertEquals(NameResult.Valid("N\u00faria P\u00e9rez Gal\u00b7la"), NameRules.normalize(input))
        // Totes les substitucions de §3.7.5.1.
        val subs = "\u0009\u000A\u000D\u00A0\u1680\u2000\u2005\u200A\u202F\u205F\u3000"
        assertEquals(NameResult.Valid("A B"), NameRules.normalize("A${subs}B"))
        // Idempotent.
        assertEquals(NameResult.Valid("A B"), NameRules.normalize("A B"))
    }

    @Test
    fun invalidNamesAreRejected() {
        fun err(s: String) = (NameRules.normalize(s) as? NameResult.Invalid)?.error
        assertEquals(NameError.EMPTY, err(" \u00a0\t "))
        assertEquals(NameError.FORBIDDEN_CHARACTER, err("Anna\u202ePuig"))
        assertEquals(NameError.FORBIDDEN_CHARACTER, err("Anna\u200bPuig"))
        assertEquals(NameError.FORBIDDEN_CHARACTER, err("Anna\u0007Puig"))
        assertEquals(NameError.FORBIDDEN_CHARACTER, err("Anna\u00adPuig"))
        assertEquals(NameError.MALFORMED, err("Anna\ud800Puig"))
        assertEquals(NameError.TOO_LONG, err("a".repeat(81)))
        assertEquals(IssueError.InvalidName(NameError.FORBIDDEN_CHARACTER), rejected(issuer.issue(IssueRequest("A\uFEFFB", "1", iat))))
    }

    @Test
    fun resultFieldsAreSerializedInRecommendedOrderAndVerify() {
        val r = issued(
            issuer.issue(IssueRequest("Anna Exemple Puig", "1", iat, bv = "2026.11.1", app = "1.0.0+5", ok = 27, tot = 30, dur = 1834)),
        )
        val json = String(Base64Url.decodeCanonical(r.token.split('.')[1])!!, Charsets.UTF_8)
        assertTrue(
            json.endsWith(",\"orig\":\"autoservei\",\"ok\":27,\"tot\":30,\"dur\":1834,\"bv\":\"2026.11.1\",\"app\":\"1.0.0+5\"}"),
            json,
        )
        assertEquals(27, r.credential.ok)
        assertEquals(30, r.credential.tot)
        assertEquals(1834, r.credential.dur)
        val v = verifier.verify(r.token)
        assertEquals(VerificationCode.OK, v.code)
        assertEquals(r.credential, v.credential)
    }

    @Test
    fun resultFieldsAreOptionalAndIndependent() {
        val none = issued(issuer.issue(IssueRequest("Anna", "1", iat)))
        assertEquals(Triple(null, null, null), Triple(none.credential.ok, none.credential.tot, none.credential.dur))
        val onlyDur = issued(issuer.issue(IssueRequest("Anna", "1", iat, dur = 0)))
        assertEquals(Triple(null, null, 0), Triple(onlyDur.credential.ok, onlyDur.credential.tot, onlyDur.credential.dur))
        val score = issued(issuer.issue(IssueRequest("Anna", "1", iat, ok = 0, tot = 1)))
        assertEquals(Triple(0, 1, null), Triple(score.credential.ok, score.credential.tot, score.credential.dur))
        val max = issued(issuer.issue(IssueRequest("Anna", "1", iat, ok = 99, tot = 99, dur = 86400)))
        assertEquals(Triple(99, 99, 86400), Triple(max.credential.ok, max.credential.tot, max.credential.dur))
    }

    @Test
    fun invalidResultFieldsAreRejected() {
        fun reason(ok: Int?, tot: Int?, dur: Int? = null) =
            rejected(issuer.issue(IssueRequest("Anna", "1", iat, ok = ok, tot = tot, dur = dur)))
        assertEquals(IssueError.InvalidField("tot"), reason(27, null))
        assertEquals(IssueError.InvalidField("ok"), reason(null, 30))
        assertEquals(IssueError.InvalidField("ok"), reason(31, 30))
        assertEquals(IssueError.InvalidField("ok"), reason(-1, 30))
        assertEquals(IssueError.InvalidField("ok"), reason(100, 99))
        assertEquals(IssueError.InvalidField("tot"), reason(0, 0))
        assertEquals(IssueError.InvalidField("tot"), reason(27, 100))
        assertEquals(IssueError.InvalidField("dur"), reason(null, null, -1))
        assertEquals(IssueError.InvalidField("dur"), reason(null, null, 86401))
        assertEquals(IssueError.InvalidField("ok"), rejected(issuer.issue(IssueRequest("Anna", "1", iat, extensions = mapOf("ok" to "1")))))
    }

    @Test
    fun maximumPayloadWithAllFieldsFitsIn512Bytes() {
        // §3.7.6: nom de 80 punts de codi de 3 bytes UTF-8 + tots els camps al màxim del patró.
        val sixteenKid = CredentialIssuer({ ByteArray(64) }, "a".repeat(16), TestMaterial.trusted) { cid }
        val r = sixteenKid.issue(
            IssueRequest("\u30a2".repeat(80), "1".repeat(16), iat, bv = "2".repeat(24), app = "3".repeat(32), ok = 99, tot = 99, dur = 86400),
        )
        // No hi ha clau per a aquest kid: ha d'arribar a l'autoverificació (⇒ la mida s'ha acceptat).
        assertEquals(IssueError.SelfVerificationFailed(VerificationCode.UNKNOWN_KID), rejected(r))
        val ok = issued(
            issuer.issue(
                IssueRequest("\u30a2".repeat(80), "1".repeat(16), iat, bv = "2".repeat(24), app = "3".repeat(32), ok = 99, tot = 99, dur = 86400),
            ),
        )
        assertEquals(493, Base64Url.decodeCanonical(ok.token.split('.')[1])!!.size) // 505 amb kid de 16
        assertTrue(ok.token.length <= 775)
        // 80 caràcters fora del BMP (4 bytes) no hi caben amb tots els camps al màxim (ja passava sense ok/tot/dur).
        val nonBmp = "\uD842\uDFB7".repeat(80)
        val big = issuer.issue(IssueRequest(nonBmp, "1".repeat(16), iat, bv = "2".repeat(24), app = "3".repeat(32), ok = 99, tot = 99, dur = 86400))
        assertIs<IssueError.PayloadTooLarge>(rejected(big))
    }

    @Test
    fun reservedAndKnownNamesAreRejectedAsExtensions() {
        for (n in listOf("exp", "nbf", "crit", "alg")) {
            assertEquals(IssueError.ReservedName(n), rejected(issuer.issue(IssueRequest("Anna", "1", iat, extensions = mapOf(n to "x")))))
        }
        assertEquals(IssueError.InvalidField("nom"), rejected(issuer.issue(IssueRequest("Anna", "1", iat, extensions = mapOf("nom" to "x")))))
        val ok = issued(issuer.issue(IssueRequest("Anna", "1", iat, extensions = mapOf("nota" to "pro\"va"))))
        assertEquals(VerificationCode.OK, verifier.verify(ok.token).code)
    }

    @Test
    fun payloadOver512BytesIsRejected() {
        val r = issuer.issue(IssueRequest("Anna", "1", iat, extensions = mapOf("pad" to "x".repeat(400))))
        val e = assertIs<IssueError.PayloadTooLarge>(rejected(r))
        assertTrue(e.bytes > 512)
    }

    @Test
    fun invalidFieldsAreRejected() {
        assertEquals(IssueError.InvalidField("exv"), rejected(issuer.issue(IssueRequest("Anna", "1 0", iat))))
        assertEquals(IssueError.InvalidField("bv"), rejected(issuer.issue(IssueRequest("Anna", "1", iat, bv = "a/b"))))
        assertEquals(IssueError.InvalidField("app"), rejected(issuer.issue(IssueRequest("Anna", "1", iat, app = "1".repeat(33)))))
        assertEquals(IssueError.InvalidField("iat"), rejected(issuer.issue(IssueRequest("Anna", "1", LocalDate.of(10000, 1, 1)))))
        val badCid = CredentialIssuer(JcaEs256Sealer(TestMaterial.pkcs8("test")), "test", TestMaterial.trusted) { cid.uppercase() }
        assertEquals(IssueError.InvalidField("cid"), rejected(badCid.issue(IssueRequest("Anna", "1", iat))))
    }

    @Test
    fun selfVerificationBlocksWrongKeyAndCompromisedKid() {
        // Clau privada que no correspon a la pública de "test" ⇒ BAD_SIGNATURE ⇒ no s'emet.
        val wrong = CredentialIssuer(JcaEs256Sealer(TestMaterial.pkcs8("other")), "test", TestMaterial.trusted) { cid }
        assertEquals(IssueError.SelfVerificationFailed(VerificationCode.BAD_SIGNATURE), rejected(wrong.issue(IssueRequest("Anna", "1", iat))))
        val compromised = CredentialIssuer(JcaEs256Sealer(TestMaterial.pkcs8("testx")), "testx", TestMaterial.trusted) { cid }
        assertEquals(IssueError.SelfVerificationFailed(VerificationCode.KID_NOT_ACCEPTED), rejected(compromised.issue(IssueRequest("Anna", "1", iat))))
        val unknown = CredentialIssuer(JcaEs256Sealer(TestMaterial.pkcs8("test")), "s1", TestMaterial.trusted) { cid }
        assertEquals(IssueError.SelfVerificationFailed(VerificationCode.UNKNOWN_KID), rejected(unknown.issue(IssueRequest("Anna", "1", iat))))
        // Un segellador que retorna una firma defectuosa tampoc emet.
        val broken = CredentialIssuer({ ByteArray(64) }, "test", TestMaterial.trusted) { cid }
        assertEquals(IssueError.SelfVerificationFailed(VerificationCode.BAD_SIGNATURE), rejected(broken.issue(IssueRequest("Anna", "1", iat))))
        val throwing = CredentialIssuer({ throw SealException("x") }, "test", TestMaterial.trusted) { cid }
        assertIs<IssueError.SealFailed>(rejected(throwing.issue(IssueRequest("Anna", "1", iat))))
    }

    @Test
    fun defaultCidIsUuidV4Lowercase() {
        val r = issued(CredentialIssuer(JcaEs256Sealer(TestMaterial.pkcs8("test")), "test", TestMaterial.trusted).issue(IssueRequest("Anna", "1", iat)))
        assertTrue(Fields.CID.matches(r.credential.cid))
    }

    @Test
    fun publicKeyDerivedFromPrivateMatchesTrustedEntry() {
        // Correspondència privada↔pública (§3.7.9; base de la porta G2).
        val spki = P256.spkiFromPkcs8(TestMaterial.pkcs8("test"))!!
        assertTrue(spki.contentEquals(TestMaterial.trusted["test"]!!.spki))
        assertTrue(spki.contentEquals(TestMaterial.spki("test")))
    }
}
