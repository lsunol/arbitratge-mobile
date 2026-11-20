package cat.futbolbotons.arbitratge.credential.vectors

/**
 * Escriptor JSON mínim per als fitxers de `test-vectors/`. Escapa tot el que no és ASCII
 * imprimible (`\uXXXX`, parells substituts per a fora del BMP) perquè els fitxers siguin ASCII
 * pur i no depenguin de cap codificació d'editor.
 */
internal object JsonOut {
    fun str(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\t' -> sb.append("\\t")
                c == '\r' -> sb.append("\\r")
                c.code < 0x20 || c.code > 0x7E -> sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> sb.append(c)
            }
        }
        return sb.append('"').toString()
    }

    fun value(v: Any?, indent: String = ""): String = when (v) {
        null -> "null"
        is String -> str(v)
        is Int, is Long, is Boolean -> v.toString()
        is Map<*, *> -> obj(v, indent)
        is List<*> -> if (v.isEmpty()) "[]" else v.joinToString(",\n", "[\n", "\n$indent]") { indent + "  " + value(it, "$indent  ") }
        else -> error("tipus no admès: ${v::class}")
    }

    /** Objecte en una sola línia si és petit; si no, un membre per línia. */
    fun obj(m: Map<*, *>, indent: String = ""): String {
        if (m.isEmpty()) return "{}"
        val inline = m.entries.joinToString(", ", "{", "}") { str(it.key as String) + ": " + value(it.value, indent) }
        if (inline.length <= 100 && m.values.none { it is Map<*, *> || it is List<*> }) return inline
        return m.entries.joinToString(",\n", "{\n", "\n$indent}") {
            "$indent  " + str(it.key as String) + ": " + value(it.value, "$indent  ")
        }
    }
}
