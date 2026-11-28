package dev.lluissunol.arbitratge.support

import dev.lluissunol.arbitratge.credential.ArbCredentialIssuing
import dev.lluissunol.arbitratge.credential.CredentialIssuing
import dev.lluissunol.arbitratge.credential.IssueRequest
import dev.lluissunol.arbitratge.credential.IssueResult
import dev.lluissunol.arbitratge.data.Finish
import dev.lluissunol.arbitratge.data.SessionNotActiveException
import dev.lluissunol.arbitratge.data.SessionStore
import dev.lluissunol.arbitratge.data.StorageException
import dev.lluissunol.arbitratge.data.StoredCredential
import dev.lluissunol.arbitratge.data.StoredSession
import dev.lluissunol.arbitratge.exam.AnswerRecord
import dev.lluissunol.arbitratge.exam.Bank
import dev.lluissunol.arbitratge.exam.ClockState
import dev.lluissunol.arbitratge.exam.ExamClock
import dev.lluissunol.arbitratge.exam.SeededRng
import dev.lluissunol.arbitratge.exam.SessionState
import dev.lluissunol.arbitratge.platform.BankLoader
import dev.lluissunol.arbitratge.platform.SealMaterial
import dev.lluissunol.arbitratge.ui.ExamDeps
import dev.lluissunol.arbitratge.ui.WelcomePrefs
import java.io.File
import java.time.ZoneId

/** Reloj controlable (03 §11: reinicios, BOOT_COUNT ausente, saltos de hora). */
class FakeClock(var mono: Long = 1_000_000L, var wall: Long = 1_800_000_000_000L, var boot: Int? = 7) : ExamClock {
    override fun monotonicMs() = mono
    override fun wallMs() = wall
    override fun bootCount() = boot
    fun advance(ms: Long) {
        mono += ms
        wall += ms
    }
}

class FakePrefs(override var welcomeSeen: Boolean = true) : WelcomePrefs

enum class Op { CREATE, COMMIT_ANSWER, SAVE_PROGRESS, SAVE_CLOCK, FINISH, ABANDON, INSERT_CREDENTIAL, READ, READ_PENDING }

/**
 * Almacén en memoria con la misma semántica transaccional que Room (cada operación es atómica: o se aplica
 * entera o falla sin efectos) y con fallos inyectables por operación (03 §11).
 */
class InMemoryStore : SessionStore {
    val sessions = LinkedHashMap<String, StoredSession>()
    val credentials = LinkedHashMap<String, StoredCredential>()
    private val failing = mutableMapOf<Op, Int>() // Int.MAX_VALUE = siempre
    val calls = mutableListOf<Op>()

    fun failAlways(op: Op) { failing[op] = Int.MAX_VALUE }
    fun failTimes(op: Op, n: Int) { failing[op] = n }
    fun heal(op: Op) { failing.remove(op) }
    fun healAll() = failing.clear()
    fun count(op: Op) = calls.count { it == op }

    private fun maybeFail(op: Op) {
        calls += op
        val n = failing[op] ?: return
        if (n != Int.MAX_VALUE) { if (n <= 1) failing.remove(op) else failing[op] = n - 1 }
        throw StorageException("fallo inyectado: $op")
    }

    override suspend fun active() = sessions.values.firstOrNull { it.state == SessionState.IN_PROGRESS }.also { maybeFail(Op.READ); failIfCorrupt() }
    override suspend fun latestFinished() =
        sessions.values.lastOrNull { it.state == SessionState.FINISHED }.also { maybeFail(Op.READ) }
    /** Ids "corruptos": toda lectura falla (como una fila ilegible en Room) hasta que se ponen en cuarentena. */
    val corruptIds = mutableSetOf<String>()
    private fun failIfCorrupt() { if (corruptIds.isNotEmpty()) throw StorageException("dades il·legibles") }
    override suspend fun quarantineUnreadable(): Int {
        val n = corruptIds.size
        corruptIds.forEach { sessions.remove(it) }
        corruptIds.clear()
        return n
    }
    override suspend fun finishedWithoutCredential() =
        sessions.values.filter { it.state == SessionState.FINISHED && credentials.values.none { c -> c.sessionId == it.id } }
            .also { maybeFail(Op.READ_PENDING); failIfCorrupt() }
    override suspend fun session(id: String) = sessions[id].also { maybeFail(Op.READ) }
    override suspend fun failedExamsBefore(sessionId: String): Int {
        maybeFail(Op.READ)
        return sessions.values.takeWhile { it.id != sessionId }.count {
            it.kind == dev.lluissunol.arbitratge.data.SessionKind.EXAM && it.state == SessionState.FINISHED && it.correctCount != null &&
                it.correctCount < it.session.snapshot.rules.passThresholdCount
        }
    }

    override suspend fun create(session: StoredSession) {
        maybeFail(Op.CREATE)
        check(sessions.values.none { it.state == SessionState.IN_PROGRESS }) { "ja hi ha una sessió activa" }
        sessions[session.id] = session
    }

    var commitGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    var finishGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    override suspend fun commitAnswer(sessionId: String, answer: AnswerRecord, currentIndex: Int, clock: ClockState) {
        maybeFail(Op.COMMIT_ANSWER)
        commitGate?.await()
        val s = sessions[sessionId]?.takeIf { it.state == SessionState.IN_PROGRESS } ?: throw SessionNotActiveException("no activa")
        // UPSERT por (session, index): la respuesta se puede sustituir o vaciar.
        val answers = (s.session.answers.filter { it.index != answer.index } + answer).sortedBy { it.index }
        sessions[sessionId] = s.copy(session = s.session.copy(answers = answers), currentIndex = currentIndex, clock = clock)
    }

