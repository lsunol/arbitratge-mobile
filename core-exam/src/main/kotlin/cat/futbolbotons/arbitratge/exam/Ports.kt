package cat.futbolbotons.arbitratge.exam

import java.security.SecureRandom
import kotlin.random.Random

/** Puerto de aleatoriedad (03 §4.1). */
interface Rng {
    /** Entero uniforme en `[0, bound)`; `bound > 0`. */
    fun nextInt(bound: Int): Int
}

class SecureRandomRng(private val random: SecureRandom = SecureRandom()) : Rng {
    override fun nextInt(bound: Int): Int = random.nextInt(bound)
}

/** Semilla fija: pruebas deterministas. */
class SeededRng(seed: Long) : Rng {
    private val random = Random(seed)
    override fun nextInt(bound: Int): Int = random.nextInt(bound)
}

/** Puerto de reloj (03 §4.1, §8.1). */
interface ExamClock {
    /** `SystemClock.elapsedRealtime()`: base única del cómputo. */
    fun monotonicMs(): Long

    /** Hora de pared: solo informativa. */
    fun wallMs(): Long

    /** `BOOT_COUNT`; nulo si no es fiable. */
    fun bootCount(): Int?
}
