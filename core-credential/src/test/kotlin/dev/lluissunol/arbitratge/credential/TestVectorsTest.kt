package dev.lluissunol.arbitratge.credential

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Porta G1 (JVM): tots els casos públics de `test-vectors/cases.json` (`02` §3.7.11) amb el codi
 * exacte i, per a `OK`, els camps descodificats literals.
 */
class TestVectorsTest {
    private val dir = File(System.getProperty("arb1.testVectorsDir") ?: "../test-vectors")
    private val trusted = TrustedKeys.parseList(File(dir, "trusted-keys-test.json").readText())
    private val doc = MiniJson.parse(File(dir, "cases.json").readText()) as Map<*, *>
    private val cases = (doc["cases"] as List<*>).map { it as Map<*, *> }

    @Test
    fun trustListIsCoherent() {
        assertTrue(trusted.discarded.isEmpty(), "entrades descartades: ${trusted.discarded}")
        assertEquals(setOf("test", "testx"), trusted.kids)
        assertEquals(KeyStatus.ACTIVE, trusted["test"]!!.status)
        assertEquals(KeyStatus.COMPROMISED, trusted["testx"]!!.status)
    }

    /**
     * Cada variant enumerada a §3.7.11, per id estable (`grup-variant`). Llista independent del
     * generador: si un cas desapareix o canvia d'id, la prova falla.
     */
    private val requiredIds: Map<String, String> = linkedMapOf(
        // Vàlids
        "V01" to "OK", "V02" to "OK", "V03" to "OK", "V04" to "OK", "V05" to "OK", "V06" to "OK",
        "V07" to "OK", "V08" to "OK", "V09" to "OK", "V10" to "OK", "V11" to "OK",
        // V12 ok/tot/dur (cadena G5), V13 ok/tot sense dur, V14 només dur
        "V12" to "OK", "V13" to "OK", "V14" to "OK",
        // F01 buit o només espais
        "F01-empty" to "BAD_FORMAT", "F01-spaces" to "BAD_FORMAT", "F01-removable-only" to "BAD_FORMAT",
        // F02 https://… i HTTP://…
        "F02-https" to "IS_URL", "F02-http-upper" to "IS_URL",
        // F03 entrada > 2048
        "F03-token-plus-spaces" to "BAD_FORMAT", "F03-2049-chars" to "BAD_FORMAT",
        // F04 arb1., ARB0., ARB01., ARB1 sense punt
        "F04-lowercase" to "BAD_FORMAT", "F04-arb0" to "BAD_FORMAT", "F04-arb01" to "BAD_FORMAT",
        "F04-no-dot" to "BAD_FORMAT", "F04-no-dot-with-rest" to "BAD_FORMAT",
        // F05 2 o 4 segments
        "F05-two-segments" to "BAD_FORMAT", "F05-four-segments" to "BAD_FORMAT", "F05-trailing-dot" to "BAD_FORMAT",
        // F06 amb '=', amb '+' o '/', amb caràcter no ASCII
        "F06-padding-payload" to "BAD_FORMAT", "F06-padding-signature" to "BAD_FORMAT", "F06-plus" to "BAD_FORMAT",
        "F06-slash" to "BAD_FORMAT", "F06-non-ascii" to "BAD_FORMAT",
        // F07 no canònic en payload i en firma
        "F07-payload-noncanonical" to "BAD_FORMAT", "F07-signature-noncanonical" to "BAD_FORMAT",
        "F07-length-1-mod-4" to "BAD_FORMAT",
        // F08 firma de 63 o 65 bytes, firma DER
        "F08-63-bytes" to "BAD_FORMAT", "F08-65-bytes" to "BAD_FORMAT", "F08-der" to "BAD_FORMAT",
        // F09 payload buit o de 513 bytes, token > 1024
        "F09-empty-payload" to "BAD_FORMAT", "F09-513-bytes" to "BAD_FORMAT", "F09-token-over-1024" to "BAD_FORMAT",
        // U01 ARB2. + resta vàlida, ARB2.xyz, ARB999.
        "U01-arb2-valid-rest" to "UNKNOWN_VERSION", "U01-arb2-xyz" to "UNKNOWN_VERSION", "U01-arb999" to "UNKNOWN_VERSION",
        // P01 overlong, substitut codificat, byte solt
        "P01-overlong-c1" to "BAD_PAYLOAD", "P01-overlong-c0" to "BAD_PAYLOAD", "P01-overlong-e0" to "BAD_PAYLOAD",
        "P01-encoded-surrogate" to "BAD_PAYLOAD", "P01-cesu8-pair" to "BAD_PAYLOAD", "P01-stray-continuation" to "BAD_PAYLOAD",
        "P01-truncated" to "BAD_PAYLOAD", "P01-byte-ff" to "BAD_PAYLOAD", "P01-above-10ffff" to "BAD_PAYLOAD",
        // P02 BOM
        "P02" to "BAD_PAYLOAD",
        // P03 no objecte, mal format, coma final
        "P03-array" to "BAD_PAYLOAD", "P03-text" to "BAD_PAYLOAD", "P03-integer" to "BAD_PAYLOAD",
        "P03-unclosed" to "BAD_PAYLOAD", "P03-trailing-comma" to "BAD_PAYLOAD", "P03-trailing-garbage" to "BAD_PAYLOAD",
        "P03-two-objects" to "BAD_PAYLOAD", "P03-single-quotes" to "BAD_PAYLOAD", "P03-unquoted-name" to "BAD_PAYLOAD",
        "P03-raw-control" to "BAD_PAYLOAD", "P03-bad-escape" to "BAD_PAYLOAD", "P03-feff-as-space" to "BAD_PAYLOAD",
        // P04 niuat, array, true/null, decimal, exponent, negatiu, 10 dígits
        "P04-nested" to "BAD_PAYLOAD", "P04-empty-object" to "BAD_PAYLOAD", "P04-array" to "BAD_PAYLOAD",
        "P04-empty-array" to "BAD_PAYLOAD", "P04-true" to "BAD_PAYLOAD", "P04-false" to "BAD_PAYLOAD",
        "P04-null" to "BAD_PAYLOAD", "P04-decimal" to "BAD_PAYLOAD", "P04-exponent" to "BAD_PAYLOAD",
        "P04-negative" to "BAD_PAYLOAD", "P04-ten-digits" to "BAD_PAYLOAD", "P04-leading-zero" to "BAD_PAYLOAD",
        "P04-v-decimal" to "BAD_PAYLOAD", "P04-v-exponent" to "BAD_PAYLOAD",
        "P04-ok-decimal" to "BAD_PAYLOAD", "P04-dur-negative" to "BAD_PAYLOAD",
        // P05 nom duplicat, també per escapament
        "P05-nom" to "BAD_PAYLOAD", "P05-nom-escaped" to "BAD_PAYLOAD", "P05-kid-escaped" to "BAD_PAYLOAD",
        "P05-v" to "BAD_PAYLOAD", "P05-unknown" to "BAD_PAYLOAD",
        // P06 \ud800 aïllat
        "P06-lone-high" to "BAD_PAYLOAD", "P06-lone-low" to "BAD_PAYLOAD", "P06-high-then-bmp" to "BAD_PAYLOAD",
        "P06-reversed-pair" to "BAD_PAYLOAD", "P06-high-at-end" to "BAD_PAYLOAD", "P06-member-name" to "BAD_PAYLOAD",
        // P07 falta v o kid, v com a text, v=2 a ARB1, kid amb majúscules o '-'
        "P07-missing-v" to "BAD_PAYLOAD", "P07-missing-kid" to "BAD_PAYLOAD", "P07-v-text" to "BAD_PAYLOAD",
        "P07-v-2" to "BAD_PAYLOAD", "P07-v-0" to "BAD_PAYLOAD", "P07-kid-uppercase" to "BAD_PAYLOAD",
        "P07-kid-dash" to "BAD_PAYLOAD", "P07-kid-empty" to "BAD_PAYLOAD", "P07-kid-17-chars" to "BAD_PAYLOAD",
        "P07-kid-integer" to "BAD_PAYLOAD",
        // P08 17 membres; P09 byte alterat que trenca el JSON
        "P08" to "BAD_PAYLOAD", "P09-last-byte" to "BAD_PAYLOAD", "P09-first-byte" to "BAD_PAYLOAD",
        // K01 kid inexistent; K03 kid=testx
        "K01-unknown" to "UNKNOWN_KID", "K01-production-kid" to "UNKNOWN_KID", "K03" to "KID_NOT_ACCEPTED",
        // S01–S06
        "S01" to "BAD_SIGNATURE", "S02-r-bit" to "BAD_SIGNATURE", "S02-s-bit" to "BAD_SIGNATURE", "S03" to "BAD_SIGNATURE",
        "S04-r-zero" to "BAD_SIGNATURE", "S04-s-zero" to "BAD_SIGNATURE", "S04-r-n" to "BAD_SIGNATURE",
        "S04-r-max" to "BAD_SIGNATURE", "S04-s-n" to "BAD_SIGNATURE", "S04-r-plus-n" to "BAD_SIGNATURE",
        "S05" to "BAD_SIGNATURE", "S06" to "BAD_SIGNATURE",
        // C01 falta cada obligatori
        "C01-missing-cid" to "BAD_FIELDS", "C01-missing-nom" to "BAD_FIELDS", "C01-missing-res" to "BAD_FIELDS",
        "C01-missing-exv" to "BAD_FIELDS", "C01-missing-iat" to "BAD_FIELDS", "C01-missing-orig" to "BAD_FIELDS",
        // C02 res "apte" o "APTA"; C03 orig diferent
        "C02-lowercase" to "BAD_FIELDS", "C02-apta" to "BAD_FIELDS", "C02-integer" to "BAD_FIELDS",
        "C03-capitalized" to "BAD_FIELDS", "C03-other" to "BAD_FIELDS",
        // C04 cid en majúscules o no v4
        "C04-uppercase" to "BAD_FIELDS", "C04-version-1" to "BAD_FIELDS", "C04-variant-c" to "BAD_FIELDS",
        "C04-no-dashes" to "BAD_FIELDS",
        // C05 iat 2026-02-30, amb hora o enter
        "C05-feb-30" to "BAD_FIELDS", "C05-with-time" to "BAD_FIELDS", "C05-integer" to "BAD_FIELDS",
        "C05-month-13" to "BAD_FIELDS", "C05-month-00" to "BAD_FIELDS", "C05-feb-29-2025" to "BAD_FIELDS",
        "C05-feb-29-1900" to "BAD_FIELDS", "C05-no-padding" to "BAD_FIELDS",
        // C06 nom buit, espai inicial/final, doble espai, U+0007, U+202E, 81 punts de codi
        "C06-empty" to "BAD_FIELDS", "C06-leading-space" to "BAD_FIELDS", "C06-trailing-space" to "BAD_FIELDS",
        "C06-double-space" to "BAD_FIELDS", "C06-u0007" to "BAD_FIELDS", "C06-u202e" to "BAD_FIELDS",
        "C06-u00a0" to "BAD_FIELDS", "C06-u200b" to "BAD_FIELDS", "C06-ufeff" to "BAD_FIELDS",
        "C06-81-code-points" to "BAD_FIELDS", "C06-integer" to "BAD_FIELDS",
        // C07 exv, bv o app fora de patró
        "C07-exv-empty" to "BAD_FIELDS", "C07-exv-space" to "BAD_FIELDS", "C07-exv-17-chars" to "BAD_FIELDS",
        "C07-exv-plus" to "BAD_FIELDS", "C07-exv-integer" to "BAD_FIELDS", "C07-bv-slash" to "BAD_FIELDS",
        "C07-bv-25-chars" to "BAD_FIELDS", "C07-bv-integer" to "BAD_FIELDS", "C07-app-space" to "BAD_FIELDS",
        "C07-app-33-chars" to "BAD_FIELDS", "C07-app-non-ascii" to "BAD_FIELDS",
        // C08 exp, nbf, crit o alg
        "C08-exp" to "BAD_FIELDS", "C08-nbf" to "BAD_FIELDS", "C08-crit" to "BAD_FIELDS", "C08-alg" to "BAD_FIELDS",
        // C09 ok/tot/dur: ok sense tot (o al revés), ok > tot, fora de rang, com a text
        "C09-ok-without-tot" to "BAD_FIELDS", "C09-tot-without-ok" to "BAD_FIELDS", "C09-ok-gt-tot" to "BAD_FIELDS",
        "C09-tot-zero" to "BAD_FIELDS", "C09-tot-100" to "BAD_FIELDS", "C09-ok-100" to "BAD_FIELDS",
        "C09-dur-86401" to "BAD_FIELDS", "C09-ok-text" to "BAD_FIELDS", "C09-tot-text" to "BAD_FIELDS",
        "C09-dur-text" to "BAD_FIELDS",
        // Extres del mateix contracte (límits i precedències)
        "X01" to "OK", "X02" to "OK", "X03" to "OK", "X04" to "BAD_FORMAT", "X05" to "UNKNOWN_VERSION",
        "X06" to "IS_URL", "X07" to "OK", "X08" to "OK", "X09" to "KID_NOT_ACCEPTED", "X10" to "OK",
        "X11" to "BAD_FORMAT", "X12" to "OK", "X13" to "OK", "X14" to "OK",
    )

