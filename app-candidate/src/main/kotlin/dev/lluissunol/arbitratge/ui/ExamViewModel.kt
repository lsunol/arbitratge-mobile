package dev.lluissunol.arbitratge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.lluissunol.arbitratge.credential.CredentialIssuing
import dev.lluissunol.arbitratge.credential.IssueRequest
import dev.lluissunol.arbitratge.credential.IssueResult
import dev.lluissunol.arbitratge.credential.NameError
import dev.lluissunol.arbitratge.credential.NameResult
import dev.lluissunol.arbitratge.credential.NameRules
import dev.lluissunol.arbitratge.data.Finish
import dev.lluissunol.arbitratge.data.SessionKind
import dev.lluissunol.arbitratge.data.SessionNotActiveException
import dev.lluissunol.arbitratge.data.SessionStore
import dev.lluissunol.arbitratge.data.StorageException
import dev.lluissunol.arbitratge.data.StoredCredential
import dev.lluissunol.arbitratge.data.StoredSession
import dev.lluissunol.arbitratge.exam.AnswerRecord
import dev.lluissunol.arbitratge.exam.Bank
import dev.lluissunol.arbitratge.exam.ClockState
import dev.lluissunol.arbitratge.exam.ExamClock
import dev.lluissunol.arbitratge.exam.ExamClockModel
import dev.lluissunol.arbitratge.exam.ExamEngine
import dev.lluissunol.arbitratge.exam.ExamEngineException
import dev.lluissunol.arbitratge.exam.ExamResult
import dev.lluissunol.arbitratge.exam.ExamStateMachine
import dev.lluissunol.arbitratge.exam.FinishReason
import dev.lluissunol.arbitratge.exam.QuestionType
import dev.lluissunol.arbitratge.exam.Rng
import dev.lluissunol.arbitratge.exam.SessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Bandera de primer uso (C1). No es crítica: perderla solo repite la bienvenida. */
interface WelcomePrefs {
    var welcomeSeen: Boolean
}

/** Dependencias (puertos) del ViewModel: en tests se inyectan fakes (03 §4.1, §11). */
class ExamDeps(
    val store: SessionStore,
    val clock: ExamClock,
    val rng: Rng,
    val bank: () -> Result<Bank>,
    val issuing: () -> Result<CredentialIssuing>,
    val prefs: WelcomePrefs,
    val newSessionId: () -> String = { UUID.randomUUID().toString() },
    /** UUID v4 en minúsculas (02 §3.7.5). */
    val newCid: () -> String = { UUID.randomUUID().toString() },
    val zone: ZoneId = ZoneId.systemDefault(),
)

/** Eventos de una sola vez hacia la Activity. */
sealed interface UiEvent {
    /** "Sortir": se cierra la app; el tiempo sigue corriendo (04 §2.4 C5-a). */
    data object LeaveApp : UiEvent
}

/**
 * Orquesta el flujo completo (03 §4.3). La regla de oro (B3): **persistir y luego avanzar**; la
 * pantalla solo cambia después del commit y un fallo de guardado deja al usuario donde está.
 */
class ExamViewModel(private val deps: ExamDeps) : ViewModel() {

    private val _stage = MutableStateFlow<Stage>(Stage.Loading)
    val stage: StateFlow<Stage> = _stage

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Serializa las operaciones que escriben (doble toque, tick frente a guardado…). */
    private val lock = Mutex()

    private var bank: Bank? = null
    private var summary = RulesSummary(0, 0, 0)

    // Estado de pantalla de la pregunta (nada de esto es verdad persistida salvo `current`).
    private var current: StoredSession? = null
    private var selected: Set<String> = emptySet()
    /** Verdadero desde que se decide el fin por tiempo: los taps nuevos se rechazan de forma síncrona. */
    private var timingOut = false
    private var pending: PendingTap? = null
    private val pendingTapMs: Long? get() = pending?.remainingMs
    private var timeHidden = false
    private var dismissedBanner: TimeBanner? = null
    private var dialog: QuestionDialog? = null
    private var saving = false
    private var saveError: SaveError? = null
    private var afterNoAnswerFinish = false

