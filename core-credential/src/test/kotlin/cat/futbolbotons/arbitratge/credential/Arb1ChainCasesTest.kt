@file:OptIn(Arb1TestInstrumentation::class)

package cat.futbolbotons.arbitratge.credential

import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Cadena G5 en JVM: V01–V05 emesos per l'emissor de producció (V03 amb la instrumentació
 * d'escapes) i verificats amb codi `OK` i camps literals. La mateixa prova s'ha d'executar a
 * l'APK de test (API 26+) amb el verificador del dispositiu i la PWA.
 */
class Arb1ChainCasesTest {
    private val sealer = JcaEs256Sealer(TestMaterial.pkcs8("test"))
    private val verifier = CredentialVerifier(TestMaterial.trusted)
    private val vectors = ((MiniJson.parse(File(TestMaterial.dir, "cases.json").readText()) as Map<*, *>)["cases"] as List<*>)
        .map { it as Map<*, *> }

    private fun payloadJson(token: String) = String(Base64Url.decodeCanonical(token.split('.')[1])!!, Charsets.UTF_8)

    @Test
    fun v01ToV05AreIssuedByTheIssuerAndVerifyOk() {
        assertEquals(listOf("V01", "V02", "V03", "V04", "V05"), Arb1ChainCases.cases.map { it.id })
        for (case in Arb1ChainCases.cases) {
            val cid = "3f0c1e9a-5b7d-4c2e-9a10-6d8b2f4e7c0" + case.id.last()
            val issued = assertIs<IssueResult.Issued>(case.issue(sealer, "test", TestMaterial.trusted) { cid }, case.id)
            val r = verifier.verify(issued.token)
            assertEquals(VerificationCode.OK, r.code, case.id)
            assertEquals(case.expected("test", cid), r.credential, case.id)
            // Els camps esperats coincideixen amb els del vector fix del mateix id (excepte cid).
            val vf = vectors.single { it["id"] == case.id }["fields"] as Map<*, *>
            assertEquals(vf["nom"], case.nom, case.id)
            assertEquals(vf["bv"], case.bv, case.id)
            assertEquals(vf["app"], case.app, case.id)
        }
    }

    @Test
    fun instrumentedIssuerReproducesTheFixedV03PayloadByteForByte() {
        val v03 = vectors.single { it["id"] == "V03" }
        val cid = (v03["fields"] as Map<*, *>)["cid"] as String
        val case = Arb1ChainCases.cases.single { it.id == "V03" }
        val issued = assertIs<IssueResult.Issued>(case.issue(sealer, "test", TestMaterial.trusted) { cid })
        assertEquals((v03["input"] as String).split('.')[1], issued.token.split('.')[1])
        val json = payloadJson(issued.token)
        assertTrue(json.contains("\"nom\":\"N\\u00faria Gal\\u00b7la P\\u00e9rez\""), json)
        assertTrue(json.all { it.code < 0x80 })
    }

    @Test
    fun productionIssuerNeverEscapesAndEscapesHandleNonBmp() {
        val iat = LocalDate.of(2026, 11, 20)
        val cid = "3f0c1e9a-5b7d-4c2e-9a10-6d8b2f4e7c01"
        val prod = assertIs<IssueResult.Issued>(
            CredentialIssuer(sealer, "test", TestMaterial.trusted) { cid }.issue(IssueRequest(Arb1ChainCases.NOM_ACCENTS, "1", iat)),
        )
        assertFalse(payloadJson(prod.token).contains("\\u"))
        val esc = assertIs<IssueResult.Issued>(
            CredentialIssuer.withNonAsciiEscapes(sealer, "test", TestMaterial.trusted) { cid }
                .issue(IssueRequest(Arb1ChainCases.NOM_80, "1", iat)),
        )
        assertTrue(payloadJson(esc.token).contains("\\ud842\\udfb7\\u7530"))
        assertEquals(Arb1ChainCases.NOM_80, esc.credential.nom)
        assertEquals(VerificationCode.OK, verifier.verify(esc.token).code)
    }
}
