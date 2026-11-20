package cat.futbolbotons.arbitratge.credential

/** Valor d'un membre del payload: només text o enter (§3.7.4). */
sealed interface PayloadValue {
    data class Text(val value: String) : PayloadValue
    data class Integer(val value: Int) : PayloadValue
}

/**
 * Analitzador JSON restringit del payload ARB1 (§3.7.4), subconjunt de RFC 8259:
 *
 * - un únic objecte, amb espais insignificants (U+0020, U+0009, U+000A, U+000D);
 * - valors text o enter `0` / `[1-9][0-9]{0,8}`; res més (`true`, `false`, `null`, arrays,
 *   objectes, signe, fracció, exponent ⇒ rebuig);
 * - escapes de RFC 8259 §7, `\uXXXX` amb parells substituts vàlids; substitut aïllat ⇒ rebuig;
 * - caràcters de control < U+0020 sense escapar ⇒ rebuig (RFC 8259 §7);
 * - noms duplicats (comparats després de resoldre escapes) ⇒ rebuig;
 * - com a màxim [Arb1.MAX_MEMBERS] membres.
 *
 * Retorna `null` en qualsevol rebuig (el verificador ho tradueix a `BAD_PAYLOAD`).
 */
internal class RestrictedJson private constructor(private val s: String) {
    private var pos = 0

    private class Reject : RuntimeException(null, null, false, false)

    companion object {
        fun parseObject(text: String): LinkedHashMap<String, PayloadValue>? =
            try {
                RestrictedJson(text).parseTop()
            } catch (e: Reject) {
                null
            }
    }

    private fun fail(): Nothing = throw Reject()

    private fun parseTop(): LinkedHashMap<String, PayloadValue> {
        skipWs()
        val obj = parseObjectBody()
        skipWs()
        if (pos != s.length) fail()
        return obj
    }

    private fun skipWs() {
        while (pos < s.length) {
            when (s[pos]) {
                ' ', '\t', '\n', '\r' -> pos++
                else -> return
            }
        }
    }

    private fun expect(c: Char) {
        if (pos >= s.length || s[pos] != c) fail()
        pos++
    }

    private fun parseObjectBody(): LinkedHashMap<String, PayloadValue> {
        expect('{')
        val members = LinkedHashMap<String, PayloadValue>()
        skipWs()
        if (pos < s.length && s[pos] == '}') {
            pos++
            return members
        }
        while (true) {
            skipWs()
            val name = parseString()
            skipWs()
            expect(':')
            skipWs()
            val value = parseValue()
            if (members.containsKey(name)) fail()
            members[name] = value
            if (members.size > Arb1.MAX_MEMBERS) fail()
            skipWs()
            if (pos >= s.length) fail()
            when (s[pos]) {
                ',' -> pos++
                '}' -> {
                    pos++
                    return members
                }
                else -> fail()
            }
        }
    }

    private fun parseValue(): PayloadValue {
        if (pos >= s.length) fail()
        val c = s[pos]
        return when {
            c == '"' -> PayloadValue.Text(parseString())
            c in '0'..'9' -> parseInteger()
            else -> fail()
        }
    }

    private fun parseInteger(): PayloadValue.Integer {
        val start = pos
        if (s[pos] == '0') {
            pos++
        } else {
            while (pos < s.length && s[pos] in '0'..'9') pos++
        }
        val digits = pos - start
        if (digits > Arb1.MAX_INT_DIGITS) fail()
        // Després d'un enter només pot venir espai, ',' o '}' (rebutja `01`, `1.0`, `1e0`).
        if (pos < s.length) {
            when (s[pos]) {
                ' ', '\t', '\n', '\r', ',', '}' -> Unit
                else -> fail()
            }
        }
        return PayloadValue.Integer(s.substring(start, pos).toInt())
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (true) {
            if (pos >= s.length) fail()
            val c = s[pos++]
            when {
                c == '"' -> break
                c == '\\' -> {
                    if (pos >= s.length) fail()
                    when (s[pos++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val unit = readHex4()
                            when {
                                unit in 0xD800..0xDBFF -> {
                                    // Ha d'anar seguit immediatament d'un `\u` amb substitut baix.
                                    if (pos + 1 >= s.length || s[pos] != '\\' || s[pos + 1] != 'u') fail()
                                    pos += 2
                                    val low = readHex4()
                                    if (low !in 0xDC00..0xDFFF) fail()
                                    sb.append(unit.toChar()).append(low.toChar())
                                }
                                unit in 0xDC00..0xDFFF -> fail()
                                else -> sb.append(unit.toChar())
                            }
                        }
                        else -> fail()
                    }
                }
                c.code < 0x20 -> fail()
                // El text prové d'UTF-8 estricte: no hi pot haver substituts aïllats en cru.
                Character.isSurrogate(c) -> {
                    if (Character.isHighSurrogate(c) && pos < s.length && Character.isLowSurrogate(s[pos])) {
                        sb.append(c).append(s[pos++])
                    } else {
                        fail()
                    }
                }
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun readHex4(): Int {
        if (pos + 4 > s.length) fail()
        var v = 0
        repeat(4) {
            val d = Character.digit(s[pos], 16)
            // Character.digit accepta dígits no ASCII: només s'admet [0-9A-Fa-f].
            if (d < 0 || s[pos].code > 0x7F) fail()
            v = v * 16 + d
            pos++
        }
        return v
    }
}
