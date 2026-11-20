package cat.futbolbotons.arbitratge.credential

/**
 * Analitzador JSON general i petit (RFC 8259) per a fitxers de configuració pública: llista de
 * confiança (§3.7.9) i fitxers de vectors. **No** s'usa per al payload ARB1 (vegeu
 * [RestrictedJson]). Rebutja noms duplicats i substituts aïllats.
 *
 * Valors: `Map<String, Any?>` (objecte, ordre conservat), `List<Any?>`, `String`, `Boolean`,
 * `null` i nombres com a [MiniJson.Number] (text literal).
 */
internal class MiniJson private constructor(private val s: String) {
    private var pos = 0

    data class Number(val literal: String)

    class ParseException(message: String) : Exception(message)

    companion object {
        fun parse(text: String): Any? {
            val p = MiniJson(text)
            p.ws()
            val v = p.value(0)
            p.ws()
            if (p.pos != text.length) throw ParseException("contingut sobrant a ${p.pos}")
            return v
        }
    }

    private fun fail(msg: String): Nothing = throw ParseException("$msg a $pos")

    private fun ws() {
        while (pos < s.length && (s[pos] == ' ' || s[pos] == '\t' || s[pos] == '\n' || s[pos] == '\r')) pos++
    }

    private fun value(depth: Int): Any? {
        if (depth > 64) fail("massa niuament")
        if (pos >= s.length) fail("final inesperat")
        return when (val c = s[pos]) {
            '{' -> obj(depth)
            '[' -> arr(depth)
            '"' -> str()
            't' -> lit("true", true)
            'f' -> lit("false", false)
            'n' -> lit("null", null)
            else -> if (c == '-' || c in '0'..'9') num() else fail("valor invàlid")
        }
    }

    private fun lit(word: String, v: Any?): Any? {
        if (!s.startsWith(word, pos)) fail("literal invàlid")
        pos += word.length
        return v
    }

    private fun num(): Number {
        val m = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?").matchAt(s, pos) ?: fail("nombre invàlid")
        pos += m.value.length
        return Number(m.value)
    }

    private fun obj(depth: Int): Map<String, Any?> {
        pos++
        val out = LinkedHashMap<String, Any?>()
        ws()
        if (pos < s.length && s[pos] == '}') {
            pos++
            return out
        }
        while (true) {
            ws()
            if (pos >= s.length || s[pos] != '"') fail("s'esperava un nom")
            val k = str()
            ws()
            if (pos >= s.length || s[pos] != ':') fail("s'esperava ':'")
            pos++
            ws()
            val v = value(depth + 1)
            if (out.containsKey(k)) fail("nom duplicat '$k'")
            out[k] = v
            ws()
            if (pos >= s.length) fail("final inesperat")
            when (s[pos++]) {
                ',' -> Unit
                '}' -> return out
                else -> fail("s'esperava ',' o '}'")
            }
        }
    }

    private fun arr(depth: Int): List<Any?> {
        pos++
        val out = ArrayList<Any?>()
        ws()
        if (pos < s.length && s[pos] == ']') {
            pos++
            return out
        }
        while (true) {
            ws()
            out.add(value(depth + 1))
            ws()
            if (pos >= s.length) fail("final inesperat")
            when (s[pos++]) {
                ',' -> Unit
                ']' -> return out
                else -> fail("s'esperava ',' o ']'")
            }
        }
    }

    private fun str(): String = strRaw().also { checkSurrogates(it) }

    private fun strRaw(): String {
        pos++
        val sb = StringBuilder()
        while (true) {
            if (pos >= s.length) fail("text sense tancar")
            val c = s[pos++]
            when {
                c == '"' -> return sb.toString()
                c == '\\' -> {
                    if (pos >= s.length) fail("escapament truncat")
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
                            if (pos + 4 > s.length) fail("escapament truncat")
                            val h = s.substring(pos, pos + 4)
                            if (!h.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) fail("hex invàlid")
                            sb.append(h.toInt(16).toChar())
                            pos += 4
                        }
                        else -> fail("escapament invàlid")
                    }
                }
                c.code < 0x20 -> fail("control sense escapar")
                else -> sb.append(c)
            }
        }
    }

    private fun checkSurrogates(t: String) {
        var i = 0
        while (i < t.length) {
            val c = t[i]
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= t.length || !Character.isLowSurrogate(t[i + 1])) fail("substitut aïllat")
                i += 2
            } else {
                if (Character.isLowSurrogate(c)) fail("substitut aïllat")
                i++
            }
        }
    }
}