    private var nameDraft = ""
    private var confirmedNom: String? = null
    private var tickJob: Job? = null
    private var ticks = 0
    private var gradingRetry: (suspend () -> Unit)? = null

    init {
        viewModelScope.launch { lock.withLock { resolve() } }
    }

    // ------------------------------------------------------------------ arranque / recuperación

    /** Deriva el `Stage` del estado persistido (03 §4.5). */
    private suspend fun resolve() {
        stopTick()
        resetQuestionState()
        val b = deps.bank().getOrElse {
            _stage.value = Stage.BuildError
            return
        }
        bank = b
        summary = RulesSummary(b.rules.numQuestions, b.rules.timeLimitMinutes, b.rules.passThresholdCount)
        try {
            val active = deps.store.active()
            if (active != null) {
                resumeActive(active)
                return
            }
            // Recuperación (03 §4.3): SOLO la sesión aprobada más reciente sin credencial es "el resultado actual"
            // y dirige la navegación (Result(Passed) + emisión automática). Las demás se procesan en segundo plano.
            val newest = try {
                deps.store.finishedWithoutCredential().lastOrNull { mustIssue(it) }
            } catch (e: StorageException) {
                null // el fallo de lectura solo se refleja en la tarjeta de Home
            }
            if (newest != null) showResult(newest)
            else _stage.value = if (!deps.prefs.welcomeSeen) Stage.Welcome else home()
            backgroundIssuePass()
            return
        } catch (e: StorageException) {
            _stage.value = Stage.StateError
        }
    }

    private suspend fun mustIssue(s: StoredSession): Boolean =
        ExamStateMachine.shouldIssueCredential(
            s.state, s.finishReason, s.correctCount, s.session.snapshot.rules.passThresholdCount,
            credentialExists = deps.store.credentialFor(s.id) != null, isPractice = s.kind == SessionKind.PRACTICE,
        )

    private suspend fun resumeActive(s: StoredSession) {
        val rec = ExamClockModel.reconcile(s.clock, deps.clock.monotonicMs(), deps.clock.bootCount(), deps.clock.wallMs())
        if (rec.rebooted) {
            // Reanclaje atómico e idempotente; si no se puede escribir se repetirá (el restante guardado no cambia).
            // El reanclaje DEBE quedar persistido antes de entrar: si no, un proceso muerto concedería tiempo.
            val ok = withRetry { deps.store.saveClock(s.id, rec.state) }
            if (ok.isFailure) {
                _stage.value = Stage.RecoveryError
                return
            }
        }
        val sess = s.copy(clock = rec.state)
        current = sess
        when {
            sess.session.isComplete -> {
                // Estado improbable (la última respuesta cierra la sesión en la misma transacción): se cierra.
                val r = ExamEngine.grade(sess.session, ExamClockModel.elapsedMs(sess.clock, deps.clock.monotonicMs()))
                val finish = Finish(FinishReason.COMPLETED, r.correct, deps.clock.wallMs())
                persistFinish(sess, sess.clock, finish) ?: return
            }
            ExamClockModel.shouldTimeOut(ExamClockModel.remaining(sess.clock, deps.clock.monotonicMs())) -> finishTimedOut(away = true)
            else -> enterQuestion()
        }
    }

    private suspend fun home(): Stage.Home {
        val last = deps.store.latestFinished()?.let {
            LastResult(it.id, it.correctCount ?: 0, it.session.total, it.correctCount?.let { c -> c >= it.session.snapshot.rules.passThresholdCount } == true)
        }
        return Stage.Home(summary, deps.store.latestCredential(), last, pendingCard())
    }

