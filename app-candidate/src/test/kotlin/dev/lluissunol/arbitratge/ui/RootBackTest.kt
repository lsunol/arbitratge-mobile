package dev.lluissunol.arbitratge.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.lluissunol.arbitratge.data.SessionKind
import dev.lluissunol.arbitratge.data.StoredCredential
import dev.lluissunol.arbitratge.data.StoredSession
import dev.lluissunol.arbitratge.exam.AnswerRecord
import dev.lluissunol.arbitratge.exam.ClockState
import dev.lluissunol.arbitratge.exam.ExamEngine
import dev.lluissunol.arbitratge.exam.FinishReason
import dev.lluissunol.arbitratge.exam.SeededRng
import dev.lluissunol.arbitratge.exam.SessionState
import dev.lluissunol.arbitratge.support.FakeClock
import dev.lluissunol.arbitratge.support.InMemoryStore
import dev.lluissunol.arbitratge.support.TestEnv
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Atrás del sistema a través del `BackHandler` real de la raíz (04 §1.2): nunca cierra la app en pantallas internas. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
class RootBackTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun seeded(): InMemoryStore {
        val store = InMemoryStore()
        val session = ExamEngine.start(TestEnv.bank, TestEnv.bank.rules, SeededRng(1))
        val answered = session.copy(answers = session.snapshot.questions.mapIndexed { i, q -> AnswerRecord(i, q.correctIds, 100_000) })
        store.sessions["session-1"] = StoredSession(
            "session-1", SessionKind.EXAM, SessionState.FINISHED, "7f3a92c1-4b5d-4e6f-8a9b-000000000001", "Maria Puig Vila",
            answered, answered.total, ClockState(300_000, 100_000, 5, null, 6), 1L, 2L, FinishReason.COMPLETED, 5,
        )
        val c = StoredCredential(
            "7f3a92c1-4b5d-4e6f-8a9b-000000000001", "session-1", TestEnv.realIssuingToken(), "Maria Puig Vila",
            "2026-11-20", "dev", "sample-1", "0.0.0-sample",
        )
        store.credentials[c.cid] = c
        return store
    }

    private fun pressBack() {
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test fun backOnCredentialReturnsHomeInsteadOfExiting() {
        val vm = ExamViewModel(TestEnv.deps(seeded(), FakeClock()))
        rule.setContent { ArbitratgeRoot(vm) }
        rule.waitForIdle()
        rule.onNodeWithText("Veure la credencial").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Mostrar a un organitzador").assertIsDisplayed()
        pressBack()
        rule.onNodeWithText("Veure la credencial").assertIsDisplayed()
        assertFalse(rule.activity.isFinishing)
    }

    @Test fun backDuringQuestionOpensExitDialogAndKeepsExam() {
        val store = InMemoryStore()
        val vm = ExamViewModel(TestEnv.deps(store, FakeClock()))
        rule.setContent { ArbitratgeRoot(vm) }
        rule.waitForIdle()
        vm.onStartExam(); rule.waitForIdle()
        vm.onNameChange("Maria Puig"); vm.onNameContinue(); vm.onBeginExam(); rule.waitForIdle()
        rule.onNodeWithText("Pregunta 1 de 5").assertIsDisplayed()
        pressBack()
        rule.onNodeWithText("Vols sortir de l'examen?").assertIsDisplayed()
        assertFalse(rule.activity.isFinishing)
        assertEquals(SessionState.IN_PROGRESS, store.sessions.values.single().state)
    }
}
