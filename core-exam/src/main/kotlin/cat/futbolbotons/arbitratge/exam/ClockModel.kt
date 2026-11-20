package cat.futbolbotons.arbitratge.exam

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Reloj persistido con ancla (03 §8.2). Se guarda en una sola fila de `session`.
 * [anchorWallMs] es obligatorio (se actualiza siempre junto al ancla): sirve de indicio de reinicio
 * cuando `BOOT_COUNT` no es fiable (§8.3). Nunca decide el tiempo restante.
 */
@Serializable
data class ClockState(
    val limitMs: Long,
    val remainingMsSaved: Long,
    val anchorMono: Long,
    val anchorBoot: Int?,
    val anchorWallMs: Long,
)

data class Reconciled(val state: ClockState, val rebooted: Boolean)

object ExamClockModel {
    /** Tolerancia entre avance de pared y monotónico cuando no hay `BOOT_COUNT` fiable (§8.3). */
    const val WALL_TOLERANCE_MS = 2 * 60_000L

    /** Estado inicial, misma transacción que el inicio de la sesión. */
    fun initial(limitMs: Long, nowMono: Long, boot: Int?, nowWall: Long): ClockState {
        require(limitMs > 0) { "limitMs debe ser > 0" }
        return ClockState(limitMs, limitMs, nowMono, boot, nowWall)
    }

    /** Restante en `nowMono` (puede ser <= 0 = agotado). Asume el mismo arranque (usar antes [reconcile]). */
    fun remaining(state: ClockState, nowMono: Long): Long {
        val elapsed = (nowMono - state.anchorMono).coerceAtLeast(0L)
        return state.remainingMsSaved - elapsed
    }

    /** Tiempo empleado = límite − restante (acotado a `[0, limit]`). Entrada de `ExamEngine.grade`. */
    fun elapsedMs(state: ClockState, nowMono: Long): Long =
        (state.limitMs - remaining(state, nowMono)).coerceIn(0L, state.limitMs)

    fun isExpired(remainingMs: Long): Boolean = remainingMs <= 0

    /** Punto de control: restante nuevo y ancla nueva a la vez; restante limitado a `[0, limit]`. */
    fun checkpoint(state: ClockState, nowMono: Long, nowBoot: Int?, nowWall: Long): ClockState =
        state.copy(
            remainingMsSaved = remaining(state, nowMono).coerceIn(0L, state.limitMs),
            anchorMono = nowMono,
            anchorBoot = nowBoot,
            anchorWallMs = nowWall,
        )

    /** ¿Hubo reinicio desde el ancla? (§8.3) */
    fun rebootDetected(state: ClockState, nowMono: Long, nowBoot: Int?, nowWall: Long): Boolean {
        if (nowMono < state.anchorMono) return true // imposible en el mismo arranque
        val bootReliable = nowBoot != null && state.anchorBoot != null
        if (bootReliable) return nowBoot != state.anchorBoot
        val wallAdvance = nowWall - state.anchorWallMs
        val monoAdvance = nowMono - state.anchorMono
        return abs(wallAdvance - monoAdvance) > WALL_TOLERANCE_MS
    }

    /**
     * Al reabrir: si hay reinicio, reancla dejando `remainingMsSaved` intacto (el tiempo apagado no cuenta).
     * Idempotente: repetirlo con el mismo arranque no cambia nada más.
     */
    fun reconcile(state: ClockState, nowMono: Long, nowBoot: Int?, nowWall: Long): Reconciled =
        if (rebootDetected(state, nowMono, nowBoot, nowWall))
            Reconciled(state.copy(anchorMono = nowMono, anchorBoot = nowBoot, anchorWallMs = nowWall), true)
        else Reconciled(state, false)

    /** Una respuesta tocada con restante >= 0 es válida aunque se guarde después. */
    fun tapAccepted(remainingAtTapMs: Long): Boolean = remainingAtTapMs >= 0

    /**
     * Fin por tiempo (§4.3): restante <= 0 y no hay respuesta pendiente de guardar tocada con restante >= 0.
     * [pendingTapRemainingMs]: restante al tocar de la respuesta aún sin guardar, o null.
     */
    fun shouldTimeOut(remainingMs: Long, pendingTapRemainingMs: Long? = null): Boolean =
        isExpired(remainingMs) && !(pendingTapRemainingMs != null && tapAccepted(pendingTapRemainingMs))
}
