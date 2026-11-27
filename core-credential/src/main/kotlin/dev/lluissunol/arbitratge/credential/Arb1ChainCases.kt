package dev.lluissunol.arbitratge.credential

import java.time.LocalDate

/**
 * Definició única dels casos V01–V05 i V12 de `02` §3.7.11 que genera l'**emissor de producció** amb
 * `kid=test` (cadena "Android signa → PWA verifica", porta G5 de `05` §5.8). La usen el generador
 * de `test-vectors/` (JVM) i la prova instrumentada de l'APK de test (API 26+), perquè tots dos
 * emetin exactament les mateixes peticions.
 *
 * Com que ECDSA no és determinista, a G5 no es compara el text del token sinó el codi de
 * verificació i els camps descodificats ([ChainCase.expected], amb el `cid` injectat).
 */
@Arb1TestInstrumentation
object Arb1ChainCases {
    const val IAT = "2026-11-20"
    const val NOM_ASCII = "Anna Exemple Puig"

    /** Accents i «l·l» amb U+00B7, en NFC. */
    const val NOM_ACCENTS = "N\u00faria Gal\u00b7la P\u00e9rez"

    /** 80 punts de codi amb un caràcter fora del BMP (U+20BB7). */
    val NOM_80: String = run {
        val words = listOf(
            "\uD842\uDFB7\u7530", "Maria", "Ant\u00f2nia", "Exemple", "Puig", "Ferrer", "Font", "Vidal", "Serra",
            "Pons", "Roca", "Soler", "Casals", "Riera", "Bosch", "Camps", "Mas", "Vila",
        )
        var s = words.joinToString(" ")
        s = s.substring(0, s.offsetByCodePoints(0, Arb1.MAX_NOM_CODE_POINTS))
        if (s.endsWith(" ")) s = s.dropLast(1) + "a"
        check(s.codePointCount(0, s.length) == Arb1.MAX_NOM_CODE_POINTS)
        s
    }

    /**
     * @param escapeNonAscii `true` només per a V03: el mateix emissor amb
     *   [CredentialIssuer.withNonAsciiEscapes] (escapes `\u00XX` al JSON).
     */
    class ChainCase(
        val id: String,
        val description: String,
        val request: IssueRequest,
        val escapeNonAscii: Boolean,
        val nom: String,
        val bv: String? = null,
        val app: String? = null,
        val ok: Int? = null,
        val tot: Int? = null,
        val dur: Int? = null,
    ) {
        /** Camps que el verificador ha de retornar (literalment) amb `code == OK`. */
        fun expected(kid: String, cid: String): Arb1Credential =
            Arb1Credential(Arb1.VERSION, kid, cid, nom, Arb1.RES_APTE, "1", IAT, Arb1.ORIG_AUTOSERVEI, bv, app, ok, tot, dur)

        /** Emet el cas amb l'emissor de producció (o la seva variant amb escapes per a V03). */
        fun issue(sealer: CredentialSealer, kid: String, trustedKeys: TrustedKeys, cid: () -> String): IssueResult {
            val issuer = if (escapeNonAscii) {
                CredentialIssuer.withNonAsciiEscapes(sealer, kid, trustedKeys, cid)
            } else {
                CredentialIssuer(sealer, kid, trustedKeys, cid)
            }
            return issuer.issue(request)
        }
    }

    private fun req(nom: String, bv: String? = null, app: String? = null, ok: Int? = null, tot: Int? = null, dur: Int? = null) =
        IssueRequest(nom, "1", LocalDate.parse(IAT), bv, app, ok = ok, tot = tot, dur = dur)

    val cases: List<ChainCase> = listOf(
        ChainCase("V01", "Només obligatoris, nom ASCII", req(NOM_ASCII), false, NOM_ASCII),
        ChainCase("V02", "Accents i l·l en UTF-8 directe", req(NOM_ACCENTS), false, NOM_ACCENTS),
        ChainCase("V03", "Mateix nom que V02 amb escapes \\u00XX", req(NOM_ACCENTS), true, NOM_ACCENTS),
        ChainCase("V04", "nom de 80 punts de codi amb un caràcter fora del BMP", req(NOM_80), false, NOM_80),
        ChainCase("V05", "Amb bv i app", req(NOM_ASCII, "2026.11.1", "1.0.0+5"), false, NOM_ASCII, "2026.11.1", "1.0.0+5"),
        ChainCase(
            "V12", "Amb resultat ok, tot i dur (i bv)",
            req(NOM_ACCENTS, bv = "2026.11.1", ok = 27, tot = 30, dur = 1834), false, NOM_ACCENTS,
            bv = "2026.11.1", ok = 27, tot = 30, dur = 1834,
        ),
    )
}
