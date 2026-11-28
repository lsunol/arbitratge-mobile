package dev.lluissunol.arbitratge

import android.app.Application
import android.content.Context
import dev.lluissunol.arbitratge.credential.ArbCredentialIssuing
import dev.lluissunol.arbitratge.credential.CredentialIssuing
import dev.lluissunol.arbitratge.data.AppDatabase
import dev.lluissunol.arbitratge.data.RoomSessionStore
import dev.lluissunol.arbitratge.data.SessionStore
import dev.lluissunol.arbitratge.exam.Bank
import dev.lluissunol.arbitratge.exam.SecureRandomRng
import dev.lluissunol.arbitratge.platform.AndroidExamClock
import dev.lluissunol.arbitratge.platform.BankLoader
import dev.lluissunol.arbitratge.platform.SealMaterial
import dev.lluissunol.arbitratge.ui.ExamDeps
import dev.lluissunol.arbitratge.ui.WelcomePrefs

/** Inyección manual (03 §4.1): el grafo es pequeño. */
class AppContainer(private val context: Context) {
    private val db: AppDatabase by lazy { AppDatabase.open(context) }
    val store: SessionStore by lazy { RoomSessionStore(db.sessionDao()) }

    private val prefs = context.getSharedPreferences("arbitratge_prefs", Context.MODE_PRIVATE)
    private val welcomePrefs = object : WelcomePrefs {
        override var welcomeSeen: Boolean
            get() = prefs.getBoolean("welcome_seen", false)
            set(value) { prefs.edit().putBoolean("welcome_seen", value).apply() }
    }

    private val bank: Result<Bank> by lazy { BankLoader.load(context) }
    private val issuing: Result<CredentialIssuing> by lazy {
        SealMaterial.load(context).mapCatching { ArbCredentialIssuing(it) }
    }

    fun examDeps(): ExamDeps = ExamDeps(
        store = store,
        clock = AndroidExamClock(context.contentResolver),
        rng = SecureRandomRng(),
        bank = { bank },
        issuing = { issuing },
        prefs = welcomePrefs,
        appVersion = appVersion,
    )

    /** `versionName` del paquete instalado (viaja en la credencial como `app`). */
    private val appVersion: String by lazy {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
            ?.takeIf { it.matches(Regex("[0-9A-Za-z._+-]{1,24}")) } ?: "0.0.0"
    }
}

class ArbitratgeApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