    /** Si no es null, `saveProgress` se suspende hasta completarlo (guardado de posición lento). */
    var saveProgressGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    override suspend fun saveProgress(sessionId: String, currentIndex: Int, clock: ClockState) {
        maybeFail(Op.SAVE_PROGRESS)
        saveProgressGate?.await()
        val s = sessions[sessionId]?.takeIf { it.state == SessionState.IN_PROGRESS } ?: throw SessionNotActiveException("no activa")
        sessions[sessionId] = s.copy(currentIndex = currentIndex, clock = clock)
    }

    /** Si no es null, `saveClock` se suspende hasta completarlo (checkpoint lento que retiene el mutex). */
    var saveClockGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    override suspend fun saveClock(sessionId: String, clock: ClockState) {
        maybeFail(Op.SAVE_CLOCK)
        saveClockGate?.await()
        sessions[sessionId]?.takeIf { it.state == SessionState.IN_PROGRESS }?.let { sessions[sessionId] = it.copy(clock = clock) }
    }

    override suspend fun finish(sessionId: String, clock: ClockState, finish: Finish) {
        maybeFail(Op.FINISH)
        finishGate?.await()
        val s = sessions[sessionId] ?: throw SessionNotActiveException("inexistent")
        if (s.state == SessionState.FINISHED) return
        if (s.state != SessionState.IN_PROGRESS) throw SessionNotActiveException("no activa")
        sessions[sessionId] = s.copy(
            state = SessionState.FINISHED, clock = clock, finishReason = finish.reason,
            correctCount = finish.correctCount, finishedWall = finish.finishedWall,
        )
    }

    override suspend fun abandon(sessionId: String) {
        maybeFail(Op.ABANDON)
        sessions[sessionId]?.takeIf { it.state == SessionState.IN_PROGRESS }?.let { sessions[sessionId] = it.copy(state = SessionState.ABANDONED) }
    }

    override suspend fun credentialFor(sessionId: String) = credentials.values.firstOrNull { it.sessionId == sessionId }
    override suspend fun latestCredential() = credentials.values.lastOrNull()

    /** Sesiones cuyo insert de credencial falla (fallo transitorio "en medio" de la cola). */
    val failInsertForSession = mutableSetOf<String>()
    var insertGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    override suspend fun insertCredential(credential: StoredCredential): StoredCredential {
        maybeFail(Op.INSERT_CREDENTIAL)
        insertGate?.await()
        if (credential.sessionId in failInsertForSession) throw StorageException("fallo inyectado en la sesión")
        credentials.values.firstOrNull { it.sessionId == credential.sessionId }?.let { return it }
        credentials[credential.cid] = credential
        return credential
    }
}

/** Emisor falso configurable; delega en el real cuando [mode] es OK. */
class SwitchableIssuing(private val real: CredentialIssuing) : CredentialIssuing {
    enum class Mode { OK, THROW }
    var mode = Mode.OK
    val cids = mutableListOf<String>()
    override fun issue(cid: String, request: IssueRequest): IssueResult {
        cids += cid
        if (mode == Mode.THROW) throw IllegalStateException("fallo inyectado en el segell")
        return real.issue(cid, request)
    }
}

object TestEnv {
    /** Módulo `app-candidate` es el directorio de trabajo de los tests unitarios. */
    private val moduleDir = File(System.getProperty("user.dir"))
    val sampleBankText: String by lazy { File(moduleDir, "../bank-sample/bank-sample.json").readText() }
    val bank: Bank by lazy { BankLoader.parse(sampleBankText) }
    val devKey: ByteArray by lazy { File(moduleDir, "src/debug/seal/dev-seal-key.der").readBytes() }
    val devTrustedJson: String by lazy { File(moduleDir, "src/debug/seal/dev-trusted.json").readText() }
    fun devTrusted() = dev.lluissunol.arbitratge.credential.TrustedKeys.of(
        listOf(dev.lluissunol.arbitratge.credential.TrustedKeys.parseEntry(devTrustedJson, "dev").getOrThrow()),
    )
    fun realIssuingToken(cid: String = "7f3a92c1-4b5d-4e6f-8a9b-000000000001"): String =
        (realIssuing.issue(cid, IssueRequest("Maria Puig Vila", "sample-1", java.time.LocalDate.of(2026, 11, 20), 27, 30, 1834, 0, "0.0.0-sample", "0.0.0")) as IssueResult.Issued).token
    val realIssuing: CredentialIssuing by lazy { ArbCredentialIssuing(SealMaterial(devKey, "dev")) }

    fun deps(
        store: SessionStore,
        clock: FakeClock,
        issuing: CredentialIssuing = realIssuing,
        prefs: WelcomePrefs = FakePrefs(),
        bank: Result<Bank> = Result.success(this.bank),
        seed: Long = 42,
    ): ExamDeps {
        var n = 0
        return ExamDeps(
            store = store, clock = clock, rng = SeededRng(seed), bank = { bank }, issuing = { Result.success(issuing) },
            prefs = prefs,
            newSessionId = { "session-${++n}" },
            newCid = { "7f3a92c1-4b5d-4e6f-8a9b-%012d".format(n) },
            zone = ZoneId.of("Europe/Madrid"),
        )
    }
}
