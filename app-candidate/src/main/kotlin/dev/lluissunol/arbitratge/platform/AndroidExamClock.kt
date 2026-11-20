package dev.lluissunol.arbitratge.platform

import android.content.ContentResolver
import android.os.SystemClock
import android.provider.Settings
import dev.lluissunol.arbitratge.exam.ExamClock

/** Reloj real (03 §8.1): `elapsedRealtime` es la única base del cómputo; la hora de pared solo informa. */
class AndroidExamClock(private val resolver: ContentResolver) : ExamClock {
    override fun monotonicMs(): Long = SystemClock.elapsedRealtime()
    override fun wallMs(): Long = System.currentTimeMillis()

    /** `BOOT_COUNT` (API 24); nulo si no se puede leer o no es plausible (03 §8.3). */
    override fun bootCount(): Int? =
        try {
            Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT, -1).takeIf { it >= 0 }
        } catch (e: Exception) {
            null
        }
}
