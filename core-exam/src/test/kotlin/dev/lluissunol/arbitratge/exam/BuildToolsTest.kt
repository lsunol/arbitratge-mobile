package dev.lluissunol.arbitratge.exam

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

    @Test fun sealKeyChecks() {
        val g = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }
        val pk8 = g.generateKeyPair().private.encoded
        assertEquals("abc1", BuildTools.checkSealKey(pk8, "abc1", release = true))
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(pk8, "dev", release = true) }
        assertEquals("dev", BuildTools.checkSealKey(pk8, "dev", release = false))
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(pk8, "BAD KID", release = false) }
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(ByteArray(10), "abc", release = false) }
        val g384 = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp384r1")) }
        assertFailsWith<BuildTools.ToolException> { BuildTools.checkSealKey(g384.generateKeyPair().private.encoded, "abc", false) }
    }
}
