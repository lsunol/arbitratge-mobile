package dev.lluissunol.arbitratge.ui

import dev.lluissunol.arbitratge.data.StoredCredential
import dev.lluissunol.arbitratge.exam.ExamResult
import dev.lluissunol.arbitratge.exam.SessionQuestion

data class RulesSummary(val questions: Int, val minutes: Int, val threshold: Int)

data class LastResult(val sessionId: String, val correct: Int, val total: Int, val passed: Boolean)

enum class NameProblem { EMPTY, TOO_LONG, INVALID_CHARS }

enum class TimeBanner { FIVE_MIN, ONE_MIN }

enum class QuestionDialog { EXIT, NO_ANSWER, FINISH }

/** Qué no se ha podido desar (03 §4.3 "Fallos de persistencia"). */
enum class SaveError { ANSWER, RESULT }

/** Estado de la credencial dentro de C8 (04 §2.7, §2.10). */
sealed interface CredentialPhase {
    data object Issuing : CredentialPhase
    data class Ready(val credential: StoredCredential) : CredentialPhase

    /** "Encara no s'ha pogut crear la credencial" + reintento. */
    data object Pending : CredentialPhase
}

/**
 * Pantalla raíz derivada del estado persistido (03 §4.1): no hay pila de navegación.
 * Los datos transitorios (nombre en edición, respuesta marcada y no guardada) viven solo aquí.
 */
sealed interface Stage {
    data object Loading : Stage
    data object Welcome : Stage

    /** C0. */
    data class Home(val rules: RulesSummary, val credential: StoredCredential?, val last: LastResult?) : Stage

    /** C2. */
    data class Name(val draft: String, val problem: NameProblem?) : Stage

    /** C3. [nom] ya normalizado (NameRules) y mostrado para confirmar. */
    data class Rules(val nom: String, val summary: RulesSummary, val starting: Boolean, val saveError: Boolean) : Stage

    /** C4 con los diálogos C5. */
    data class Question(
        val index: Int,
        val total: Int,
        val question: SessionQuestion,
        val selected: Set<String>,
        val remainingMs: Long,
        val timeHidden: Boolean,
        val banner: TimeBanner?,
        val dialog: QuestionDialog?,
        val saving: Boolean,
        val saveError: SaveError?,
        /** Umbral de tiempo cruzado (independiente de si el aviso se ha descartado); null si no aplica. */
        val level: TimeBanner? = null,
    ) : Stage {
        val isLast: Boolean get() = index == total - 1
    }

    /** C6. */
    data class TimeUp(val answered: Int, val total: Int, val away: Boolean) : Stage

    /** C7. [error] = no se ha podido desar/leer el resultado. */
    data class Grading(val error: Boolean) : Stage

    /** C8/C9. */
    data class Result(val sessionId: String, val result: ExamResult, val credential: CredentialPhase?) : Stage

    /** C10 (lista y detalle). [openIndex] = índice en la lista filtrada. */
    data class Review(
        val sessionId: String,
        val result: ExamResult,
        val onlyWrong: Boolean,
        val openIndex: Int?,
        val fromResult: Boolean,
    ) : Stage

    /** C11 y modo presentación. */
    data class Credential(val credential: StoredCredential, val presentation: Boolean, val fromResult: Boolean = false) : Stage

    /** 04 §2.10: banco/ajustes inválidos (build roto). */
    data object BuildError : Stage

    /** El reanclaje del reloj tras un reinicio no se pudo persistir: se reintenta antes de reanudar. */
    data object RecoveryError : Stage

    /** 04 §2.10: estado ilegible. */
    data object StateError : Stage
}
