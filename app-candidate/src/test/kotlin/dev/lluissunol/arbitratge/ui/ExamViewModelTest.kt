package dev.lluissunol.arbitratge.ui

import androidx.lifecycle.viewModelScope
import dev.lluissunol.arbitratge.credential.CredentialVerifier
import dev.lluissunol.arbitratge.credential.TrustedKeys
import dev.lluissunol.arbitratge.credential.VerificationCode
import dev.lluissunol.arbitratge.data.StoredCredential
import dev.lluissunol.arbitratge.exam.FinishReason
import dev.lluissunol.arbitratge.exam.QuestionType
import dev.lluissunol.arbitratge.exam.SessionState
import dev.lluissunol.arbitratge.support.FakeClock
import dev.lluissunol.arbitratge.support.FakePrefs
import dev.lluissunol.arbitratge.support.InMemoryStore
import dev.lluissunol.arbitratge.support.Op
import dev.lluissunol.arbitratge.support.SwitchableIssuing
import dev.lluissunol.arbitratge.support.TestEnv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExamViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val vms = mutableListOf<ExamViewModel>()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() {
        vms.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    private fun vmOf(store: InMemoryStore, clock: FakeClock, issuing: SwitchableIssuing = SwitchableIssuing(TestEnv.realIssuing), prefs: FakePrefs = FakePrefs()) =
        ExamViewModel(TestEnv.deps(store, clock, issuing, prefs)).also { vms += it }

    /** Cancela los ViewModels (bucle de ticks infinito) antes de que runTest drene el planificador. */
    private fun testBody(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try {
            block()
        } finally {
            vms.forEach { it.viewModelScope.cancel() }
        }
    }

    private fun TestScope.settle(ms: Long = 1000) {
        advanceTimeBy(ms)
        runCurrent()
    }

    private val ExamViewModel.q get() = stage.value as Stage.Question

    private fun TestScope.begin(vm: ExamViewModel, name: String = "  Maria   Puig Vila ") {
        settle()
        vm.onStartExam(); settle()
        vm.onNameChange(name); vm.onNameContinue()
        vm.onBeginExam(); settle()
    }

    private fun wrongPick(q: Stage.Question): Set<String> =
        setOf(q.question.options.first { it.id !in q.question.correctIds }.id)

    /** Responde la pregunta actual (bien o mal) y confirma si es la última. */
    private fun TestScope.answer(vm: ExamViewModel, right: Boolean) {
        val q = vm.q
        val ids = if (right) q.question.correctIds.toSet() else wrongPick(q)
        ids.forEach(vm::onSelect)
        vm.onNext(); settle(50)
        if (q.isLast) { vm.onDialogConfirm(); settle(50) }
        settle()
    }

    private fun TestScope.playAll(vm: ExamViewModel, rights: List<Boolean>) = rights.forEach { answer(vm, it) }

    // ------------------------------------------------------------------ flujo completo

    @Test fun firstRunShowsWelcomeThenHome() = testBody {
        val vm = vmOf(InMemoryStore(), FakeClock(), prefs = FakePrefs(welcomeSeen = false))
        settle()
        assertEquals(Stage.Welcome, vm.stage.value)
        vm.onWelcomeUnderstood(); settle()
        val home = assertIs<Stage.Home>(vm.stage.value)
        assertEquals(5, home.rules.questions)
        assertNull(home.credential)
    }

    @Test fun nameIsNormalizedAndConfirmedBeforeStart() = testBody {
        val store = InMemoryStore()
        val vm = vmOf(store, FakeClock())
        settle(); vm.onStartExam(); settle()
        // NFD + espacios múltiples + NBSP (02 §3.7.5.1)
        vm.onNameChange("Mar\u0069\u0301a\u00A0 Puig   Vila"); vm.onNameContinue()
        val rules = assertIs<Stage.Rules>(vm.stage.value)
        assertEquals("Mar\u00EDa Puig Vila", rules.nom)
        assertTrue(store.sessions.isEmpty(), "no hay sesión hasta pulsar «Començar ara»")
        vm.onChangeName()
        assertEquals("Mar\u00EDa Puig Vila", assertIs<Stage.Name>(vm.stage.value).draft)
        vm.onNameChange("   "); vm.onNameContinue()
        assertEquals(NameProblem.EMPTY, assertIs<Stage.Name>(vm.stage.value).problem)
        vm.onNameChange("A"); vm.onNameContinue()
        assertEquals(NameProblem.EMPTY, assertIs<Stage.Name>(vm.stage.value).problem)
        vm.onNameChange("x".repeat(81)); vm.onNameContinue()
        assertEquals(NameProblem.TOO_LONG, assertIs<Stage.Name>(vm.stage.value).problem)
        vm.onNameChange("Ana\u200Bx"); vm.onNameContinue()
        assertEquals(NameProblem.INVALID_CHARS, assertIs<Stage.Name>(vm.stage.value).problem)
    }

    @Test fun passingFlowIssuesVerifiableCredential() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        val session = store.sessions.values.single()
        assertEquals("Maria Puig Vila", session.candidateName)
        assertEquals(SessionState.IN_PROGRESS, session.state)
        playAll(vm, listOf(true, true, true, false, true))
        val result = assertIs<Stage.Result>(vm.stage.value)
        assertTrue(result.result.passed)
        assertEquals(4, result.result.correct)
        val ready = assertIs<CredentialPhase.Ready>(result.credential).credential
        // persistido: sesión FINISHED(COMPLETED) con correct_count y credencial con el cid reservado
        val fin = store.sessions.getValue(session.id)
        assertEquals(SessionState.FINISHED, fin.state)
        assertEquals(FinishReason.COMPLETED, fin.finishReason)
        assertEquals(4, fin.correctCount)
        assertEquals(session.cid, ready.cid)
        assertEquals("Maria Puig Vila", ready.name)
        assertEquals("dev", ready.kid)
        assertEquals("sample-1", ready.exv)
        assertEquals("0.0.0-sample", ready.bv)
        assertEquals(1, store.credentials.size)
        // el token exacto guardado verifica con la entrada de confianza de `dev`
        val trusted = TrustedKeys.of(listOf(TrustedKeys.parseEntry(TestEnv.devTrustedJson, "dev").getOrThrow()))
        val v = CredentialVerifier(trusted).verify(ready.token)
        assertEquals(VerificationCode.OK, v.code)
        assertEquals(ready.cid, v.credential!!.cid)
        assertEquals(ready.iat, v.credential!!.iat)
        assertEquals("2027-01-15", ready.iat) // fecha local del dispositivo (Europe/Madrid)
        // Home posterior muestra la credencial
        vm.onGoHome(); settle()
        assertEquals(ready, assertIs<Stage.Home>(vm.stage.value).credential)
    }

    @Test fun failingExamIssuesNothing() = testBody {
        val store = InMemoryStore()
        val issuing = SwitchableIssuing(TestEnv.realIssuing)
        val vm = vmOf(store, FakeClock(), issuing)
        begin(vm)
        playAll(vm, listOf(true, true, false, false, false))
        val r = assertIs<Stage.Result>(vm.stage.value)
        assertTrue(!r.result.passed)
        assertNull(r.credential)
        assertTrue(store.credentials.isEmpty())
        assertTrue(issuing.cids.isEmpty())
        assertEquals(3, r.result.failures)
    }

    // ------------------------------------------------------------------ persistencia (B3)

    @Test fun answerSaveFailureKeepsUserOnQuestionWithSelection() = testBody {
        val store = InMemoryStore()
        val vm = vmOf(store, FakeClock())
        begin(vm)
        val first = vm.q
        val pick = first.question.correctIds.toSet()
        pick.forEach(vm::onSelect)
        store.failAlways(Op.COMMIT_ANSWER)
        vm.onNext(); settle(1000)
        val st = vm.q
        assertEquals(SaveError.ANSWER, st.saveError)
        assertEquals(0, st.index, "no se avanza")
        assertEquals(pick, st.selected, "la selección sigue visible")
        assertTrue(store.sessions.values.single().session.answers.isEmpty(), "nada guardado")
        assertEquals(ExamViewModel.RETRIES, store.count(Op.COMMIT_ANSWER), "reintentos automáticos acotados")
        // la selección queda fijada mientras no se guarde
        vm.onSelect(first.question.options.first { it.id !in pick }.id)
        assertEquals(pick, vm.q.selected)
        // reintento manual con el almacén sano: avanza, sin duplicar filas
        store.healAll()
        vm.onRetrySave(); settle()
        val after = vm.q
        assertEquals(1, after.index)
        assertNull(after.saveError)
        assertEquals(1, store.sessions.values.single().session.answers.size)
    }

    @Test fun lastAnswerSaveFailureDoesNotGradeNorIssue() = testBody {
        val store = InMemoryStore()
        val vm = vmOf(store, FakeClock())
        begin(vm)
        playAll(vm, listOf(true, true, true, true))
        store.failAlways(Op.COMMIT_ANSWER)
        val q = vm.q
        q.question.correctIds.forEach(vm::onSelect)
        vm.onNext(); settle(50)
        vm.onDialogConfirm(); settle(2000)
        assertEquals(SaveError.RESULT, vm.q.saveError)
        assertEquals(SessionState.IN_PROGRESS, store.sessions.values.single().state)
        assertTrue(store.credentials.isEmpty())
        store.healAll()
        vm.onRetrySave(); settle(2000)
        assertIs<Stage.Result>(vm.stage.value)
        assertEquals(1, store.credentials.size)
    }

    @Test fun startFailureStaysOnRulesWithRetry() = testBody {
        val store = InMemoryStore()
        val vm = vmOf(store, FakeClock())
        settle(); vm.onStartExam(); settle()
        vm.onNameChange("Maria Puig"); vm.onNameContinue()
        store.failAlways(Op.CREATE)
        vm.onBeginExam(); settle(2000)
        val st = assertIs<Stage.Rules>(vm.stage.value)
        assertTrue(st.saveError && !st.starting)
        assertTrue(store.sessions.isEmpty())
        store.healAll()
        vm.onBeginExam(); settle()
        assertIs<Stage.Question>(vm.stage.value)
        assertEquals(1, store.sessions.size)
    }

    @Test fun abandonOnlyWhenSaved() = testBody {
        val store = InMemoryStore()
        val vm = vmOf(store, FakeClock())
        begin(vm)
        vm.onSelect(vm.q.question.options.first().id)
        store.failAlways(Op.COMMIT_ANSWER)
        vm.onNext(); settle(2000)
        store.failAlways(Op.ABANDON)
        vm.onAbandon(); settle(2000)
        assertIs<Stage.Question>(vm.stage.value) // ni abandona ni cambia de estado
        assertEquals(SessionState.IN_PROGRESS, store.sessions.values.single().state)
        store.healAll()
        vm.onAbandon(); settle()
        assertIs<Stage.Home>(vm.stage.value)
        assertEquals(SessionState.ABANDONED, store.sessions.values.single().state)
        assertTrue(store.credentials.isEmpty())
    }

    @Test fun noAnswerNeedsConfirmationAndCountsAsFailure() = testBody {
        val store = InMemoryStore()
        val vm = vmOf(store, FakeClock())
        begin(vm)
        vm.onNext(); settle(50)
        assertEquals(QuestionDialog.NO_ANSWER, vm.q.dialog)
        assertTrue(store.sessions.values.single().session.answers.isEmpty())
        vm.onDialogDismiss()
        assertNull(vm.q.dialog)
        vm.onNext(); vm.onDialogConfirm(); settle()
        assertEquals(1, vm.q.index)
        assertEquals(emptyList(), store.sessions.values.single().session.answers.single().selectedIds)
    }

    @Test fun backAsksToExitInsteadOfGoingBack() = testBody {
        val vm = vmOf(InMemoryStore(), FakeClock())
        begin(vm)
        assertTrue(vm.onBack())
        assertEquals(QuestionDialog.EXIT, vm.q.dialog)
        assertTrue(vm.onBack()) // cierra el diálogo
        assertNull(vm.q.dialog)
    }

    @Test fun selectionRulesPerType() = testBody {
        val vm = vmOf(InMemoryStore(), FakeClock())
        begin(vm)
        // recorrer hasta encontrar una múltiple para comprobar que admite varias y las simples solo una
        var sawMultiple = false
        repeat(5) {
            val q = vm.q
            val ids = q.question.options.map { it.id }
            ids.take(2).forEach(vm::onSelect)
            if (q.question.type == QuestionType.MULTIPLE_CHOICE) {
                sawMultiple = true
                assertEquals(2, vm.q.selected.size)
                vm.onSelect(ids[0]); assertEquals(1, vm.q.selected.size)
                vm.onSelect(ids[0])
            } else assertEquals(setOf(ids[1]), vm.q.selected)
            vm.onNext(); settle(50)
            if (q.isLast) { vm.onDialogConfirm() }
            settle()
        }
        assertTrue(sawMultiple)
    }

    // ------------------------------------------------------------------ reloj y fin por tiempo

    @Test fun timeoutFinishesAndPersistsCorrectCount() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        answer(vm, true); answer(vm, true); answer(vm, true)
        clock.advance(5 * 60_000L + 1)
        settle(1000) // el siguiente tick detecta el fin
        val up = assertIs<Stage.TimeUp>(vm.stage.value)
        assertEquals(3, up.answered)
        assertTrue(!up.away)
        val s = store.sessions.values.single()
        assertEquals(SessionState.FINISHED, s.state)
        assertEquals(FinishReason.TIMED_OUT, s.finishReason)
        assertEquals(3, s.correctCount)
        vm.onSeeResult(); settle(1000)
        val r = assertIs<Stage.Result>(vm.stage.value)
        assertEquals(5 * 60_000L, r.result.elapsedMs)
        assertTrue(r.result.passed) // 3 >= umbral 3: también emite si aprueba por tiempo
        assertIs<CredentialPhase.Ready>(r.credential)
    }

    @Test fun pendingTapWithTimeLeftIsNotTimedOutWhileSaveFails() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        vm.q.question.correctIds.forEach(vm::onSelect)
        store.failAlways(Op.COMMIT_ANSWER)
        vm.onNext(); settle(1000)
        assertEquals(SaveError.ANSWER, vm.q.saveError)
        clock.advance(10 * 60_000L) // se agota mientras el guardado falla
        settle(2000)
        assertIs<Stage.Question>(vm.stage.value) // la respuesta tocada a tiempo no se pierde por el almacenamiento
        store.healAll()
        vm.onRetrySave(); settle(100)
        assertEquals(1, store.sessions.values.single().session.answers.size)
        assertEquals(1, vm.q.index) // se guardó y recién entonces el tick finalizará por tiempo
        settle(1000)
        assertIs<Stage.TimeUp>(vm.stage.value)
    }

    @Test fun tapAfterExpiryIsRejectedAndTimesOut() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        vm.q.question.correctIds.forEach(vm::onSelect)
        clock.advance(6 * 60_000L)
        vm.onNext(); settle(50) // antes de que llegue el tick
        assertIs<Stage.TimeUp>(vm.stage.value)
        assertTrue(store.sessions.values.single().session.answers.isEmpty())
    }

    @Test fun timeoutFinishFailureShowsGradingErrorAndRetries() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        store.failAlways(Op.FINISH)
        clock.advance(6 * 60_000L)
        settle(3000)
        assertEquals(Stage.Grading(error = true), vm.stage.value)
        assertEquals(SessionState.IN_PROGRESS, store.sessions.values.single().state)
        store.healAll()
        vm.onRetryGrading(); settle(1000)
        assertIs<Stage.TimeUp>(vm.stage.value)
        assertEquals(SessionState.FINISHED, store.sessions.values.single().state)
    }

    // ------------------------------------------------------------------ proceso muerto / reanudación

    @Test fun processDeathResumesAtPersistedQuestion() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        answer(vm, true); answer(vm, false)
        val marked = vm.q.question.correctIds.first()
        vm.onSelect(marked) // marcada pero NO guardada: se pierde al morir el proceso
        vm.viewModelScope.cancel()
        clock.advance(60_000)
        val vm2 = vmOf(store, clock) // nuevo proceso: todo se recompone desde el almacén
        settle()
        val q = vm2.q
        assertEquals(2, q.index)
        assertTrue(q.selected.isEmpty())
        assertTrue(q.remainingMs in 1..(5 * 60_000L - 60_000L))
    }

    @Test fun rebootReanchorsKeepingRemainingTime() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock(mono = 5_000_000L, boot = 7)
        val vm = vmOf(store, clock)
        begin(vm)
        answer(vm, true)
        val saved = store.sessions.values.single().clock
        vm.viewModelScope.cancel()
        // reinicio: nuevo arranque, monotónico reiniciado cerca de 0; el tiempo apagado no cuenta
        clock.mono = 2_000L; clock.boot = 8; clock.wall += 3_600_000L
        val vm2 = vmOf(store, clock)
        settle()
        assertEquals(saved.remainingMsSaved, vm2.q.remainingMs)
        val re = store.sessions.values.single().clock
        assertEquals(2_000L, re.anchorMono)
        assertEquals(8, re.anchorBoot)
        assertEquals(saved.remainingMsSaved, re.remainingMsSaved)
        // idempotente: repetir la recuperación no cambia nada
        vm2.viewModelScope.cancel()
        val vm3 = vmOf(store, clock)
        settle()
        assertEquals(saved.remainingMsSaved, vm3.q.remainingMs)
    }

    @Test fun resumeAfterDeadlineShowsTimeUpAway() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        answer(vm, true)
        vm.viewModelScope.cancel()
        clock.advance(30 * 60_000L)
        val vm2 = vmOf(store, clock)
        settle()
        val up = assertIs<Stage.TimeUp>(vm2.stage.value)
        assertTrue(up.away)
        assertEquals(1, up.answered)
        assertEquals(FinishReason.TIMED_OUT, store.sessions.values.single().finishReason)
    }

    // ------------------------------------------------------------------ emisión

    @Test fun issuanceFailureThenRetryIsIdempotent() = testBody {
        val store = InMemoryStore()
        val issuing = SwitchableIssuing(TestEnv.realIssuing).apply { mode = SwitchableIssuing.Mode.THROW }
        val vm = vmOf(store, FakeClock(), issuing)
        begin(vm)
        playAll(vm, listOf(true, true, true, true, true))
        val pending = assertIs<Stage.Result>(vm.stage.value)
        assertEquals(CredentialPhase.Pending, pending.credential)
        assertTrue(store.credentials.isEmpty(), "no queda credencial a medias")
        issuing.mode = SwitchableIssuing.Mode.OK
        vm.onRetryCredential(); settle()
        val ready = assertIs<CredentialPhase.Ready>(assertIs<Stage.Result>(vm.stage.value).credential).credential
        assertEquals(store.sessions.values.single().cid, ready.cid)
        assertEquals(1, store.credentials.size)
        assertEquals(setOf(ready.cid), issuing.cids.toSet(), "siempre el mismo cid reservado")
    }

    @Test fun credentialInsertFailureLeavesPendingThenRecoversOnReopen() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        store.failAlways(Op.INSERT_CREDENTIAL)
        playAll(vm, listOf(true, true, true, true, true))
        assertEquals(CredentialPhase.Pending, assertIs<Stage.Result>(vm.stage.value).credential)
        assertTrue(store.credentials.isEmpty())
        vm.viewModelScope.cancel()
        // recuperación: sesión FINISHED aprobada sin credencial → al reabrir se reintenta sola
        store.healAll()
        val vm2 = vmOf(store, clock)
        settle()
        val ready = assertIs<CredentialPhase.Ready>(assertIs<Stage.Result>(vm2.stage.value).credential).credential
        assertEquals("Maria Puig Vila", ready.name)
        assertEquals(store.sessions.values.single().cid, ready.cid)
        // reabrir otra vez no emite una segunda
        vm2.viewModelScope.cancel()
        val vm3 = vmOf(store, clock)
        settle()
        assertIs<Stage.Home>(vm3.stage.value)
        assertEquals(1, store.credentials.size)
    }

    @Test fun nameIsImmutableOnceStarted() = testBody {
        val store = InMemoryStore()
        val vm = vmOf(store, FakeClock())
        begin(vm, "Maria Puig Vila")
        val before = store.sessions.values.single().candidateName
        vm.onNameChange("Otra Persona")
        assertEquals(before, store.sessions.values.single().candidateName)
        assertNotNull(vm.q)
    }

    @Test fun buildErrorWhenBankInvalid() = testBody {
        val vm = ExamViewModel(
            TestEnv.deps(InMemoryStore(), FakeClock(), bank = Result.failure(IllegalStateException("x"))),
        ).also { vms += it }
        settle()
        assertEquals(Stage.BuildError, vm.stage.value)
    }

    @Test fun reviewShowsFailuresFirstWithCorrectAnswers() = testBody {
        val store = InMemoryStore()
        val vm = vmOf(store, FakeClock())
        begin(vm)
        playAll(vm, listOf(true, false, true, true, false))
        val sid = (vm.stage.value as Stage.Result).sessionId
        vm.onReview(sid, fromResult = true); settle()
        val rev = assertIs<Stage.Review>(vm.stage.value)
        assertTrue(rev.onlyWrong)
        assertEquals(2, rev.result.items.count { !it.isCorrect })
        assertTrue(rev.result.items.filter { !it.isCorrect }.all { it.correctIds.isNotEmpty() })
        vm.onBack(); settle()
        assertIs<Stage.Result>(vm.stage.value)
    }

    // ------------------------------------------------------------------ P0: captura síncrona del tap

    @Test fun tapBeforeDeadlineSurvivesCheckpointHoldingTheMutex() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        clock.advance(5 * 60_000L - 1_000L) // quedan 1000 ms
        vm.q.question.correctIds.forEach(vm::onSelect)
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        store.saveClockGate = gate
        vm.onAppStopped(); runCurrent() // punto de control lento: retiene el mutex
        vm.onNext(); runCurrent() // tap a tiempo, mientras el mutex está ocupado
        clock.advance(5_000L) // se cruza el plazo antes de que el guardado obtenga el mutex
        gate.complete(Unit)
        settle(100)
        val answers = store.sessions.values.single().session.answers
        assertEquals(1, answers.size, "la respuesta tocada a tiempo se acepta")
        assertTrue(answers.single().remainingAtTapMs in 0..1_000L)
        settle(1000)
        assertIs<Stage.TimeUp>(vm.stage.value)
    }

    @Test fun duplicateTapsDoNotCommitNextQuestionWithEmptySelection() = testBody {
        val store = InMemoryStore()
        val vm = vmOf(store, FakeClock())
        begin(vm)
        vm.q.question.correctIds.forEach(vm::onSelect)
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        store.saveClockGate = gate
        vm.onAppStopped(); runCurrent()
        vm.onNext(); vm.onNext(); vm.onNext() // doble/triple toque con el mutex ocupado
        vm.onDialogConfirm() // evento obsoleto
        runCurrent()
        gate.complete(Unit)
        settle(100)
        val sess = store.sessions.values.single()
        assertEquals(1, sess.session.answers.size, "una sola respuesta")
        assertEquals(1, vm.q.index)
        assertTrue(vm.q.selected.isEmpty())
        assertNull(vm.q.dialog, "la pregunta siguiente conserva su confirmación pendiente")
    }

    // ------------------------------------------------------------------ P1: recuperación de emisión

    private fun TestScope.secondExam(vm: ExamViewModel, rights: List<Boolean>) {
        vm.onGoHome(); settle()
        vm.onStartExam(); settle()
        vm.onNameChange("Maria Puig Vila"); vm.onNameContinue(); vm.onBeginExam(); settle()
        playAll(vm, rights)
    }

    @Test fun passedPendingSessionIsRecoveredAfterNewerExamAndClockChange() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        store.failAlways(Op.INSERT_CREDENTIAL)
        playAll(vm, listOf(true, true, true, true, true))
        assertEquals(CredentialPhase.Pending, assertIs<Stage.Result>(vm.stage.value).credential)
        store.healAll()
        clock.wall -= 10 * 24 * 3_600_000L // el reloj de pared retrocede: el 2.º examen "termina antes"
        secondExam(vm, listOf(false, false, false, false, false))
        assertTrue(store.credentials.isEmpty())
        vm.viewModelScope.cancel()
        val vm2 = vmOf(store, clock)
        settle()
        val ready = assertIs<CredentialPhase.Ready>(assertIs<Stage.Result>(vm2.stage.value).credential).credential
        assertEquals("session-1", ready.sessionId)
        assertEquals(1, store.credentials.size)
    }

    @Test fun everyPendingPassedSessionIsIssuedOnRelaunch() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock()
        val vm = vmOf(store, clock)
        begin(vm)
        store.failAlways(Op.INSERT_CREDENTIAL)
        playAll(vm, listOf(true, true, true, true, true))
        secondExam(vm, listOf(true, true, true, true, true))
        assertTrue(store.credentials.isEmpty())
        store.healAll()
        vm.viewModelScope.cancel()
        val vm2 = vmOf(store, clock)
        settle()
        assertIs<Stage.Result>(vm2.stage.value)
        assertEquals(setOf("session-1", "session-2"), store.credentials.values.map { it.sessionId }.toSet())
    }

    // ------------------------------------------------------------------ P1: reanclaje no persistido

    @Test fun rebootReanchorFailureBlocksResumeUntilPersisted() = testBody {
        val store = InMemoryStore()
        val clock = FakeClock(mono = 5_000_000L, boot = 7)
        val vm = vmOf(store, clock)
        begin(vm)
        answer(vm, true)
        val saved = store.sessions.values.single().clock
        vm.viewModelScope.cancel()
        clock.mono = 2_000L; clock.boot = 8
        store.failAlways(Op.SAVE_CLOCK)
        val vm2 = vmOf(store, clock)
        settle(2000)
        assertEquals(Stage.RecoveryError, vm2.stage.value)
        // muerte del proceso con el almacén aún fallando: sigue sin entrar y la BD conserva el ancla vieja
        vm2.viewModelScope.cancel()
        val vm3 = vmOf(store, clock)
        settle(2000)
        assertEquals(Stage.RecoveryError, vm3.stage.value)
        assertEquals(saved, store.sessions.values.single().clock)
        store.healAll()
        vm3.onRetryRecovery(); settle(100)
        assertEquals(saved.remainingMsSaved, vm3.q.remainingMs)
        assertEquals(8, store.sessions.values.single().clock.anchorBoot)
    }
}
