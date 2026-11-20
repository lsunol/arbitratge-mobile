package dev.lluissunol.arbitratge.data

import dev.lluissunol.arbitratge.credential.ArbCredentialIssuing
import dev.lluissunol.arbitratge.credential.CredentialVerifier
import dev.lluissunol.arbitratge.credential.IssueRequest
import dev.lluissunol.arbitratge.credential.IssueResult
import dev.lluissunol.arbitratge.credential.P256
import dev.lluissunol.arbitratge.credential.TrustedKeys
import dev.lluissunol.arbitratge.credential.VerificationCode
import dev.lluissunol.arbitratge.credential.QrEncoder
import dev.lluissunol.arbitratge.platform.SealMaterial
import dev.lluissunol.arbitratge.support.TestEnv
import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DevSealKeyTest {
    @Test fun committedTrustedEntryMatchesPrivateKey() {
        val entry = TrustedKeys.parseEntry(TestEnv.devTrustedJson, "dev").getOrThrow()
        assertContentEquals(entry.spki, P256.spkiFromPkcs8(TestEnv.devKey))
        assertEquals("dev", entry.kid)
    }

    @Test fun appIssuerIssuesTokenVerifiableOnlyWithDevKey() {
        val issuing = ArbCredentialIssuing(SealMaterial(TestEnv.devKey, "dev"))
        val cid = "7f3a92c1-4b5d-4e6f-8a9b-000000000001"
        val r = issuing.issue(cid, IssueRequest("Mar\u0069\u0301a  Puig", "sample-1", LocalDate.of(2026, 11, 20), "0.0.0-sample"))
        val issued = assertIs<IssueResult.Issued>(r)
        assertEquals("Mar\u00EDa Puig", issued.credential.nom)
        assertEquals("7F3A-92C1", issued.credential.shortCode)
        assertEquals(VerificationCode.OK, CredentialVerifier(TestEnv.devTrusted()).verify(issued.token).code)
        // la PWA de producción no incluye `dev`: sería NO VÀLIDA (UNKNOWN_KID)
        assertEquals(VerificationCode.UNKNOWN_KID, CredentialVerifier(TrustedKeys.EMPTY).verify(issued.token).code)
    }

    @Test fun qrEncodesExactTokenAtReasonableVersion() {
        val token = TestEnv.realIssuing.issue("7f3a92c1-4b5d-4e6f-8a9b-000000000001", IssueRequest("Maria Puig Vila", "sample-1", LocalDate.of(2026, 11, 20), "0.0.0-sample"))
        val t = assertIs<IssueResult.Issued>(token).token
        val m = QrEncoder.encode(t)
        assertTrue(m.size in 45..90, "versió QR raonable: ${m.size}") // v8..v18
        // los tres localizadores de la esquina superior izquierda: módulo oscuro en (0,0) y (6,0)
        assertTrue(m[0, 0] && m[6, 0] && m[0, 6])
    }
}