    @Test
    fun everyRequiredVariantOfSection3711IsPresentWithItsCode() {
        val actual = cases.associate { (it["id"] as String) to (it["code"] as String) }
        assertEquals(cases.size, actual.size, "ids duplicats")
        assertEquals(emptyList(), requiredIds.keys.filter { it !in actual }, "variants absents")
        assertEquals(emptyList(), actual.keys.filter { it !in requiredIds }, "casos no declarats a la prova")
        for ((id, code) in requiredIds) assertEquals(code, actual[id], id)
        for (c in cases) assertEquals((c["id"] as String).substringBefore('-'), c["group"], "group de ${c["id"]}")
        val groups = actual.keys.map { it.substringBefore('-') }.toSet()
        assertTrue("R01" !in groups && "K02" !in groups, "R01/K02 són exclusius de release (G3)")
    }

    @Test
    fun groupCodesMatchGateG1() {
        val expected = mapOf(
            'V' to "OK", 'U' to "UNKNOWN_VERSION", 'P' to "BAD_PAYLOAD", 'S' to "BAD_SIGNATURE", 'C' to "BAD_FIELDS",
        )
        for (c in cases) {
            val g = c["group"] as String
            val code = c["code"] as String
            when {
                g == "F02" -> assertEquals("IS_URL", code, g)
                g.startsWith("F") -> assertEquals("BAD_FORMAT", code, g)
                g == "K01" -> assertEquals("UNKNOWN_KID", code, g)
                g == "K03" -> assertEquals("KID_NOT_ACCEPTED", code, g)
                g[0] in expected -> assertEquals(expected[g[0]], code, g)
            }
        }
    }