    /** Tarjeta de Home: el recuento sale SIEMPRE de la BD (nada puede quedar descolgado). */
    private suspend fun pendingCard(): PendingIssuance? {
        val count = try {
            deps.store.finishedWithoutCredential().count { mustIssue(it) }
        } catch (e: StorageException) {
            return PendingIssuance(0, error = true)
        }
        return if (count == 0) null else PendingIssuance(count, error = retryFailed)
    }

    /** Se pidió "Tornar-ho a provar" y aún quedan pendientes. */
    private var retryFailed = false

    /**
     * Pasada en segundo plano, idempotente: intenta emitir las sesiones aprobadas sin credencial (salvo la que
     * muestra el Result actual). NUNCA cambia de pantalla: solo refresca la tarjeta si el Stage es Home.
     * Un fallo de una sesión no detiene las demás; los fallos son silenciosos pero siguen contando en la BD.
     */
    private suspend fun backgroundIssuePass() {
        val currentId = (_stage.value as? Stage.Result)?.sessionId
        try {
            for (p in deps.store.finishedWithoutCredential().filter { it.id != currentId && mustIssue(it) }) issue(p)
        } catch (e: StorageException) {
            // reflejado en la tarjeta (pendingCard lo vuelve a leer)
        }
        if (_stage.value is Stage.Home) {
            _stage.value = try {
                home()
            } catch (e: StorageException) {
                return
            }
        }
    }

    /** "Tornar-ho a provar" de la tarjeta de Home. */
    fun onRetryPending() = op {
        backgroundIssuePass()
        val home = _stage.value as? Stage.Home ?: return@op
        retryFailed = home.pending != null
        _stage.value = home.copy(pending = home.pending?.copy(error = retryFailed))
    }

    // ------------------------------------------------------------------ C0–C3

    fun onWelcomeUnderstood() = op {
        deps.prefs.welcomeSeen = true
        goHomeLocked()
    }

    fun onStartExam() = op {
        val prefill = try {
            deps.store.latestFinished()?.candidateName
        } catch (e: StorageException) {
            null
        }
        nameDraft = confirmedNom ?: prefill ?: ""
        _stage.value = Stage.Name(nameDraft, null)
    }

    fun onNameChange(text: String) {
        nameDraft = text
        if (_stage.value is Stage.Name) _stage.value = Stage.Name(text, null)
    }

    fun onNameContinue() {
        if (_stage.value !is Stage.Name) return
        when (val r = NameRules.normalize(nameDraft)) {
            is NameResult.Invalid -> _stage.value = Stage.Name(nameDraft, r.error.toProblem())
            is NameResult.Valid ->
                if (r.nom.codePointCount(0, r.nom.length) < 2) {
                    _stage.value = Stage.Name(nameDraft, NameProblem.EMPTY)
                } else {
                    confirmedNom = r.nom
                    _stage.value = Stage.Rules(r.nom, summary, starting = false, saveError = false)
                }
        }
    }

    fun onChangeName() {
        val nom = confirmedNom ?: return
        nameDraft = nom
        _stage.value = Stage.Name(nom, null)
    }

    /** "Començar ara": crea y persiste la sesión (nombre congelado, `cid`, instantánea, reloj). */
    fun onBeginExam() = op {
        val nom = confirmedNom ?: return@op
        val b = bank ?: return@op
        if (_stage.value !is Stage.Rules) return@op
        _stage.value = Stage.Rules(nom, summary, starting = true, saveError = false)
        val session = try {
            ExamEngine.start(b, b.rules, deps.rng)
        } catch (e: ExamEngineException) {
            _stage.value = Stage.BuildError
            return@op
        }
        val clock = deps.clock
        val stored = StoredSession(
            id = deps.newSessionId(), kind = SessionKind.EXAM, state = SessionState.IN_PROGRESS,
            cid = deps.newCid(), candidateName = nom, session = session, currentIndex = 0,
            clock = ExamClockModel.initial(b.rules.limitMs, clock.monotonicMs(), clock.bootCount(), clock.wallMs()),
            startWall = clock.wallMs(),
        )
        val ok = withRetry { deps.store.create(stored) }
        if (ok.isFailure) {
            _stage.value = Stage.Rules(nom, summary, starting = false, saveError = true)
            return@op
        }
        current = stored
        enterQuestion()
    }

