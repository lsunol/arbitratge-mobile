package dev.lluissunol.arbitratge.credential

import java.io.File

/** Material de prova fix de `test-vectors/` (claus públiques per disseny, kid=test/testx). */
internal object TestMaterial {
    val dir = File(System.getProperty("arb1.testVectorsDir") ?: "../test-vectors")

    private val keys: List<Map<*, *>> by lazy {
        ((MiniJson.parse(File(dir, "test-keys.json").readText()) as Map<*, *>)["keys"] as List<*>).map { it as Map<*, *> }
    }

    fun pkcs8(name: String): ByteArray = Base64Url.decodeCanonical(keys.single { it["name"] == name }["pkcs8"] as String)!!

    fun spki(name: String): ByteArray = Base64Url.decodeCanonical(keys.single { it["name"] == name }["spki"] as String)!!

    val trusted: TrustedKeys by lazy { TrustedKeys.parseList(File(dir, "trusted-keys-test.json").readText()) }
}