    @Test
    fun allVectorsGiveExactCode() {
        val verifier = CredentialVerifier(trusted)
        val failures = ArrayList<String>()
        for (c in cases) {
            val id = c["id"] as String
            val expected = VerificationCode.valueOf(c["code"] as String)
            assertEquals(expected.outcome.label, c["result"], "$id: result")
            val r = verifier.verify(c["input"] as String)
            if (r.code != expected) {
                failures += "$id: esperat $expected, obtingut ${r.code}"
                continue
            }
            if (expected == VerificationCode.OK) {
                val f = c["fields"] as Map<*, *>
                val cr = assertNotNull(r.credential)
                val got = linkedMapOf<String, Any?>(
                    "v" to MiniJson.Number(cr.v.toString()), "kid" to cr.kid, "cid" to cr.cid, "nom" to cr.nom,
                    "res" to cr.res, "exv" to cr.exv, "iat" to cr.iat, "orig" to cr.orig,
                )
                cr.ok?.let { got["ok"] = MiniJson.Number(it.toString()) }
                cr.tot?.let { got["tot"] = MiniJson.Number(it.toString()) }
                cr.dur?.let { got["dur"] = MiniJson.Number(it.toString()) }
                cr.bv?.let { got["bv"] = it }
                cr.app?.let { got["app"] = it }
                if (got != f) failures += "$id: camps $got != $f"
                if (cr.shortCode != c["shortCode"]) failures += "$id: shortCode"
            } else {
                assertEquals(null, r.credential, id)
            }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
        println("Vectors ARB1 (JVM): ${cases.size} casos OK")
    }
}
