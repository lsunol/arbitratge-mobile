package dev.lluissunol.arbitratge.exam

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import kotlin.system.exitProcess

/**
 * Herramientas de build (03 §5.4), invocadas desde Gradle (`packBank`, `packSealKey`).
 * NUNCA imprimen texto del banco ni material de clave: solo recuentos, IDs técnicos y el `kid`.
 */
object BuildTools {
    /** Kids de prueba que un build de release no puede embeber (05 §4.3.1). */
    val NON_PRODUCTION_KIDS = setOf("dev", "test", "testx")
    private val KID = Regex("^[a-z0-9]{1,16}$")
    private val json = Json { prettyPrint = false }

    class ToolException(message: String) : Exception(message)

    /**
     * Valida el banco (con el validador de runtime/release) y devuelve el JSON a empaquetar:
     * el mismo documento sin el campo privado `canary`. [release] ⇒ rechaza `sample=true`.
     */
    fun packBank(text: String, release: Boolean, log: (String) -> Unit = {}): String {
        val bank = try {
            BankParser.parse(text)
        } catch (e: BankParseException) {
            throw ToolException(e.message ?: "Banc il·legible")
        }
        val report = BankValidator.validate(bank, if (release) ValidationMode.RELEASE else ValidationMode.RUNTIME)
        if (!report.isValid) throw ToolException(report.summary())
        val active = bank.questions.count { it.status == QuestionStatus.ACTIVE }
        log(
            "Banc OK: bank_version=${bank.bankVersion} exam_version=${bank.rules.examVersion} sample=${bank.sample} " +
                "preguntes=${bank.questions.size} actives=${active} N=${bank.rules.numQuestions} " +
                "minuts=${bank.rules.timeLimitMinutes} llindar=${bank.rules.passThresholdCount}",
        )
        val root: JsonObject = Json.parseToJsonElement(text).jsonObject
        return json.encodeToString(JsonObject.serializer(), JsonObject(root.filterKeys { it != "canary" }))
    }

    /** Valida kid y clave PKCS#8 P-256; devuelve el kid. No lee ni imprime la clave. */
    fun checkSealKey(pkcs8: ByteArray, kid: String, release: Boolean): String {
        if (!KID.matches(kid)) throw ToolException("ARB_SEAL_KID fora de patró [a-z0-9]{1,16}")
        if (release && kid in NON_PRODUCTION_KIDS) throw ToolException("kid '$kid' no es pot usar en release")
        val key = try {
            KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(pkcs8)) as? ECPrivateKey
        } catch (e: Exception) {
            null
        }
        if (key == null || key.params.order.bitLength() != 256 || key.params.curve.field.fieldSize != 256) {
            throw ToolException("La clau de sello no és una PKCS#8 P-256 vàlida")
        }
        return kid
    }

    /**
     * `pack-bank <in> <out> <debug|release>` · `pack-seal <in.der> <kid> <outDir> <debug|release>`.
     * Código de salida 1 con mensaje claro; los mensajes no contienen contenido del banco.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            when (args.firstOrNull()) {
                "pack-bank" -> {
                    val out = packBank(File(args[1]).readText(Charsets.UTF_8), args[3] == "release") { println(it) }
                    File(args[2]).apply { parentFile.mkdirs() }.writeText(out, Charsets.UTF_8)
                }
                "pack-seal" -> {
                    val bytes = File(args[1]).readBytes()
                    val kid = checkSealKey(bytes, args[2], args[4] == "release")
                    val dir = File(args[3]).apply { mkdirs() }
                    File(dir, "seal-key.der").writeBytes(bytes)
                    File(dir, "kid.txt").writeText(kid, Charsets.US_ASCII)
                    println("Clau de sello OK: kid=$kid")
                }
                else -> throw ToolException("ús: pack-bank|pack-seal …")
            }
        } catch (e: ToolException) {
            System.err.println("ERROR: ${e.message}")
            exitProcess(1)
        }
    }
}
