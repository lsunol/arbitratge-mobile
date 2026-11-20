package cat.futbolbotons.arbitratge.credential

import java.math.BigInteger
import java.security.SecureRandom
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** DER → R‖S (§3.7.3) amb INTEGER de 31, 32 i 33 bytes, i rebuig de qualsevol altra forma. */
class DerConversionTest {
    private fun integer(content: ByteArray) = byteArrayOf(0x02, content.size.toByte()) + content
    private fun seq(vararg parts: ByteArray): ByteArray {
        val body = parts.fold(ByteArray(0)) { a, b -> a + b }
        return byteArrayOf(0x30, body.size.toByte()) + body
    }

    private fun bytes(n: Int, first: Int, fill: Int = 0x11) = ByteArray(n) { if (it == 0) first.toByte() else fill.toByte() }

    @Test
    fun integersOf31_32_33Bytes() {
        val r31 = bytes(31, 0x7a)
        val s32 = bytes(32, 0x12)
        val s33 = byteArrayOf(0) + bytes(32, 0xff - 0x01) // 0x00 + valor amb bit alt
        val raw = P256.derToRaw(seq(integer(r31), integer(s33)))!!
        assertContentEquals(byteArrayOf(0) + r31, raw.copyOfRange(0, 32))
        assertContentEquals(s33.copyOfRange(1, 33), raw.copyOfRange(32, 64))

        val raw2 = P256.derToRaw(seq(integer(s32), integer(r31)))!!
        assertContentEquals(s32, raw2.copyOfRange(0, 32))
        assertContentEquals(byteArrayOf(0) + r31, raw2.copyOfRange(32, 64))

        // 32 bytes que comencen per 0x00 (valor de 31 bytes amb bit alt): forma DER vàlida.
        val r32z = byteArrayOf(0) + bytes(31, 0x80)
        assertContentEquals(r32z, P256.derToRaw(seq(integer(r32z), integer(s32)))!!.copyOfRange(0, 32))

        // Valors molt curts (1 byte) es farceixen a 32.
        val tiny = P256.derToRaw(seq(integer(byteArrayOf(1)), integer(byteArrayOf(0x7f))))!!
        assertEquals(BigInteger.ONE, P256.rOf(tiny))
        assertEquals(BigInteger.valueOf(0x7f), P256.sOf(tiny))
    }

    @Test
    fun rejectsAnyOtherForm() {
        val ok = integer(bytes(32, 0x12))
        assertNull(P256.derToRaw(seq(integer(bytes(33, 0x01)), ok)), "33 bytes sense 0x00 inicial")
        assertNull(P256.derToRaw(seq(integer(byteArrayOf(0, 0) + bytes(32, 0x80)), ok)), "34 bytes")
        assertNull(P256.derToRaw(seq(integer(ByteArray(0)), ok)), "INTEGER buit")
        assertNull(P256.derToRaw(seq(integer(bytes(32, 0x80)), ok)), "negatiu")
        assertNull(P256.derToRaw(seq(ok)), "un sol INTEGER")
        assertNull(P256.derToRaw(seq(ok, ok, ok)), "tres INTEGER")
        assertNull(P256.derToRaw(seq(ok, ok) + byteArrayOf(0)), "bytes sobrants")
        assertNull(P256.derToRaw(byteArrayOf(0x31) + seq(ok, ok).copyOfRange(1, 70)), "no és SEQUENCE")
        assertNull(P256.derToRaw(seq(ok, byteArrayOf(0x03, 0x20) + bytes(32, 0x12))), "tipus no INTEGER")
        // Codificacions no mínimes (zeros inicials redundants).
        assertNull(P256.derToRaw(seq(integer(byteArrayOf(0) + bytes(32, 0x12)), ok)), "33 bytes amb 0x00 + byte sense bit alt")
        assertNull(P256.derToRaw(seq(ok, integer(byteArrayOf(0) + bytes(32, 0x7f)))), "33 bytes no mínim a s")
        assertNull(P256.derToRaw(seq(integer(byteArrayOf(0) + bytes(31, 0x12)), ok)), "32 bytes amb 0x00 redundant")
        assertNull(P256.derToRaw(seq(integer(byteArrayOf(0) + bytes(30, 0x7a)), ok)), "31 bytes amb 0x00 redundant")
        assertNull(P256.derToRaw(seq(integer(byteArrayOf(0, 0)), ok)), "zero amb 2 bytes")
        assertNull(P256.derToRaw(seq(integer(byteArrayOf(0, 1)), ok)), "u amb 0x00 redundant")
        assertNull(P256.derToRaw(seq(integer(byteArrayOf(0, 0) + bytes(31, 0x80)), ok)), "33 bytes amb dos 0x00")
        // Negatius en qualsevol longitud.
        assertNull(P256.derToRaw(seq(integer(byteArrayOf(0x80.toByte())), ok)), "negatiu d'1 byte")
        assertNull(P256.derToRaw(seq(integer(bytes(31, 0xff)), ok)), "negatiu de 31 bytes")
        assertNull(P256.derToRaw(seq(ok, integer(bytes(32, 0x80)))), "s negatiu")
        val badLen = seq(ok, ok).also { it[1] = (it[1] + 1).toByte() }
        assertNull(P256.derToRaw(badLen), "longitud de SEQUENCE incoherent")
        assertNull(P256.derToRaw(ByteArray(0)))
    }

    @Test
    fun rawToDerRoundTrip() {
        val rnd = SecureRandom()
        repeat(200) {
            val raw = ByteArray(64).also { rnd.nextBytes(it) }
            if (it % 3 == 0) raw.fill(0, 0, 1 + it % 5)
            if (it % 5 == 0) raw.fill(0, 32, 33 + it % 3)
            assertContentEquals(raw, P256.derToRaw(P256.rawToDer(raw)))
        }
    }

    @Test
    fun realJcaSignaturesConvertIncludingShortIntegers() {
        // Firmes reals del proveïdor: busca almenys una amb r o s de menys de 32 bytes significatius.
        val pkcs8 = TestMaterial.pkcs8("test")
        val key = TestMaterial.trusted["test"]!!
        val msg = "ARB1.eyJ2IjoxfQ".toByteArray()
        val sealer = JcaEs256Sealer(pkcs8)
        var shortSeen = false
        var i = 0
        while (i < 2000 && !(shortSeen && i >= 50)) {
            val raw = sealer.seal(msg)
            assertEquals(64, raw.size)
            assertTrue(CredentialVerifier.verifyEs256(key, msg, raw))
            if (raw[0] == 0.toByte() || raw[32] == 0.toByte()) shortSeen = true
            i++
        }
        assertTrue(shortSeen, "no s'ha vist cap INTEGER curt en $i firmes")
    }
}