    // ------------------------------------------------------------------ C4

    private fun enterQuestion() {
        resetQuestionState()
        publishQuestion()
        startTick()
    }

    private fun resetQuestionState() {
        selected = emptySet()
        pending = null
        dialog = null
        saving = false
        saveError = null
        afterNoAnswerFinish = false
        dismissedBanner = null
        timingOut = false
        ticks = 0
    }

    private fun remainingNow(s: StoredSession): Long = ExamClockModel.remaining(s.clock, deps.clock.monotonicMs())

    private fun publishQuestion() {
        val s = current ?: return
        val idx = s.session.currentIndex
        val q = s.session.snapshot.questions.getOrNull(idx) ?: return
        val rem = remainingNow(s).coerceAtLeast(0L)
        // Un umbral solo avisa si el límite lo supera (04 §2.4): con 5 min de límite no hay aviso de 5 min al empezar.
        val limit = s.clock.limitMs
        val level = when {
            rem <= 60_000L && limit > 60_000L -> TimeBanner.ONE_MIN
            rem <= 300_000L && limit > 300_000L -> TimeBanner.FIVE_MIN
            else -> null
        }
        val d = dismissedBanner
        val banner = level?.takeIf { d == null || it.ordinal > d.ordinal }
        _stage.value = Stage.Question(idx, s.session.total, q, selected, rem, timeHidden, banner, dialog, saving, saveError, level)
    }

    fun onSelect(optionId: String) {
        val st = _stage.value as? Stage.Question ?: return
        if (saving || pendingTapMs != null) return // tras tocar "Següent" la selección queda fijada (B3)
        if (optionId !in st.question.options.map { it.id }) return
        selected = if (st.question.type == QuestionType.MULTIPLE_CHOICE) {
            if (optionId in selected) selected - optionId else selected + optionId
        } else setOf(optionId)
        publishQuestion()
    }

    fun onToggleTimeHidden() {
        timeHidden = !timeHidden
        publishQuestion()
    }

    fun onDismissBanner() {
        val st = _stage.value as? Stage.Question ?: return
        dismissedBanner = st.banner
        publishQuestion()
    }

    /** "Següent" / "Finalitzar l'examen". */
    fun onNext() {
        val st = _stage.value as? Stage.Question ?: return
        if (saving || st.dialog != null) return
        when {
            selected.isEmpty() -> { dialog = QuestionDialog.NO_ANSWER; afterNoAnswerFinish = st.isLast }
            st.isLast -> dialog = QuestionDialog.FINISH
            else -> { commit(); return }
        }
        publishQuestion()
    }

    /** "Continuar igualment" (C5-b) o "Finalitzar" (C5-c). */
    fun onDialogConfirm() {
        when (dialog) {
            QuestionDialog.NO_ANSWER -> if (afterNoAnswerFinish) {
                dialog = QuestionDialog.FINISH
                publishQuestion()
            } else commit()
            QuestionDialog.FINISH -> commit()
            QuestionDialog.EXIT -> {
                dialog = null
                publishQuestion()
                checkpointNow()
                _events.trySend(UiEvent.LeaveApp)
            }
            null -> Unit
        }
    }

    /** Botón seguro de los diálogos: "Marcar una resposta" / "Revisar" / "Continuar". */
    fun onDialogDismiss() {
        dialog = null
        afterNoAnswerFinish = false
        publishQuestion()
    }

    /** Pulsación confirmada, capturada SÍNCRONAMENTE (índice, selección y restante al tocar). */
    private data class PendingTap(val index: Int, val ids: Set<String>, val remainingMs: Long)

