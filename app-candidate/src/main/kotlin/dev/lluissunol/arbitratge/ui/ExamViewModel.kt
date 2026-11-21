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

    // Estado de pantalla del examen (nada de esto es verdad persistida salvo `current`).
    private var current: StoredSession? = null
    /** Última pregunta visitada (se persiste como `current_index`). */
    private var position = 0
    private var overviewOpen = false
    private var selected: Set<String> = emptySet()
    /** Verdadero desde que se decide el fin por tiempo: los taps nuevos se rechazan de forma síncrona. */
    private var timingOut = false
    /** Última selección tocada y aún no confirmada como guardada (el último toque gana). */
    private var pending: PendingTap? = null
    /** Restante capturado al confirmar "Finalitzar" (se reutiliza en el reintento: no se recaptura). */
    private var finishTap: Long? = null
    private val pendingTapMs: Long? get() = pending?.remainingMs ?: finishTap
    private var timeHidden = false
    private var dismissedBanner: TimeBanner? = null
    private var shownBanner: TimeBanner? = null
    private var dialog: QuestionDialog? = null
    private var saveError: SaveError? = null
    private var finishing = false
    /** Navegación en curso (se marca de forma SÍNCRONA antes de guardar la posición): no se aceptan toques ni más navegación. */
    private var navigating = false

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
        if (ExamClockModel.shouldTimeOut(ExamClockModel.remaining(sess.clock, deps.clock.monotonicMs()))) finishTimedOut(away = true)
        else enterQuestion(sess.currentIndex)
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
        enterQuestion(0)
    }

    // ------------------------------------------------------------------ C4 / C4b (navegación libre)

    private fun enterQuestion(index: Int) {
        resetQuestionState()
        val s = current ?: return
        position = index.coerceIn(0, s.session.total - 1)
        selected = selectionOf(s, position)
        publish()
        startTick()
    }

    private fun resetQuestionState() {
        selected = emptySet()
        pending = null
        finishTap = null
        dialog = null
        saveError = null
        finishing = false
        navigating = false
        overviewOpen = false
        dismissedBanner = null
        shownBanner = null
        timingOut = false
        ticks = 0
    }

    private fun selectionOf(s: StoredSession, index: Int): Set<String> = s.session.answerFor(index)?.selectedIds?.toSet().orEmpty()

    private fun remainingNow(s: StoredSession): Long = ExamClockModel.remaining(s.clock, deps.clock.monotonicMs())

    private fun isExamStage(): Boolean = _stage.value.let { it is Stage.Question || it is Stage.Overview }

    private fun publish() {
        val s = current ?: return
        val total = s.session.total
        val q = s.session.snapshot.questions.getOrNull(position) ?: return
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
        shownBanner = banner
        // Las respuestas contadas incluyen la selección visible (aunque su guardado esté en vuelo).
        val persisted = s.session.answeredIndices
        val answered = if (selected.isEmpty()) persisted - position else persisted + position
        _stage.value = if (overviewOpen) {
            Stage.Overview(
                items = s.session.snapshot.questions.mapIndexed { i, sq -> OverviewItem(i, sq.text, i in answered) },
                current = position, remainingMs = rem, timeHidden = timeHidden, banner = banner,
                dialog = dialog, saveError = saveError, finishing = finishing, level = level,
            )
        } else {
            Stage.Question(
                position, total, q, selected, answered, rem, timeHidden, banner, dialog,
                saving = pending != null && saveError == null, saveError = saveError, level = level,
                committed = selectionOf(s, position),
            )
        }
    }

    /**
     * Selección (radio o casilla): se guarda al INSTANTE (upsert). Se acepta aunque haya un guardado en vuelo (el último
     * toque gana). El restante se captura síncronamente al tocar (arbitraje con el fin por tiempo, 03 §4.3).
     */
    fun onSelect(optionId: String) {
        val st = _stage.value as? Stage.Question ?: return
        val s = current ?: return
        if (timingOut || navigating || st.saveError != null || st.dialog != null || finishing) return
        if (optionId !in st.question.options.map { it.id }) return
        val next = if (st.question.type == QuestionType.MULTIPLE_CHOICE) {
            if (optionId in selected) selected - optionId else selected + optionId
        } else setOf(optionId)
        if (next == selected) return
        val tap = remainingNow(s)
        if (tap < 0) { // el tiempo ya se había agotado al tocar: la respuesta no cuenta
            // Mismo arbitraje que el tick: con un guardado pendiente tocado A TIEMPO (en vuelo o fallido) el toque tardío
            // se ignora, se conserva el reintento y el fin por tiempo espera a que se resuelva.
            if (pending != null) return
            timingOut = true
            op {
                if (pending != null) timingOut = false // llegó un guardado pendiente a tiempo: el fin espera
                else if (isExamStage()) finishTimedOut(away = false)
            }
            return
        }
        selected = next
        pending = PendingTap(position, next, tap)
        publish()
        op { persistPending() }
    }

    fun onToggleTimeHidden() {
        timeHidden = !timeHidden
        publish()
    }

    fun onDismissBanner() {
        if (!isExamStage()) return
        dismissedBanner = shownBanner
        publish()
    }

    /**
     * Arranca una navegación: se marca [navigating] de forma SÍNCRONA (antes de cualquier suspensión) para que ningún
     * toque ni otra navegación se cuele mientras se guarda la posición. La selección pendiente ya está en cola (FIFO del
     * mutex) y se guarda ANTES; si falla, [canNavigate] la bloquea y se muestra el diálogo de error.
     */
    private fun nav(block: suspend () -> Unit) {
        if (navigating || timingOut || finishing || saveError != null || dialog != null || !isExamStage()) return
        navigating = true
        viewModelScope.launch {
            lock.withLock {
                try {
                    block()
                } finally {
                    navigating = false
                    if (isExamStage()) publish()
                }
            }
        }
    }

    /** Botones ← Anterior / Següent → (en la última: "Revisar i finalitzar" = resumen). */
    fun onPrevious() = nav { if (position > 0) goTo(position - 1) }

    fun onNext() = nav { if (position < (current?.session?.total ?: 0) - 1) goTo(position + 1) else openOverview() }

    /** "Veure-les totes" y "Revisar i finalitzar". */
    fun onOpenOverview() = nav { openOverview() }

    /** Fila del resumen / "Anar a la primera sense resposta". */
    fun onGoToQuestion(index: Int) = nav { goTo(index) }

    fun onGoToFirstUnanswered() = nav {
        (_stage.value as? Stage.Overview)?.firstUnanswered?.let { goTo(it) }
    }

    /** ← del resumen: vuelve a la pregunta de origen. */
    fun onBackToQuestion() = nav { goTo(position) }

    /** No se navega con una selección sin guardar, un fallo de guardado, un diálogo o el cierre en curso (B3). */
    private fun canNavigate(): Boolean =
        isExamStage() && !timingOut && pending == null && saveError == null && !finishing && dialog == null

    private suspend fun goTo(index: Int) {
        val s = current ?: return
        if (!canNavigate() || index !in 0 until s.session.total) return
        if (index == position && !overviewOpen) return
        persistPosition(index) // primero se guarda la posición (best effort) y luego se cambia de pantalla
        if (!canNavigate()) return // algo cambió mientras se guardaba (diálogo, fallo, fin por tiempo): no se pinta
        position = index
        selected = selectionOf(current ?: s, index)
        overviewOpen = false
        publish()
    }

    private suspend fun openOverview() {
        if (!canNavigate() || overviewOpen) return
        persistPosition(position)
        if (!canNavigate()) return
        overviewOpen = true
        publish()
    }

    /**
     * La selección ya está guardada (cada toque se guarda); la posición (última pregunta visitada) es un dato
     * secundario: se guarda ANTES de cambiar de pantalla, pero un fallo de almacenamiento no bloquea la navegación.
     */
    private suspend fun persistPosition(target: Int) {
        val s = current ?: return
        if (s.state != SessionState.IN_PROGRESS) return
        val c = deps.clock
        val next = ExamClockModel.checkpoint(s.clock, c.monotonicMs(), c.bootCount(), c.wallMs())
        try {
            deps.store.saveProgress(s.id, target, next)
            current = s.copy(currentIndex = target, clock = next)
        } catch (_: StorageException) {
        } catch (_: SessionNotActiveException) {
            resolve()
        }
    }

    /** "Finalitzar l'examen" (resumen): abre la confirmación con el recuento de preguntas sin respuesta. */
    fun onFinishRequested() {
        val st = _stage.value as? Stage.Overview ?: return
        if (st.saveError != null || st.dialog != null || finishing || navigating || pending != null) return
        dialog = QuestionDialog.FINISH
        publish()
    }

    fun onDialogConfirm() {
        when (dialog) {
            QuestionDialog.FINISH -> startFinish()
            QuestionDialog.EXIT -> {
                dialog = null
                publish()
                checkpointNow()
                _events.trySend(UiEvent.LeaveApp)
            }
            null -> Unit
        }
    }

    /** Botón seguro de los diálogos: "Tornar a l'examen" / "Continuar l'examen". */
    fun onDialogDismiss() {
        dialog = null
        publish()
    }

    private fun startFinish() {
        val st = _stage.value as? Stage.Overview ?: return
        val s = current ?: return
        if (st.saveError != null || finishing || navigating || pending != null || timingOut) return
        val tap = remainingNow(s)
        dialog = null
        if (tap < 0) { // se agotó el tiempo antes de confirmar: cuenta como fin por tiempo
            timingOut = true
            op { if (isExamStage()) finishTimedOut(away = false) }
            return
        }
        finishTap = tap
        finishing = true
        publish()
        op { doFinish() }
    }

    /** FINISHED(COMPLETED) + correct_count en UNA transacción, tras la confirmación explícita. */
    private suspend fun doFinish() {
        val s = current ?: return
        val tap = finishTap ?: return
        val c = deps.clock
        val limit = s.clock.limitMs
        val wall = c.wallMs()
        val clock = ClockState(limit, tap.coerceIn(0L, limit), c.monotonicMs(), c.bootCount(), wall)
        val r = ExamEngine.grade(s.session, limit - clock.remainingMsSaved)
        val finish = Finish(FinishReason.COMPLETED, r.correct, wall)
        val outcome = try {
            withRetry { deps.store.finish(s.id, clock, finish) }
        } catch (e: SessionNotActiveException) {
            resolve()
            return
        }
        if (outcome.isFailure) {
            finishing = false
            saveError = SaveError.RESULT
            publish()
            return
        }
        stopTick()
        finishing = false
        finishTap = null
        val done = s.copy(
            state = SessionState.FINISHED, clock = clock, finishReason = finish.reason,
            correctCount = finish.correctCount, finishedWall = finish.finishedWall,
        )
        current = done
        gradingThenResult(done)
    }

    /** Selección tocada y su restante al tocar. */
    private data class PendingTap(val index: Int, val ids: Set<String>, val remainingMs: Long)

    /** Guarda la ÚLTIMA selección pendiente (los toques intermedios se fusionan). Los fallos dejan `pending` intacto. */
    private suspend fun persistPending() {
        val p = pending ?: return
        if (saveError != null) return
        val s = current
        if (s == null || p.index != position) { // obsoleto
            pending = null
            return
        }
        val outcome = try {
            withRetry { persistAnswer(s, p) }
        } catch (e: SessionNotActiveException) {
            resolve()
            return
        } catch (e: ExamEngineException) {
            _stage.value = Stage.StateError
            return
        }
        val result = outcome.getOrNull()
        if (result == null) {
            saveError = SaveError.ANSWER
            publish()
            return
        }
        current = result
        if (pending === p) pending = null
        publish()
    }

    private suspend fun persistAnswer(s: StoredSession, p: PendingTap): StoredSession {
        val newSession = ExamEngine.answer(s.session, p.index, p.ids, p.remainingMs)
        val c = deps.clock
        val clock = ExamClockModel.checkpoint(s.clock, c.monotonicMs(), c.bootCount(), c.wallMs())
        deps.store.commitAnswer(s.id, newSession.answerFor(p.index)!!, p.index, clock)
        return s.copy(session = newSession, currentIndex = p.index, clock = clock)
    }

    /** "Tornar-ho a provar" tras un fallo de guardado: reintenta SIN recapturar el tiempo de la pulsación. */
    fun onRetrySave() {
        val err = saveError ?: return
        saveError = null
        when (err) {
            SaveError.ANSWER -> {
                publish()
                op { persistPending() }
            }
            SaveError.RESULT -> {
                if (finishTap == null) return
                finishing = true
                publish()
                op { doFinish() }
            }
        }
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
        if (!isExamStage() || lock.isLocked) return
        val rem = remainingNow(s)
        if (!timingOut && ExamClockModel.shouldTimeOut(rem, pendingTapMs)) {
            // Arbitraje: desde este instante (síncrono, mismo hilo que los taps) todo tap nuevo se rechaza.
            // Un tap ya capturado (`pending`) impide llegar aquí: el fin por tiempo espera a que se guarde.
            timingOut = true
            // En otra corrutina: finishTimedOut cancela el bucle de ticks (que es quien nos llama).
            viewModelScope.launch {
                lock.withLock {
                    if (isExamStage() && current != null && pending == null && !finishing) finishTimedOut(away = false)
                    else timingOut = false
                }
            }
            return
        }
        publish()
        if (++ticks % 30 == 0) checkpointNow()
    }

    /** Punto de control del reloj (`onStop`, cada ~30 s). Un fallo no interrumpe el examen. */
    private fun checkpointNow() {
        val s = current ?: return
        if (!isExamStage() || pending != null) return
        viewModelScope.launch {
            lock.withLock {
                val cur = current ?: return@withLock
                if (cur.state != SessionState.IN_PROGRESS || pending != null) return@withLock
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
        _stage.value = Stage.TimeUp(s.session.answeredCount, s.session.total, away)
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

    /**
     * Botón/gesto Atrás del sistema. Devuelve `true` si se consume. En la pregunta abre "Vols sortir?" (no retrocede de
     * pregunta: para eso está "Anterior"); en el resumen vuelve a la pregunta de origen; un diálogo se cierra primero.
     */
    fun onBack(): Boolean {
        when (val st = _stage.value) {
            is Stage.Question -> {
                if (st.saveError != null || st.saving || navigating) return true // Atrás se consume durante un guardado/navegación
                if (st.dialog != null) onDialogDismiss() else { dialog = QuestionDialog.EXIT; publish() }
            }
            is Stage.Overview -> {
                if (st.saveError != null || st.finishing || navigating) return true
                if (st.dialog != null) onDialogDismiss() else onBackToQuestion()
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
