package dev.lluissunol.arbitratge.data

import dev.lluissunol.arbitratge.exam.AnswerRecord
import dev.lluissunol.arbitratge.exam.ClockState
import dev.lluissunol.arbitratge.exam.ExamSession
import dev.lluissunol.arbitratge.exam.FinishReason
import dev.lluissunol.arbitratge.exam.SessionState

enum class SessionKind { EXAM, PRACTICE }

/** Sesión persistida (03 §4.4): instantánea + respuestas + reloj. Todo lo demás se recalcula. */
data class StoredSession(
    val id: String,
    val kind: SessionKind,
    val state: SessionState,
    /** UUID v4 reservado al empezar (emisión idempotente, 03 §4.3). */
    val cid: String,
    /** Congelado e inmutable desde `Ready → InProgress`. */
    val candidateName: String,
    val session: ExamSession,
    val currentIndex: Int,
    val clock: ClockState,
    val startWall: Long,
    val finishedWall: Long? = null,
    val finishReason: FinishReason? = null,
    val correctCount: Int? = null,
) {
    val bankVersion: String get() = session.snapshot.bankVersion
    val examVersion: String get() = session.snapshot.examVersion
}

/** Cierre de sesión: se escribe en la MISMA transacción que la última respuesta o el fin por tiempo. */
data class Finish(val reason: FinishReason, val correctCount: Int, val finishedWall: Long)

data class StoredCredential(
    val cid: String,
    val sessionId: String,
    /** Texto exacto del token: nunca se re-serializa (02 §3.7.10 paso 6). */
    val token: String,
    val name: String,
    val iat: String,
    val kid: String,
    val exv: String,
    val bv: String?,
    val pdfExported: Boolean = false,
)

/** Error de almacenamiento (disco lleno, BD corrupta…). Es el único error que la UI trata como "reintentable". */
class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** La sesión ya no está `IN_PROGRESS` (p. ej. terminó por tiempo antes de que llegara el guardado). */
class SessionNotActiveException(message: String) : Exception(message)

/**
 * Puerto de persistencia (03 §4.1). Cada método de escritura es UNA transacción atómica.
 * Los tests inyectan implementaciones que fallan (03 §11).
 */
interface SessionStore {
    suspend fun active(): StoredSession?
    /** Última sesión terminada por ORDEN DE INSERCIÓN (no por hora de pared, que puede cambiarse). */
    suspend fun latestFinished(): StoredSession?

    /** Sesiones `FINISHED` de examen SIN fila `credential`, en orden de inserción (recuperación de emisión). */
    suspend fun finishedWithoutCredential(): List<StoredSession>
    suspend fun session(id: String): StoredSession?

    /** Crea la sesión `IN_PROGRESS`. Falla si ya hay otra activa (unicidad en BD). */
    suspend fun create(session: StoredSession)

    /**
     * Una transacción: respuesta (idempotente por `(session, i)`) + `current_index` + punto de control del
     * reloj [clock] y, si [finish] != null, `FINISHED` + motivo + `correct_count`.
     */
    suspend fun commitAnswer(sessionId: String, answer: AnswerRecord, clock: ClockState, finish: Finish?)

    /** Punto de control o reanclaje del reloj (solo si `IN_PROGRESS`). Idempotente. */
    suspend fun saveClock(sessionId: String, clock: ClockState)

    /** Fin por tiempo (o cierre) sin respuesta nueva. Idempotente si ya está `FINISHED`. */
    suspend fun finish(sessionId: String, clock: ClockState, finish: Finish)

    /** `ABANDONED` (terminal). Idempotente. */
    suspend fun abandon(sessionId: String)

    suspend fun credentialFor(sessionId: String): StoredCredential?
    suspend fun latestCredential(): StoredCredential?

    /** Inserta la credencial; si ya existe una para el `cid` devuelve la existente (idempotente). */
    suspend fun insertCredential(credential: StoredCredential): StoredCredential
}
