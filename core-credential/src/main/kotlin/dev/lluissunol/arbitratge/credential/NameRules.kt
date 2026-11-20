package dev.lluissunol.arbitratge.credential

import java.text.Normalizer

/** Motiu de rebuig d'un `nom` (§3.7.5.1). Els textos en català són de `04`. */
enum class NameError {
    /** 0 punts de codi (buit després de normalitzar). */
    EMPTY,

    /** Més de 80 punts de codi. */
    TOO_LONG,

    /** Espai inicial o final, o dos espais seguits (regla 2). */
    BAD_SPACES,

    /** Conté un caràcter del conjunt F (regla 3). */
    FORBIDDEN_CHARACTER,

    /** Text Java mal format (substitut UTF-16 aïllat): no es pot codificar en UTF-8. */
    MALFORMED,
}

/** Resultat de construir el nom a partir del que tecleja l'usuari. */
sealed interface NameResult {
    data class Valid(val nom: String) : NameResult
    data class Invalid(val error: NameError) : NameResult
}

/** Regles de `nom` (§3.7.5.1). */
object NameRules {

    /** Regla 3: conjunt fix F, independent de la versió d'Unicode de la plataforma. */
    fun isForbidden(cp: Int): Boolean =
        cp in 0x0000..0x001F || cp in 0x007F..0x009F || cp == 0x00A0 || cp == 0x00AD ||
            cp == 0x061C || cp == 0x1680 || cp == 0x180E || cp in 0x2000..0x200F ||
            cp in 0x2028..0x202F || cp in 0x205F..0x206F || cp == 0x3000 || cp == 0xFEFF ||
            cp in 0xFFF9..0xFFFB

    /** Regles 1–3 (les comprova l'emissor i el verificador després de la firma). No comprova NFC. */
    fun check(nom: String): NameError? {
        if (hasLoneSurrogate(nom)) return NameError.MALFORMED
        val count = nom.codePointCount(0, nom.length)
        if (count == 0) return NameError.EMPTY
        if (count > Arb1.MAX_NOM_CODE_POINTS) return NameError.TOO_LONG
        if (nom.startsWith(' ') || nom.endsWith(' ') || nom.contains("  ")) return NameError.BAD_SPACES
        var i = 0
        while (i < nom.length) {
            val cp = nom.codePointAt(i)
            if (isForbidden(cp)) return NameError.FORBIDDEN_CHARACTER
            i += Character.charCount(cp)
        }
        return null
    }

    /** Caràcters que l'app substitueix per U+0020 abans de col·lapsar (§3.7.5.1). */
    private fun isSubstituted(c: Char): Boolean {
        val cp = c.code
        return cp == 0x0009 || cp == 0x000A || cp == 0x000D || cp == 0x00A0 || cp == 0x1680 ||
            cp in 0x2000..0x200A || cp == 0x202F || cp == 0x205F || cp == 0x3000
    }

    /**
     * Construcció del nom a l'app (§3.7.5.1): substitucions → U+0020, col·lapse d'espais, retall
     * d'U+0020 als extrems, NFC i regles 1–3.
     */
    fun normalize(input: String): NameResult {
        if (hasLoneSurrogate(input)) return NameResult.Invalid(NameError.MALFORMED)
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val c = if (isSubstituted(ch)) ' ' else ch
            if (c == ' ' && sb.isNotEmpty() && sb[sb.length - 1] == ' ') continue
            sb.append(c)
        }
        val trimmed = sb.toString().trim { it == ' ' }
        val nfc = Normalizer.normalize(trimmed, Normalizer.Form.NFC)
        val error = check(nfc)
        return if (error == null) NameResult.Valid(nfc) else NameResult.Invalid(error)
    }

    internal fun hasLoneSurrogate(s: String): Boolean {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= s.length || !Character.isLowSurrogate(s[i + 1])) return true
                i += 2
            } else {
                if (Character.isLowSurrogate(c)) return true
                i++
            }
        }
        return false
    }
}
