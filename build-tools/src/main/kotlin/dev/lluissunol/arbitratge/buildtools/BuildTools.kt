package dev.lluissunol.arbitratge.buildtools

import dev.lluissunol.arbitratge.credential.P256
import dev.lluissunol.arbitratge.exam.BankParseException
import dev.lluissunol.arbitratge.exam.BankParser
import dev.lluissunol.arbitratge.exam.BankValidator
import dev.lluissunol.arbitratge.exam.QuestionStatus
import dev.lluissunol.arbitratge.exam.ValidationMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Base64
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

    /**
     * Valida kid y clave PKCS#8 con la validación P-256 canónica de `core-credential` (`P256.spkiFromPkcs8`:
     * curva P-256 exacta, 0 < d < n). En release rechaza además el material de debug/prueba: se compara el
     * SPKI derivado con [denySpkis], sea cual sea el `kid`. No imprime la clave. Devuelve el kid.
     */
    fun checkSealKey(pkcs8: ByteArray, kid: String, release: Boolean, denySpkis: List<ByteArray> = emptyList()): String {
        if (!KID.matches(kid)) throw ToolException("ARB_SEAL_KID fora de patró [a-z0-9]{1,16}")
        if (release && kid in NON_PRODUCTION_KIDS) throw ToolException("kid '$kid' no es pot usar en release")
        val spki = P256.spkiFromPkcs8(pkcs8) ?: throw ToolException("La clau de sello no és una PKCS#8 P-256 vàlida")
        if (release && denySpkis.any { it.contentEquals(spki) }) {
            throw ToolException("La clau de sello és material de debug/prova (dev/test): no es pot usar en release")
        }
        return kid
    }

    /** SPKI de las claves prohibidas en release: `.der` (PKCS#8) o `test-keys.json` (`keys[].spki|pkcs8`). */
    fun denySpkis(files: List<File>): List<ByteArray> {
        val dec = Base64.getUrlDecoder()
        val out = mutableListOf<ByteArray>()
        for (f in files) {
            if (f.name.endsWith(".json")) {
                val keys = Json.parseToJsonElement(f.readText(Charsets.UTF_8)).jsonObject["keys"]?.jsonArray ?: JsonArray(emptyList())
                for (k in keys) {
                    val o = k.jsonObject
                    o["spki"]?.jsonPrimitive?.content?.let { out += dec.decode(it) }
                    o["pkcs8"]?.jsonPrimitive?.content?.let { P256.spkiFromPkcs8(dec.decode(it))?.let(out::add) }
                }
            } else {
                P256.spkiFromPkcs8(f.readBytes())?.let(out::add)
            }
        }
        return out
    }

    /**
     * `pack-bank <in> <out> <debug|release>` · `pack-seal <in.der> <kid> <outDir> <debug|release> [denyFile…]`.
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
                    val kid = checkSealKey(bytes, args[2], args[4] == "release", denySpkis(args.drop(5).map(::File)))
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
