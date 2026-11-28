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
        val bv: String = BV,
        val app: String = APP,
        val ok: Int = OK,
        val tot: Int = TOT,
        val dur: Int = DUR,
        val fail: Int = FAIL,
    ) {
        /** Camps que el verificador ha de retornar (literalment) amb `code == OK`. */
        fun expected(kid: String, cid: String): Arb1Credential =
            Arb1Credential(Arb1.VERSION, kid, cid, nom, Arb1.RES_APTE, "1", IAT, Arb1.ORIG_AUTOSERVEI, ok, tot, dur, fail, bv, app)

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

    /** Resultat per defecte dels casos de cadena (camps obligatoris). */
    const val OK = 27
    const val TOT = 30
    const val DUR = 1834
    const val FAIL = 0

    const val BV = "2026.11.1"
    const val APP = "1.0.0+5"

    private fun req(nom: String, bv: String = BV, app: String = APP, ok: Int = OK, tot: Int = TOT, dur: Int = DUR, fail: Int = FAIL) =
        IssueRequest(nom, "1", LocalDate.parse(IAT), ok, tot, dur, fail, bv, app)

    val cases: List<ChainCase> = listOf(
        ChainCase("V01", "Només camps obligatoris, nom ASCII", req(NOM_ASCII), false, NOM_ASCII),
        ChainCase("V02", "Accents i l·l en UTF-8 directe", req(NOM_ACCENTS), false, NOM_ACCENTS),
        ChainCase("V03", "Mateix nom que V02 amb escapes \\u00XX", req(NOM_ACCENTS), true, NOM_ACCENTS),
        ChainCase("V04", "nom de 80 punts de codi amb un caràcter fora del BMP", req(NOM_80), false, NOM_80),
        ChainCase("V05", "bv i app diferents dels valors per defecte", req(NOM_ASCII, "2027.01.0", "0.9.0-rc.1"), false, NOM_ASCII, "2027.01.0", "0.9.0-rc.1"),
        ChainCase(
            "V12", "Amb ok, tot, dur i fail=2 diferents dels valors per defecte",
            req(NOM_ACCENTS, ok = 25, tot = 30, dur = 1500, fail = 2), false, NOM_ACCENTS,
            ok = 25, tot = 30, dur = 1500, fail = 2,
        ),
    )
}
