package dev.lluissunol.arbitratge.exam

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Pregunta congelada en la instantánea, con las opciones en el orden mostrado (03 §4.4). */
@Serializable
data class SessionQuestion(
    val id: String,
    val type: QuestionType,
    val text: String,
    val options: List<Option>,
    val correctIds: List<String>,
    val explanation: String? = null,
    val ref: String? = null,
)

/** Instantánea inmutable del examen: no depende del banco del APK. */
@Serializable
data class ExamSnapshot(
    val bankVersion: String,
    val examVersion: String,
    val rules: ExamRules,
    val questions: List<SessionQuestion>,
)

@Serializable
data class AnswerRecord(val index: Int, val selectedIds: List<String>, val remainingAtTapMs: Long)

/**
 * Sesión = instantánea + respuestas. Todo lo demás (resultado) se recalcula.
 *
 * Navegación libre (decisión del usuario, 2026-11-20): una respuesta puede sustituirse o vaciarse hasta finalizar.
 * Una respuesta con `selectedIds` vacío equivale a "sense resposta".
 */
@Serializable
data class ExamSession(val snapshot: ExamSnapshot, val answers: List<AnswerRecord> = emptyList()) {
    val total: Int get() = snapshot.questions.size

    /** Índices con al menos una opción marcada. */
    val answeredIndices: Set<Int> get() = answers.filter { it.selectedIds.isNotEmpty() }.map { it.index }.toSet()
    val answeredCount: Int get() = answeredIndices.size
    val unansweredCount: Int get() = total - answeredCount

    /** Primera pregunta sin respuesta (o `null` si están todas respondidas). */
    val firstUnanswered: Int? get() = (0 until total).firstOrNull { it !in answeredIndices }
    fun answerFor(index: Int): AnswerRecord? = answers.firstOrNull { it.index == index }
}

@Serializable
data class QuestionResult(
    val questionId: String,
    val text: String,
    val options: List<Option>,
    val selectedIds: List<String>,
    val correctIds: List<String>,
    val isCorrect: Boolean,
    val explanation: String? = null,
    val ref: String? = null,
    val type: QuestionType = QuestionType.SINGLE_CHOICE,
)

@Serializable
data class ExamResult(
    val correct: Int,
    /** Respondidas y erróneas. */
    val wrong: Int,
    val unanswered: Int,
    val total: Int,
    val threshold: Int,
    val passed: Boolean,
    val elapsedMs: Long,
    val items: List<QuestionResult>,
) {
    /** "Fallos" del diseño (03 §4.2): total − aciertos = wrong + unanswered. */
    val failures: Int get() = total - correct
}

enum class EngineError { INVALID_RULES, INDEX_OUT_OF_RANGE, TIME_EXPIRED, INVALID_SELECTION }

/** Mensajes solo con códigos/recuentos/IDs; nunca texto de preguntas. */
class ExamEngineException(val error: EngineError, message: String) : IllegalStateException(message)

val ExamJson: Json = Json { encodeDefaults = true }

object ExamEngine {

    /**
     * Valida banco (estructura) y reglas; selecciona N activas, baraja y congela la instantánea.
     * Si algo no es válido lanza [ExamEngineException] (INVALID_RULES): no hay examen.
     */
    fun start(bank: Bank, rules: ExamRules, rng: Rng): ExamSession {
        val report = BankValidator.validate(bank.copy(rules = rules), ValidationMode.RUNTIME)
        if (!report.isValid) throw ExamEngineException(EngineError.INVALID_RULES, report.summary())

        val active = bank.questions.filter { it.status == QuestionStatus.ACTIVE }
        val pool = if (rules.shuffleQuestions) fisherYates(active, rng) else active
        val picked = pool.take(rules.numQuestions)
        val frozen = picked.map { q ->
            // true_false conserva el orden fijo Vertader/Fals.
            val opts = if (rules.shuffleOptions && q.type != QuestionType.TRUE_FALSE) fisherYates(q.options, rng) else q.options.toList()
            SessionQuestion(
                id = q.id,
                type = q.type,
                text = q.text,
                options = opts,
                correctIds = opts.map { it.id }.filter { it in q.correct },
                explanation = q.explanation,
                ref = q.ref,
            )
        }
        return ExamSession(ExamSnapshot(bank.bankVersion, rules.examVersion, rules, frozen))
    }

    /**
     * Registra (o SUSTITUYE) la respuesta `i`: navegación libre. `remainingAtTapMs` es el restante capturado AL TOCAR
     * (>= 0 o se rechaza). Una selección vacía deja la pregunta sin responder. Idempotente: repetir exactamente la
     * misma respuesta devuelve la misma sesión.
     */
    fun answer(session: ExamSession, index: Int, selectedIds: Collection<String>, remainingAtTapMs: Long): ExamSession {
        val q = session.snapshot.questions.getOrNull(index)
            ?: fail(EngineError.INDEX_OUT_OF_RANGE, "index=$index total=${session.total}")
        if (remainingAtTapMs < 0) fail(EngineError.TIME_EXPIRED, "index=$index")

        val validIds = q.options.map { it.id }
        val unique = selectedIds.toSet()
        if (unique.size != selectedIds.size || !validIds.containsAll(unique) ||
            (q.type != QuestionType.MULTIPLE_CHOICE && unique.size > 1)
        ) fail(EngineError.INVALID_SELECTION, "index=$index n=${selectedIds.size}")
        val ordered = validIds.filter { it in unique } // orden mostrado, determinista
        val record = AnswerRecord(index, ordered, remainingAtTapMs)

        val existing = session.answerFor(index)
        if (existing == record) return session
        val answers = if (existing == null) session.answers + record else session.answers.map { if (it.index == index) record else it }
        return session.copy(answers = answers.sortedBy { it.index })
    }

    /**
     * Corrección por conjunto exacto; sin responder = fallo; sin crédito parcial;
     * aprobado = aciertos >= umbral. Determinista y recalculable desde snapshot + answers.
     *
     * [elapsedMs]: obligatorio; sale del reloj persistido y reconciliado
     * (`ExamClockModel.elapsedMs` = límite − restante). No se deriva de las respuestas.
     */
    fun grade(session: ExamSession, elapsedMs: Long): ExamResult {
        var correct = 0
        var unanswered = 0
        val items = session.snapshot.questions.mapIndexed { i, q ->
            val selected = session.answerFor(i)?.selectedIds.orEmpty()
            val ok = selected.isNotEmpty() && selected.toSet() == q.correctIds.toSet()
            if (selected.isEmpty()) unanswered++ else if (ok) correct++
            QuestionResult(q.id, q.text, q.options, selected, q.correctIds, ok, q.explanation, q.ref, q.type)
        }
        val total = session.total
        val threshold = session.snapshot.rules.passThresholdCount
        return ExamResult(
            correct = correct,
            wrong = total - correct - unanswered,
            unanswered = unanswered,
            total = total,
            threshold = threshold,
            passed = correct >= threshold,
            elapsedMs = elapsedMs,
            items = items,
        )
    }

    /** Fisher-Yates con el `Rng` inyectado; no modifica la lista de entrada. */
    fun <T> fisherYates(input: List<T>, rng: Rng): List<T> {
        val a = input.toMutableList()
        for (i in a.size - 1 downTo 1) {
            val j = rng.nextInt(i + 1)
            val t = a[i]; a[i] = a[j]; a[j] = t
        }
        return a
    }

    private fun fail(error: EngineError, detail: String): Nothing = throw ExamEngineException(error, "$error: $detail")
}