    /**
     * Captura el tap en el hilo de la UI, antes de esperar el mutex de escritura: un punto de control que
     * retenga el mutex no puede hacer caducar un tap a tiempo, y un segundo evento (doble toque, evento
     * obsoleto) se rechaza porque ya hay un tap pendiente o el índice no coincide.
     */
    private fun commit() {
        val s = current ?: return
        val st = _stage.value as? Stage.Question ?: return
        if (timingOut || pending != null || saving || st.index != s.session.currentIndex) return
        val tap = remainingNow(s)
        dialog = null
        afterNoAnswerFinish = false
        if (tap < 0) { // el tiempo ya se había agotado al tocar: la respuesta no cuenta
            timingOut = true
            op { if (_stage.value is Stage.Question) finishTimedOut(away = false) }
            return
        }
        pending = PendingTap(st.index, selected, tap)
        startSave()
    }

    private fun startSave() {
        saving = true
        saveError = null
        publishQuestion()
        op { persistPending() }
    }

    private suspend fun persistPending() {
        val p = pending ?: return
        val s = current
        if (s == null || s.session.currentIndex != p.index) { // obsoleto
            pending = null
            saving = false
            return
        }
        val idx = p.index
        val last = idx == s.session.total - 1
        val outcome = try {
            withRetry { persistAnswer(s, idx, p.ids, p.remainingMs, last) }
        } catch (e: SessionNotActiveException) {
            resolve()
            return
        } catch (e: ExamEngineException) {
            _stage.value = Stage.StateError
            return
        }
        saving = false
        val result = outcome.getOrNull()
        if (result == null) {
            saveError = if (last) SaveError.RESULT else SaveError.ANSWER
            publishQuestion()
            return
        }
        pending = null
        current = result
        if (result.state == SessionState.FINISHED) {
            stopTick()
            gradingThenResult(result)
        } else {
            selected = emptySet()
            dialog = null
            publishQuestion()
        }
    }

    private suspend fun persistAnswer(s: StoredSession, idx: Int, ids: Set<String>, tap: Long, last: Boolean): StoredSession {
        val newSession = ExamEngine.answer(s.session, idx, ids, tap)
        val c = deps.clock
        val mono = c.monotonicMs()
        val boot = c.bootCount()
        val wall = c.wallMs()
        val limit = s.clock.limitMs
        val clock = if (last) ClockState(limit, tap.coerceIn(0L, limit), mono, boot, wall)
        else ExamClockModel.checkpoint(s.clock, mono, boot, wall)
        val finish = if (last) {
            val r = ExamEngine.grade(newSession, limit - clock.remainingMsSaved)
            Finish(FinishReason.COMPLETED, r.correct, wall)
        } else null
        deps.store.commitAnswer(s.id, newSession.answers.first { it.index == idx }, clock, finish)
        return s.copy(
            session = newSession, currentIndex = idx + 1, clock = clock,
            state = if (finish != null) SessionState.FINISHED else SessionState.IN_PROGRESS,
            finishReason = finish?.reason, correctCount = finish?.correctCount, finishedWall = finish?.finishedWall,
        )
    }

    /** "Torna-ho a provar" tras un fallo de guardado: reintenta SIN recapturar el tiempo de la pulsación. */
    fun onRetrySave() {
        if (saveError == null || pending == null || saving) return
        startSave()
    }

    /** "Sortir de l'examen" desde el aviso de fallo de guardado, o "Abandonar": `ABANDONED` solo si se guarda. */
    fun onAbandon() = op {
        val s = current ?: return@op
        val ok = withRetry { deps.store.abandon(s.id) }
        if (ok.isFailure) return@op // seguimos donde estábamos (se recupera al reabrir)
        stopTick()
        current = null
        resetQuestionState()
        goHomeLocked()
    }

    // ------------------------------------------------------------------ reloj

