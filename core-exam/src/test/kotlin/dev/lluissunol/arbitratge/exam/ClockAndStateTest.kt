package dev.lluissunol.arbitratge.exam

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClockAndStateTest {
    private val limit = 30 * 60_000L
    private val W = 1_700_000_000_000
    private fun start() = ExamClockModel.initial(limit, nowMono = 1_000, boot = 7, nowWall = 1_700_000_000_000)

    @Test fun initialRemainingIsLimit() {
        val c = start()
        assertEquals(limit, c.remainingMsSaved)
        assertEquals(limit, ExamClockModel.remaining(c, 1_000))
        assertFailsWith<IllegalArgumentException> { ExamClockModel.initial(0, 0, null, 0) }
    }

    @Test fun remainingDecreasesFromSource() {
        val c = start()
        assertEquals(limit - 60_000, ExamClockModel.remaining(c, 61_000))
        assertEquals(limit - 60_000, ExamClockModel.remaining(c, 61_000)) // sin estado oculto
    }

    @Test fun checkpointWritesRemainingAndAnchorTogether() {
        val c = ExamClockModel.checkpoint(start(), 11_000, 7, 1_700_000_010_000)
        assertEquals(limit - 10_000, c.remainingMsSaved)
        assertEquals(11_000, c.anchorMono)
        assertEquals(limit - 10_000, ExamClockModel.remaining(c, 11_000))
    }

    @Test fun checkpointClampsToRange() {
        val late = ExamClockModel.checkpoint(start(), 1_000 + limit + 99_999, 7, 1_700_000_000_000 + limit + 99_999)
        assertEquals(0, late.remainingMsSaved)
        assertTrue(ExamClockModel.isExpired(ExamClockModel.remaining(late, late.anchorMono)))
    }

    @Test fun timeoutExactlyAtZero() {
        val c = start()
        assertTrue(ExamClockModel.isExpired(ExamClockModel.remaining(c, 1_000 + limit)))
        assertFalse(ExamClockModel.isExpired(ExamClockModel.remaining(c, 1_000 + limit - 1)))
    }

    @Test fun sameBootNoReanchor() {
        val r = ExamClockModel.reconcile(start(), 500_000, 7, 1_700_000_499_000)
        assertFalse(r.rebooted)
        assertEquals(start(), r.state)
    }

    @Test fun rebootReanchorsKeepingSavedRemaining() {
        val saved = ExamClockModel.checkpoint(start(), 101_000, 7, 1_700_000_100_000) // restante = limit − 100 s
        val r = ExamClockModel.reconcile(saved, nowMono = 5_000, nowBoot = 8, nowWall = 1_700_000_000_000)
        assertTrue(r.rebooted)
        assertEquals(saved.remainingMsSaved, r.state.remainingMsSaved)
        assertEquals(5_000, r.state.anchorMono)
        assertEquals(8, r.state.anchorBoot)
        assertEquals(saved.remainingMsSaved, ExamClockModel.remaining(r.state, 5_000)) // el apagado no cuenta
    }

    @Test fun reanchorIsIdempotentAndRepeatedRebootsDontAccumulateError() {
        var c = start()
        val first = ExamClockModel.reconcile(c, 100, 8, W)
        val again = ExamClockModel.reconcile(first.state, 100, 8, W)
        assertFalse(again.rebooted)
        assertEquals(first.state, again.state)
        repeat(5) { n -> c = ExamClockModel.reconcile(c, 50L + n, 8 + n, W).state }
        assertEquals(limit, c.remainingMsSaved)
    }

    @Test fun rebootBeforeFirstAnswerHasValidAnchor() {
        val r = ExamClockModel.reconcile(start(), 10, 9, W)
        assertTrue(r.rebooted)
        assertEquals(limit, ExamClockModel.remaining(r.state, 10))
    }

    @Test fun monotonicGoingBackwardsMeansReboot() {
        assertTrue(ExamClockModel.rebootDetected(start(), 500, 7, W)) // mismo boot_count pero mono < ancla
        assertTrue(ExamClockModel.rebootDetected(start(), 500, null, W))
    }

    @Test fun unreliableBootUsesWallAsHintOnly() {
        val c = start().copy(anchorBoot = null)
        // avance pared ≈ avance mono: sin reinicio
        assertFalse(ExamClockModel.rebootDetected(c, 61_000, null, 1_700_000_060_000))
        // pared salta 10 min más que mono: indicio de reinicio → se reancla (cobra 0)
        assertTrue(ExamClockModel.rebootDetected(c, 61_000, null, 1_700_000_000_000 + 60_000 + 600_000))
        // pared hacia atrás: no cobra de más, solo reancla
        assertTrue(ExamClockModel.rebootDetected(c, 61_000, null, 1_699_999_000_000))
        // dentro de la tolerancia
        assertFalse(ExamClockModel.rebootDetected(c, 61_000, null, 1_700_000_000_000 + 60_000 + 119_000))
    }

    @Test fun wallClockChangeWithReliableBootIsIgnored() {
        val r = ExamClockModel.reconcile(start(), 61_000, 7, 1_600_000_000_000)
        assertFalse(r.rebooted)
        assertEquals(limit - 60_000, ExamClockModel.remaining(r.state, 61_000))
    }

    @Test fun tapBeforeTimeoutSavedLaterIsValid() {
        val c = start()
        val tapMono = 1_000 + limit - 500          // 500 ms de margen al tocar
        val tapRemaining = ExamClockModel.remaining(c, tapMono)
        assertEquals(500, tapRemaining)
        val saveMono = tapMono + 3_000               // se guarda 3 s después, ya pasado el límite
        val nowRemaining = ExamClockModel.remaining(c, saveMono)
        assertTrue(nowRemaining < 0)
        assertTrue(ExamClockModel.tapAccepted(tapRemaining))
        assertFalse(ExamClockModel.shouldTimeOut(nowRemaining, pendingTapRemainingMs = tapRemaining))
        // y el motor la acepta
        val s = ExamEngine.start(TestBanks.bank(), TestBanks.rules(minutes = 30), SeededRng(1))
        assertEquals(1, ExamEngine.answer(s, 0, listOf("a"), tapRemaining).answers.size)
    }

    @Test fun tapAfterTimeoutIsRejectedAndTimesOut() {
        val c = start()
        val remaining = ExamClockModel.remaining(c, 1_000 + limit + 10)
        assertTrue(ExamClockModel.shouldTimeOut(remaining))
        assertTrue(ExamClockModel.shouldTimeOut(remaining, pendingTapRemainingMs = -10))
        assertFalse(ExamClockModel.tapAccepted(remaining))
        val s = ExamEngine.start(TestBanks.bank(), TestBanks.rules(), SeededRng(1))
        assertFailsWith<ExamEngineException> { ExamEngine.answer(s, 0, listOf("a"), remaining) }
        assertFalse(ExamClockModel.shouldTimeOut(1)) // aún queda tiempo
    }

    @Test fun wallAnchorIsAlwaysSet() {
        val c = start()
        assertEquals(W, c.anchorWallMs)
        val cp = ExamClockModel.checkpoint(c, 11_000, 7, W + 10_000)
        assertEquals(W + 10_000, cp.anchorWallMs)
        val rb = ExamClockModel.reconcile(cp, 2_000, 9, W + 500_000)
        assertTrue(rb.rebooted)
        assertEquals(W + 500_000, rb.state.anchorWallMs)
    }

    @Test fun rebootWithNullBootCountAndNewMonoAboveOldAnchorIsDetectedByWall() {
        val c = ExamClockModel.initial(limit, nowMono = 1_000, boot = null, nowWall = W)
        // reinicio: nuevo mono (5_000_000) > ancla vieja (1_000) pero la pared avanzó 1 h
        val r = ExamClockModel.reconcile(c, 5_000_000, null, W + 3_600_000)
        assertTrue(r.rebooted)
        assertEquals(limit, r.state.remainingMsSaved)
        assertEquals(5_000_000, r.state.anchorMono)
        assertEquals(W + 3_600_000, r.state.anchorWallMs)
        assertEquals(limit, ExamClockModel.remaining(r.state, 5_000_000))
        // tras reanclar con pared coherente no se vuelve a detectar
        assertFalse(ExamClockModel.reconcile(r.state, 5_060_000, null, W + 3_660_000).rebooted)
    }

    @Test fun elapsedFromClock() {
        val c = start()
        assertEquals(60_000, ExamClockModel.elapsedMs(c, 61_000))
        assertEquals(limit, ExamClockModel.elapsedMs(c, 1_000 + limit * 2))
    }

    // ---------- máquina de estados ----------
    private fun run(start: ExamPhase, vararg ev: ExamEvent): ExamPhase? =
        ev.fold(start as ExamPhase?) { p, e -> p?.let { ExamStateMachine.next(it, e) } }

    @Test fun happyPathToCredential() {
        val p = run(
            ExamPhase.Home, ExamEvent.OpenBriefing, ExamEvent.BriefingAccepted, ExamEvent.NameEntered,
            ExamEvent.SessionStarted,
            ExamEvent.AnswerCommitted(0, 2), ExamEvent.AnswerCommitted(1, 2),
        )
        assertEquals(ExamPhase.Finished(FinishReason.COMPLETED), p)
        assertEquals(ExamPhase.Credential, run(p!!, ExamEvent.ResultShown(true), ExamEvent.CredentialStored))
    }

    @Test fun failedResultReturnsHomeWithoutCredential() {
        val r = run(ExamPhase.Finished(FinishReason.TIMED_OUT), ExamEvent.ResultShown(false))
        assertEquals(ExamPhase.Result(false), r)
        assertEquals(ExamPhase.Home, run(r!!, ExamEvent.ReturnHome))
        assertNull(run(r, ExamEvent.CredentialStored))
    }

    @Test fun saveFailureStaysInPlace() {
        val p = ExamPhase.InProgress(3)
        assertEquals(p, ExamStateMachine.next(p, ExamEvent.SaveFailed))
        assertEquals(ExamPhase.Ready, ExamStateMachine.next(ExamPhase.Ready, ExamEvent.SaveFailed))
    }

    @Test fun invalidTransitionsAreRejected() {
        assertNull(ExamStateMachine.next(ExamPhase.Home, ExamEvent.SessionStarted))
        assertNull(ExamStateMachine.next(ExamPhase.InProgress(1), ExamEvent.AnswerCommitted(2, 5))) // salto
        assertNull(ExamStateMachine.next(ExamPhase.InProgress(1), ExamEvent.AnswerCommitted(0, 5))) // retroceso
        assertNull(ExamStateMachine.next(ExamPhase.Abandoned, ExamEvent.SessionStarted))
        assertNull(ExamStateMachine.next(ExamPhase.Finished(FinishReason.COMPLETED), ExamEvent.AbandonCommitted))
        assertEquals(ExamPhase.Identifying, ExamStateMachine.next(ExamPhase.Ready, ExamEvent.ChangeName))
    }

    @Test fun timeoutAndAbandonFromInProgress() {
        assertEquals(ExamPhase.Finished(FinishReason.TIMED_OUT), ExamStateMachine.next(ExamPhase.InProgress(4), ExamEvent.TimedOut))
        assertEquals(ExamPhase.Abandoned, ExamStateMachine.next(ExamPhase.InProgress(0), ExamEvent.AbandonCommitted))
        assertEquals(ExamPhase.Home, ExamStateMachine.next(ExamPhase.Abandoned, ExamEvent.ReturnHome))
    }

    @Test fun credentialOnlyForPersistedPassWithoutExisting() {
        fun can(
            state: SessionState = SessionState.FINISHED, reason: FinishReason? = FinishReason.COMPLETED,
            correct: Int? = 14, exists: Boolean = false, practice: Boolean = false,
        ) = ExamStateMachine.shouldIssueCredential(state, reason, correct, 14, exists, practice)
        assertTrue(can())
        assertTrue(can(reason = FinishReason.TIMED_OUT)) // aprobado con respondidas aunque se agote
        assertFalse(can(reason = null))
        assertFalse(can(correct = 13))
        assertFalse(can(correct = null))
        assertFalse(can(state = SessionState.IN_PROGRESS))
        assertFalse(can(state = SessionState.ABANDONED, reason = FinishReason.ABANDONED))
        assertFalse(can(exists = true))
        assertFalse(can(practice = true))
    }
}
