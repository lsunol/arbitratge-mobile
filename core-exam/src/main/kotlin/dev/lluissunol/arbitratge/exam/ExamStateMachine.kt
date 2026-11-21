package dev.lluissunol.arbitratge.exam

/** Estado persistido de la sesión (03 §4.4). */
enum class SessionState { IN_PROGRESS, FINISHED, ABANDONED }

enum class FinishReason { COMPLETED, TIMED_OUT, ABANDONED }

/** Fases de la máquina de estados (03 §4.3). */
sealed interface ExamPhase {
    data object Home : ExamPhase
    data object Briefing : ExamPhase
    data object Identifying : ExamPhase
    data object Ready : ExamPhase
    data class InProgress(val index: Int) : ExamPhase
    data class Finished(val reason: FinishReason) : ExamPhase
    data class Result(val passed: Boolean) : ExamPhase
    data object Credential : ExamPhase
    data object Abandoned : ExamPhase
}

/**
 * Eventos = HECHOS ya persistidos (invariante B3): quien los emite lo hace tras el commit.
 * Un fallo de guardado se modela con [SaveFailed]: se permanece en la fase actual.
 */
sealed interface ExamEvent {
    data object OpenBriefing : ExamEvent
    data object BriefingAccepted : ExamEvent
    data object NameEntered : ExamEvent
    data object ChangeName : ExamEvent
    /** Sesión creada y persistida ("Començar ara"). */
    data object SessionStarted : ExamEvent
    /** Navegación libre: la selección de la pregunta actual ya está persistida y se pasa a la pregunta `to` (0 until total). */
    data class Navigated(val to: Int, val total: Int) : ExamEvent
    /** "Finalitzar" confirmado: FINISHED(COMPLETED) + correct_count ya persistidos. Nunca se emite al responder la última. */
    data object FinishConfirmed : ExamEvent
    /** FINISHED(TimedOut) + correct_count persistidos. */
    data object TimedOut : ExamEvent
    /** ABANDONED persistido tras confirmación. */
    data object AbandonCommitted : ExamEvent
    /** Resultado (ya persistido) que se pinta. */
    data class ResultShown(val passed: Boolean) : ExamEvent
    data object ReturnHome : ExamEvent
    /** Credencial insertada en la misma transacción. */
    data object CredentialStored : ExamEvent
    data object SaveFailed : ExamEvent
}

object ExamStateMachine {
    /** Siguiente fase, o `null` si la transición no existe (el llamante trata el caso como error). */
    fun next(phase: ExamPhase, event: ExamEvent): ExamPhase? {
        if (event is ExamEvent.SaveFailed) return phase
        return when (phase) {
            ExamPhase.Home -> if (event is ExamEvent.OpenBriefing) ExamPhase.Briefing else null
            ExamPhase.Briefing -> if (event is ExamEvent.BriefingAccepted) ExamPhase.Identifying else null
            ExamPhase.Identifying -> if (event is ExamEvent.NameEntered) ExamPhase.Ready else null
            ExamPhase.Ready -> when (event) {
                is ExamEvent.ChangeName -> ExamPhase.Identifying
                is ExamEvent.SessionStarted -> ExamPhase.InProgress(0)
                else -> null
            }
            is ExamPhase.InProgress -> when (event) {
                is ExamEvent.Navigated -> if (event.to in 0 until event.total) ExamPhase.InProgress(event.to) else null
                is ExamEvent.FinishConfirmed -> ExamPhase.Finished(FinishReason.COMPLETED)
                is ExamEvent.TimedOut -> ExamPhase.Finished(FinishReason.TIMED_OUT)
                is ExamEvent.AbandonCommitted -> ExamPhase.Abandoned
                else -> null
            }
            is ExamPhase.Finished -> if (event is ExamEvent.ResultShown) ExamPhase.Result(event.passed) else null
            is ExamPhase.Result -> when {
                !phase.passed && event is ExamEvent.ReturnHome -> ExamPhase.Home
                phase.passed && event is ExamEvent.CredentialStored -> ExamPhase.Credential
                else -> null
            }
            ExamPhase.Credential -> if (event is ExamEvent.ReturnHome) ExamPhase.Home else null
            ExamPhase.Abandoned -> if (event is ExamEvent.ReturnHome) ExamPhase.Home else null
        }
    }

    /** Solo se emite credencial con FINISHED aprobado persistido y sin credencial previa (03 §4.3). */
    fun shouldIssueCredential(
        state: SessionState,
        finishReason: FinishReason?,
        correctCount: Int?,
        threshold: Int,
        credentialExists: Boolean,
        isPractice: Boolean = false,
    ): Boolean =
        !isPractice && !credentialExists && state == SessionState.FINISHED &&
            (finishReason == FinishReason.COMPLETED || finishReason == FinishReason.TIMED_OUT) && correctCount != null && correctCount >= threshold

    fun isLastQuestion(index: Int, total: Int): Boolean = index == total - 1
}
