package dev.lluissunol.arbitratge.exam

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Banco de preguntas, esquema 1 (03 §5.2). El campo privado `canary` no se modela: se ignora al leer. */
@Serializable
data class Bank(
    val schema: Int,
    @SerialName("bank_version") val bankVersion: String,
    val locale: String = "ca",
    val sample: Boolean = false,
    val rules: ExamRules,
    val questions: List<Question>,
)

@Serializable
data class ExamRules(
    @SerialName("exam_version") val examVersion: String,
    @SerialName("num_questions") val numQuestions: Int,
    @SerialName("time_limit_minutes") val timeLimitMinutes: Int,
    @SerialName("pass_threshold_count") val passThresholdCount: Int,
    @SerialName("shuffle_questions") val shuffleQuestions: Boolean = true,
    @SerialName("shuffle_options") val shuffleOptions: Boolean = true,
    val sequential: Boolean = true,
) {
    val limitMs: Long get() = timeLimitMinutes * 60_000L
}

@Serializable
enum class QuestionType {
    @SerialName("single_choice") SINGLE_CHOICE,
    @SerialName("multiple_choice") MULTIPLE_CHOICE,
    @SerialName("true_false") TRUE_FALSE,
}

@Serializable
enum class QuestionStatus {
    @SerialName("active") ACTIVE,
    @SerialName("retired") RETIRED,
}

@Serializable
data class Option(val id: String, val text: String)

@Serializable
data class Question(
    val id: String,
    val type: QuestionType,
    val status: QuestionStatus,
    val text: String,
    val options: List<Option>,
    val correct: List<String> = emptyList(),
    val explanation: String? = null,
    val ref: String? = null,
    val tags: List<String> = emptyList(),
)

/** Error de lectura. El mensaje NUNCA contiene fragmentos del JSON (podrían ser texto del banco). */
class BankParseException(message: String) : Exception(message)

object BankParser {
    const val SUPPORTED_SCHEMA = 1

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): Bank {
        val schema = try {
            val root = json.parseToJsonElement(text).jsonObject
            (root["schema"] as? JsonPrimitive)?.int
        } catch (e: Exception) {
            throw BankParseException("Banc: JSON malformat")
        }
        if (schema != SUPPORTED_SCHEMA) throw BankParseException("Banc: schema no suportat")
        return try {
            json.decodeFromString(Bank.serializer(), text)
        } catch (e: SerializationException) {
            throw BankParseException("Banc: l'estructura no compleix el schema 1")
        } catch (e: IllegalArgumentException) {
            throw BankParseException("Banc: l'estructura no compleix el schema 1")
        }
    }
}
