package dev.lluissunol.arbitratge.credential

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * UTF-8 estricte (RFC 3629 §3/§4) sense BOM inicial (§3.7.4).
 *
 * Primer es valida byte a byte amb la taula de RFC 3629 (independent del proveïdor de
 * `Charset` de cada plataforma: rebutja *overlong*, substituts codificats, bytes solts, seqüències
 * truncades i valors > U+10FFFF) i després es descodifica amb un `CharsetDecoder` en mode
 * `REPORT`. Mai `String(bytes, UTF_8)`, que substitueix en silenci.
 */
internal object StrictUtf8 {

    fun decode(bytes: ByteArray): String? {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return null
        }
        if (!isWellFormed(bytes)) return null
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            null
        }
    }

    /** Codifica a UTF-8 rebutjant substituts aïllats (en lloc de posar-hi `?`). */
    fun encode(text: String): ByteArray? {
        val encoder = StandardCharsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            val buf = encoder.encode(CharBuffer.wrap(text))
            ByteArray(buf.remaining()).also { buf.get(it) }
        } catch (e: CharacterCodingException) {
            null
        }
    }

    fun isWellFormed(bytes: ByteArray): Boolean {
        var i = 0
        val n = bytes.size
        while (i < n) {
            val b0 = bytes[i].toInt() and 0xFF
            when {
                b0 <= 0x7F -> i += 1
                b0 in 0xC2..0xDF -> {
                    if (!cont(bytes, i + 1, 0x80, 0xBF)) return false
                    i += 2
                }
                b0 in 0xE0..0xEF -> {
                    val (lo, hi) = when (b0) {
                        0xE0 -> 0xA0 to 0xBF
                        0xED -> 0x80 to 0x9F
                        else -> 0x80 to 0xBF
                    }
                    if (!cont(bytes, i + 1, lo, hi) || !cont(bytes, i + 2, 0x80, 0xBF)) return false
                    i += 3
                }
                b0 in 0xF0..0xF4 -> {
                    val (lo, hi) = when (b0) {
                        0xF0 -> 0x90 to 0xBF
                        0xF4 -> 0x80 to 0x8F
                        else -> 0x80 to 0xBF
                    }
                    if (!cont(bytes, i + 1, lo, hi) || !cont(bytes, i + 2, 0x80, 0xBF) ||
                        !cont(bytes, i + 3, 0x80, 0xBF)
                    ) {
                        return false
                    }
                    i += 4
                }
                else -> return false
            }
        }
        return true
    }

    private fun cont(bytes: ByteArray, i: Int, lo: Int, hi: Int): Boolean =
        i < bytes.size && (bytes[i].toInt() and 0xFF) in lo..hi
}
