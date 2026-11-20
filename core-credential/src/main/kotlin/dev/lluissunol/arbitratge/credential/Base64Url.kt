package dev.lluissunol.arbitratge.credential

/**
 * Base64url sense farciment (RFC 4648 §5) amb rebuig de codificacions no canòniques (§3.7.1).
 *
 * No depèn de `java.util.Base64`: la regla normativa és "descodificar i tornar a codificar ha de
 * reproduir exactament el segment".
 */
object Base64Url {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    private val REVERSE = IntArray(128) { -1 }.also { table ->
        ALPHABET.forEachIndexed { i, c -> table[c.code] = i }
    }

    fun isAlphabet(c: Char): Boolean = c.code < 128 && REVERSE[c.code] >= 0

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder((bytes.size * 4 + 2) / 3)
        var i = 0
        while (i + 3 <= bytes.size) {
            val n = (bytes[i].toInt() and 0xFF shl 16) or (bytes[i + 1].toInt() and 0xFF shl 8) or
                (bytes[i + 2].toInt() and 0xFF)
            sb.append(ALPHABET[n ushr 18 and 63]).append(ALPHABET[n ushr 12 and 63])
                .append(ALPHABET[n ushr 6 and 63]).append(ALPHABET[n and 63])
            i += 3
        }
        when (bytes.size - i) {
            1 -> {
                val n = bytes[i].toInt() and 0xFF shl 16
                sb.append(ALPHABET[n ushr 18 and 63]).append(ALPHABET[n ushr 12 and 63])
            }
            2 -> {
                val n = (bytes[i].toInt() and 0xFF shl 16) or (bytes[i + 1].toInt() and 0xFF shl 8)
                sb.append(ALPHABET[n ushr 18 and 63]).append(ALPHABET[n ushr 12 and 63])
                    .append(ALPHABET[n ushr 6 and 63])
            }
        }
        return sb.toString()
    }

    /**
     * Descodifica un segment canònic. Retorna `null` si hi ha caràcters fora de l'alfabet, si la
     * longitud és ≡ 1 (mod 4) o si la recodificació no reprodueix exactament el text.
     */
    fun decodeCanonical(text: String): ByteArray? {
        if (text.length % 4 == 1) return null
        for (c in text) if (!isAlphabet(c)) return null
        val out = ByteArray(text.length * 3 / 4)
        var acc = 0
        var bits = 0
        var o = 0
        for (c in text) {
            acc = (acc shl 6) or REVERSE[c.code]
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[o++] = (acc ushr bits and 0xFF).toByte()
            }
            acc = acc and ((1 shl bits) - 1)
        }
        val result = if (o == out.size) out else out.copyOf(o)
        return if (encode(result) == text) result else null
    }
}
