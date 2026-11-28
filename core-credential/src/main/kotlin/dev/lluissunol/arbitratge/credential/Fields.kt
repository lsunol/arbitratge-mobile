package dev.lluissunol.arbitratge.credential

/** Patrons de camps `v=1` (§3.7.5). Totes les comprovacions són de cadena completa i ASCII. */
internal object Fields {
    val KID = Regex("[a-z0-9]{1,16}")
    val CID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
    val EXV = Regex("[0-9A-Za-z._-]{1,16}")
    val IAT = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
    val BV = Regex("[0-9A-Za-z._+-]{1,24}")
    val APP = Regex("[0-9A-Za-z._+-]{1,24}")

    private val REQUIRED_TEXT = listOf("cid", "nom", "res", "exv", "iat", "orig")

    /** Data gregoriana proleptica vàlida `AAAA-MM-DD` (qualsevol any 0000–9999, sense rang). */
    fun isValidIat(s: String): Boolean {
        if (!IAT.matches(s)) return false
        val y = s.substring(0, 4).toInt()
        val m = s.substring(5, 7).toInt()
        val d = s.substring(8, 10).toInt()
        if (m !in 1..12 || d < 1) return false
        val leap = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0
        val dim = when (m) {
            2 -> if (leap) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return d <= dim
    }

    /**
     * Pas 10 de §3.7.8. [members] ja ha passat el pas 6 (`v` enter igual a la versió, `kid`
     * vàlid). Retorna la credencial o `null` (⇒ `BAD_FIELDS`).
     */
    fun validate(members: Map<String, PayloadValue>): Arb1Credential? {
        if (members.keys.any { it in Arb1.RESERVED_NAMES }) return null
        val text = HashMap<String, String>()
        for (name in REQUIRED_TEXT) {
            val v = members[name] as? PayloadValue.Text ?: return null
            text[name] = v.value
        }
        val v = (members["v"] as PayloadValue.Integer).value
        val kid = (members["kid"] as PayloadValue.Text).value
        val cid = text.getValue("cid")
        val nom = text.getValue("nom")
        val res = text.getValue("res")
        val exv = text.getValue("exv")
        val iat = text.getValue("iat")
        val orig = text.getValue("orig")
        if (!CID.matches(cid)) return null
        if (NameRules.check(nom) != null) return null
        if (res != Arb1.RES_APTE) return null
        if (!EXV.matches(exv)) return null
        if (!isValidIat(iat)) return null
        if (orig != Arb1.ORIG_AUTOSERVEI) return null
        val bv = requiredText(members, "bv", BV) ?: return null
        val app = requiredText(members, "app", APP) ?: return null
        val ok = requiredInt(members, "ok", Arb1.OK_RANGE) ?: return null
        val tot = requiredInt(members, "tot", Arb1.TOT_RANGE) ?: return null
        val dur = requiredInt(members, "dur", Arb1.DUR_RANGE) ?: return null
        val fail = requiredInt(members, "fail", Arb1.FAIL_RANGE) ?: return null
        if (ok > tot) return null
        return Arb1Credential(v, kid, cid, nom, res, exv, iat, orig, ok, tot, dur, fail, bv, app)
    }

    /** Enter JSON obligatori dins del rang; absent, text o fora de rang ⇒ `null` (⇒ `BAD_FIELDS`). */
    private fun requiredInt(members: Map<String, PayloadValue>, name: String, range: IntRange): Int? {
        val i = members[name] as? PayloadValue.Integer ?: return null
        return if (i.value in range) i.value else null
    }

    /** Text obligatori que compleix el patró; absent, no text o fora de patró ⇒ `null` (⇒ `BAD_FIELDS`). */
    private fun requiredText(members: Map<String, PayloadValue>, name: String, pattern: Regex): String? {
        val t = members[name] as? PayloadValue.Text ?: return null
        return if (pattern.matches(t.value)) t.value else null
    }
}
