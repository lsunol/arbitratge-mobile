package dev.lluissunol.arbitratge.buildtools

import dev.lluissunol.arbitratge.exam.BankParser
import java.io.File
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildToolsTest {
    private val sample = File(System.getProperty("bank.sample.path")).readText()

    @Test fun packBankDropsCanaryAndKeepsBankParsable() {
        assertTrue(sample.contains("canary"))
        val logs = mutableListOf<String>()
        val out = BuildTools.packBank(sample, release = false) { logs += it }
        assertFalse(out.contains("canary"))
        val bank = BankParser.parse(out)
        assertEquals(BankParser.parse(sample), bank)
        assertTrue(logs.single().contains("preguntes="))
    }

    @Test fun releaseRejectsSampleBank() {
        val e = assertFailsWith<BuildTools.ToolException> { BuildTools.packBank(sample, release = true) }
        assertTrue(e.message!!.contains("SAMPLE_IN_RELEASE"))
    }

    @Test fun invalidBankMessageHasNoBankText() {
        val e = assertFailsWith<BuildTools.ToolException> { BuildTools.packBank("{\"schema\":1,\"x\":\"SECRET-TEXT\"}", false) }
        assertFalse(e.message!!.contains("SECRET"))
    }

    private fun gen(curve: String) =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec(curve)) }.generateKeyPair().private.encoded

    @Test fun sealKeyChecks() {
        val pk8 = gen("secp256r1")
        assertEquals("abc1", BuildTools.checkSealKey(pk8, "abc1", release = true))
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(pk8, "dev", release = true) }
        assertEquals("dev", BuildTools.checkSealKey(pk8, "dev", release = false))
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(pk8, "BAD KID", release = false) }
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(ByteArray(10), "abc", release = false) }
    }

    @Test fun rejectsOtherCurvesEvenWhen256Bit() {
        // secp256k1 también tiene 256 bits: la comprobación por tamaño la aceptaba; la canónica no.
        val k1 = runCatching { gen("secp256k1") }.getOrNull() ?: return // proveedor sin secp256k1: nada que probar
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(k1, "abc", release = false) }
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(gen("secp384r1"), "abc", release = false) }
    }

    @Test fun releaseRejectsCommittedDebugAndTestKeysRegardlessOfKid() {
        val deny = BuildTools.denySpkis(listOf(File(System.getProperty("dev.key.path")), File(System.getProperty("test.keys.path"))))
        assertTrue(deny.size >= 3)
        val dev = File(System.getProperty("dev.key.path")).readBytes()
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(dev, "prod1", release = true, denySpkis = deny) }
        // la clave `test` de test-vectors, bajo un kid de apariencia legítima
        val b64 = Regex("\"name\": \"test\",[^}]*?\"pkcs8\": \"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
            .find(File(System.getProperty("test.keys.path")).readText())!!.groupValues[1]
        val testKey = java.util.Base64.getUrlDecoder().decode(b64)
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(testKey, "prod1", release = true, denySpkis = deny) }
        // en debug no se rechaza; una clave nueva sí pasa en release
        BuildTools.checkSealKey(dev, "dev", release = false, denySpkis = deny)
        BuildTools.checkSealKey(gen("secp256r1"), "prod1", release = true, denySpkis = deny)
    }
}
