package dev.lluissunol.arbitratge.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import dev.lluissunol.arbitratge.data.StoredCredential
import dev.lluissunol.arbitratge.exam.ExamEngine
import dev.lluissunol.arbitratge.exam.QuestionType
import dev.lluissunol.arbitratge.exam.SeededRng
import dev.lluissunol.arbitratge.support.TestEnv
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pruebas de semántica/accesibilidad de pantallas puras (sin ViewModel): 04 §8.2, 03 §10.4. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
class ComposeScreensTest {
    @get:Rule val rule = createComposeRule()

    private val questions = ExamEngine.start(TestEnv.bank, TestEnv.bank.rules, SeededRng(3)).snapshot.questions

    private fun questionState(type: QuestionType, selected: Set<String> = emptySet(), saveError: SaveError? = null, remaining: Long = 600_000, dialog: QuestionDialog? = null) =
        Stage.Question(
            index = 1, total = 5, question = questions.first { it.type == type }, selected = selected,
            remainingMs = remaining, timeHidden = false, banner = null, dialog = dialog, saving = false, saveError = saveError,
        )

    private fun hasRole(role: Role) = SemanticsMatcher("rol $role") { SemanticsProperties.Role in it.config && it.config[SemanticsProperties.Role] == role }

    @Composable
    private fun Scaled(scale: Float, content: @Composable () -> Unit) {
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) { ArbitratgeTheme(content) }
    }

    private fun show(st: Stage.Question, scale: Float = 1f, onSelect: (String) -> Unit = {}, onRetry: () -> Unit = {}, onNext: () -> Unit = {}) =
        rule.setContent {
            Scaled(scale) {
                QuestionScreen(st, onSelect, onNext, {}, {}, {}, {}, onRetry, {})
            }
        }

    @Test fun singleChoiceUsesRadioRoleAndReportsSelection() {
        val st = questionState(QuestionType.SINGLE_CHOICE)
        val clicked = mutableListOf<String>()
        show(st, onSelect = { clicked += it })
        rule.onAllNodes(hasRole(Role.RadioButton)).assertCountEquals(st.question.options.size)
        rule.onNodeWithText(st.question.options.first().text).performClick()
        assertEquals(listOf(st.question.options.first().id), clicked)
        rule.onNodeWithText("Pregunta 2 de 5").assertIsDisplayed()
        rule.onNodeWithText("Tria una resposta.").assertIsDisplayed()
        rule.onNodeWithText("Següent").assertIsDisplayed()
    }

    @Test fun multipleChoiceUsesCheckboxRoleAndCounter() {
        val st = questionState(QuestionType.MULTIPLE_CHOICE, selected = setOf("a", "b"))
        show(st)
        rule.onAllNodes(hasRole(Role.Checkbox)).assertCountEquals(st.question.options.size)
        rule.onNodeWithText("2 respostes marcades").assertIsDisplayed()
        rule.onNodeWithText("Marca totes les respostes correctes. Pots marcar-ne més d'una.").assertIsDisplayed()
    }

    @Test fun lastQuestionButtonSaysFinish() {
        show(questionState(QuestionType.SINGLE_CHOICE).copy(index = 4))
        rule.onNodeWithText("Finalitzar l'examen").assertIsDisplayed()
    }

    @Test fun saveFailureShowsModalDialogWithRetryAndKeepsSelectionVisible() {
        val st = questionState(QuestionType.SINGLE_CHOICE, selected = setOf("a"), saveError = SaveError.ANSWER)
        var retried = 0
        show(st, onRetry = { retried++ })
        rule.onNodeWithText("No s'ha pogut desar la teva resposta. Torna-ho a provar; la teva resposta continua marcada.").assertIsDisplayed()
        rule.onNodeWithText("Tornar-ho a provar").performClick()
        assertEquals(1, retried)
        rule.onNodeWithText("Sortir de l'examen").assertIsDisplayed()
    }

    @Test fun usableAtFontScale200Percent() {
        val st = questionState(QuestionType.MULTIPLE_CHOICE)
        show(st, scale = 2f)
        // el botón primario (anclado) sigue visible y las opciones alcanzables con scroll, sin truncar
        rule.onNodeWithText("Següent").assertIsDisplayed()
        st.question.options.forEach { rule.onNodeWithText(it.text).performScrollTo().assertIsDisplayed() }
    }

    @Test fun timerHasSpokenDescriptionAndWarningIconUnderFiveMinutes() {
        show(questionState(QuestionType.SINGLE_CHOICE, remaining = 4 * 60_000L + 59_000L))
        rule.onNodeWithContentDescription("Et queden 4 minuts i 59 segons").assertIsDisplayed()
        rule.onNodeWithText("⚠ 04:59").assertIsDisplayed()
    }

    private val cred = StoredCredential(
        "7f3a92c1-4b5d-4e6f-8a9b-000000000001", "s1", TestEnv.realIssuingToken(), "Maria Puig Vila", "2026-11-20", "dev", "sample-1", "0.0.0-sample",
    )

    @Test fun credentialScreenNeverClaimsValidityAndShowsShortCodeAndQr() {
        rule.setContent { ArbitratgeTheme { CredentialScreen(cred, {}, {}, {}) } }
        rule.onNodeWithText("Maria Puig Vila").assertIsDisplayed()
        rule.onNodeWithText("Codi: 7F3A-92C1").assertIsDisplayed()
        rule.onNodeWithText("Data del dispositiu: 20/11/2026").assertIsDisplayed()
        rule.onNodeWithContentDescription("Codi QR de la credencial de Maria Puig Vila").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("VÀLIDA", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("Mostrar a un organitzador").assertIsDisplayed()
    }

    @Test fun presentationShowsLargeQrNameAndCode() {
        rule.setContent { ArbitratgeTheme { PresentationScreen(cred, {}) } }
        rule.onNodeWithContentDescription("Codi QR de la credencial de Maria Puig Vila").assertIsDisplayed()
        rule.onNode(hasText("Codi: 7F3A-92C1")).assertIsDisplayed()
        rule.onNodeWithText("Emesa en autoservei").assertIsDisplayed()
    }
}