    private fun startTick() {
        stopTick()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                tick()
            }
        }
    }

    private fun stopTick() {
        tickJob?.cancel()
        tickJob = null
    }

    /** Un tick de reloj: refresca el contador, evalúa el fin por tiempo y el punto de control (~30 s). */
    internal suspend fun tick() {
        val s = current ?: return
        if (_stage.value !is Stage.Question || lock.isLocked) return
        val rem = remainingNow(s)
        if (!timingOut && ExamClockModel.shouldTimeOut(rem, pendingTapMs)) {
            // Arbitraje: desde este instante (síncrono, mismo hilo que los taps) todo tap nuevo se rechaza.
            // Un tap ya capturado (`pending`) impide llegar aquí: el fin por tiempo espera a que se guarde.
            timingOut = true
            // En otra corrutina: finishTimedOut cancela el bucle de ticks (que es quien nos llama).
            viewModelScope.launch {
                lock.withLock {
                    if (_stage.value is Stage.Question && current != null && pending == null) finishTimedOut(away = false)
                    else timingOut = false
                }
            }
            return
        }
        publishQuestion()
        if (++ticks % 30 == 0) checkpointNow()
    }

    /** Punto de control del reloj (`onStop`, cada ~30 s). Un fallo no interrumpe el examen. */
    private fun checkpointNow() {
        val s = current ?: return
        if (_stage.value !is Stage.Question || saving) return
        viewModelScope.launch {
            lock.withLock {
                val cur = current ?: return@withLock
                if (cur.state != SessionState.IN_PROGRESS || saving) return@withLock
                val c = deps.clock
                val next = ExamClockModel.checkpoint(cur.clock, c.monotonicMs(), c.bootCount(), c.wallMs())
                try {
                    deps.store.saveClock(cur.id, next)
                    current = cur.copy(clock = next)
                } catch (_: StorageException) {
                } catch (_: SessionNotActiveException) {
                }
            }
        }
    }

    fun onAppStopped() = checkpointNow()

    fun onAppStarted() {
        viewModelScope.launch { tick() }
    }

    // ------------------------------------------------------------------ fin por tiempo, C6, C7

    private suspend fun finishTimedOut(away: Boolean) {
        val s = current ?: return
        timingOut = true
        stopTick()
        val r = ExamEngine.grade(s.session, s.clock.limitMs)
        val c = deps.clock
        val clock = ClockState(s.clock.limitMs, 0L, c.monotonicMs(), c.bootCount(), c.wallMs())
        val finish = Finish(FinishReason.TIMED_OUT, r.correct, c.wallMs())
        val done = persistFinish(s, clock, finish) ?: run {
            gradingRetry = { finishTimedOut(away) }
            _stage.value = Stage.Grading(error = true)
            return
        }
        _stage.value = Stage.TimeUp(s.session.answers.count { it.selectedIds.isNotEmpty() }, s.session.total, away)
        current = done
    }

    /** Persiste el fin; null si falla (estado Grading con error ya publicado por el llamante). */
    private suspend fun persistFinish(s: StoredSession, clock: ClockState, finish: Finish): StoredSession? {
        val ok = withRetry { deps.store.finish(s.id, clock, finish) }
        if (ok.isFailure) {
            gradingRetry = { persistFinish(s, clock, finish)?.let { gradingThenResult(it) } }
            _stage.value = Stage.Grading(error = true)
            return null
        }
        val done = s.copy(
            state = SessionState.FINISHED, clock = clock, finishReason = finish.reason,
            correctCount = finish.correctCount, finishedWall = finish.finishedWall,
        )
        if (finish.reason == FinishReason.COMPLETED) gradingThenResult(done)
        return done
    }

    /** C6 → "Veure el resultat". */
    fun onSeeResult() = op {
        val s = current ?: return@op
        gradingThenResult(s)
    }

    private suspend fun gradingThenResult(s: StoredSession) {
        _stage.value = Stage.Grading(error = false)
        delay(MIN_GRADING_MS) // evita el parpadeo de C7 (04 §2.6)
        showResult(s)
    }

    /** Reintento desde C7 con error ("Tornar-ho a provar"). */
    fun onRetryGrading() = op {
        val r = gradingRetry ?: return@op
        _stage.value = Stage.Grading(error = false)
        r()
    }

    // ------------------------------------------------------------------ C8/C9 y emisión

    private fun elapsed(s: StoredSession) = (s.clock.limitMs - s.clock.remainingMsSaved).coerceIn(0L, s.clock.limitMs)

    private suspend fun showResult(s: StoredSession) {
        stopTick()
        val result = try {
            ExamEngine.grade(s.session, elapsed(s))
        } catch (e: RuntimeException) {
            _stage.value = Stage.StateError
            return
        }
        try {
            val existing = deps.store.credentialFor(s.id)
            val needs = ExamStateMachine.shouldIssueCredential(
                s.state, s.finishReason, s.correctCount, s.session.snapshot.rules.passThresholdCount,
                credentialExists = existing != null, isPractice = s.kind == SessionKind.PRACTICE,
            )
            when {
                existing != null -> _stage.value = Stage.Result(s.id, result, CredentialPhase.Ready(existing))
                !needs -> _stage.value = Stage.Result(s.id, result, null)
                else -> {
                    _stage.value = Stage.Result(s.id, result, CredentialPhase.Issuing)
                    val phase = issue(s)
                    _stage.value = Stage.Result(s.id, result, phase)
                    if (phase is CredentialPhase.Ready) backgroundIssuePass() // tras una emisión correcta
                }
            }
        } catch (e: StorageException) {
            gradingRetry = { showResult(s) }
            _stage.value = Stage.Grading(error = true)
        }
    }

    /** Emisión de 02 §3.7.10 con el `cid` reservado y el nombre congelado; idempotente. */
    private suspend fun issue(s: StoredSession): CredentialPhase {
        return try {
            val issuing = deps.issuing().getOrElse { return CredentialPhase.Pending }
            val date: LocalDate = Instant.ofEpochMilli(s.finishedWall ?: deps.clock.wallMs()).atZone(deps.zone).toLocalDate()
            val r = issuing.issue(s.cid, IssueRequest(nomInput = s.candidateName, exv = s.examVersion, iat = date, bv = s.bankVersion))
            when (r) {
                is IssueResult.Rejected -> CredentialPhase.Pending
                is IssueResult.Issued -> {
                    val c = r.credential
                    val stored = deps.store.insertCredential(
                        StoredCredential(s.cid, s.id, r.token, c.nom, c.iat, c.kid, s.examVersion, c.bv),
                    )
                    CredentialPhase.Ready(stored)
                }
            }
        } catch (e: StorageException) {
            CredentialPhase.Pending
        } catch (e: RuntimeException) {
            CredentialPhase.Pending
        }
    }

    /** "Tornar-ho a provar" en C8 con la credencial pendiente. */
    fun onRetryCredential() = op {
        val st = _stage.value as? Stage.Result ?: return@op
        if (st.credential != CredentialPhase.Pending) return@op
        val s = try {
            deps.store.session(st.sessionId)
        } catch (e: StorageException) {
            null
        } ?: return@op
        showResult(s)
    }

    // ------------------------------------------------------------------ C10, C11

    fun onReview(sessionId: String, fromResult: Boolean) = op {
        val s = try {
            deps.store.session(sessionId)
        } catch (e: StorageException) {
            null
        } ?: return@op
        val result = ExamEngine.grade(s.session, elapsed(s))
        _stage.value = Stage.Review(sessionId, result, onlyWrong = true, openIndex = null, fromResult = fromResult)
    }

    fun onReviewFilter(onlyWrong: Boolean) {
        val st = _stage.value as? Stage.Review ?: return
        _stage.value = st.copy(onlyWrong = onlyWrong, openIndex = null)
    }

    fun onReviewOpen(index: Int?) {
        val st = _stage.value as? Stage.Review ?: return
        _stage.value = st.copy(openIndex = index)
    }

    fun onOpenCredential(credential: StoredCredential, fromResult: Boolean = false) {
        _stage.value = Stage.Credential(credential, presentation = false, fromResult = fromResult)
    }

    fun onPresentation(on: Boolean) {
        val st = _stage.value as? Stage.Credential ?: return
        _stage.value = st.copy(presentation = on)
    }

    // ------------------------------------------------------------------ navegación

    fun onGoHome() = op { goHomeLocked() }

    private suspend fun goHomeLocked() {
        stopTick()
        resetQuestionState()
        _stage.value = try {
            home()
        } catch (e: StorageException) {
            Stage.StateError
        }
    }

    /** Reintento del reanclaje del reloj desde [Stage.RecoveryError]. */
    fun onRetryRecovery() = op { resolve() }

    /** "Començar de nou" / reintento desde el estado ilegible. */
    fun onRecover() = op {
        val active = try {
            deps.store.active()
        } catch (e: StorageException) {
            null
        }
        if (active != null) runCatching { deps.store.abandon(active.id) }
        // Filas ilegibles (corrupción): se marcan terminales para que Home sea alcanzable (sin bucle).
        runCatching { deps.store.quarantineUnreadable() }
        resolve()
    }

    /** Botón/gesto Atrás del sistema. Devuelve `true` si se consume (no se retrocede de pregunta, 01 §1.3). */
    fun onBack(): Boolean {
        when (val st = _stage.value) {
            is Stage.Question -> {
                if (st.saveError != null || st.saving) return true
                if (st.dialog != null) onDialogDismiss() else { dialog = QuestionDialog.EXIT; publishQuestion() }
            }
            is Stage.Name -> onGoHome()
            is Stage.Rules -> if (!st.starting) onChangeName()
            is Stage.Review -> when {
                st.openIndex != null -> onReviewOpen(null)
                st.fromResult -> backToResult(st.sessionId)
                else -> onGoHome()
            }
            is Stage.Credential -> when {
                st.presentation -> onPresentation(false)
                st.fromResult -> backToResult(st.credential.sessionId)
                else -> onGoHome()
            }
            is Stage.Result -> onGoHome()
            is Stage.TimeUp, is Stage.Grading, Stage.Loading -> Unit
            is Stage.Home, Stage.Welcome, Stage.BuildError, Stage.StateError, Stage.RecoveryError -> return false
        }
        return true
    }

    private fun backToResult(sessionId: String) = op {
        val s = try {
            deps.store.session(sessionId)
        } catch (e: StorageException) {
            null
        } ?: return@op
        showResult(s)
    }

    // ------------------------------------------------------------------ utilidades

    private fun op(block: suspend () -> Unit) {
        viewModelScope.launch { lock.withLock { block() } }
    }

    /** Reintenta un par de veces con espera corta (03 §4.3); solo errores de almacenamiento. */
    private suspend fun <T> withRetry(block: suspend () -> T): Result<T> {
        var failure: StorageException? = null
        repeat(RETRIES) { attempt ->
            try {
                return Result.success(block())
            } catch (e: StorageException) {
                failure = e
                if (attempt < RETRIES - 1) delay(RETRY_DELAY_MS)
            }
        }
        return Result.failure(failure ?: StorageException("sense detalls"))
    }

    private fun NameError.toProblem(): NameProblem = when (this) {
        NameError.EMPTY -> NameProblem.EMPTY
        NameError.TOO_LONG -> NameProblem.TOO_LONG
        else -> NameProblem.INVALID_CHARS
    }

    override fun onCleared() {
        stopTick()
    }

    companion object {
        const val RETRIES = 3
        const val RETRY_DELAY_MS = 200L
        const val MIN_GRADING_MS = 400L
    }
}
