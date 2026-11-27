package dev.lluissunol.arbitratge.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import dev.lluissunol.arbitratge.data.StoredCredential
import dev.lluissunol.arbitratge.exam.AnswerRecord
import dev.lluissunol.arbitratge.exam.ExamEngine
import dev.lluissunol.arbitratge.exam.ExamResult
import dev.lluissunol.arbitratge.exam.QuestionType
import dev.lluissunol.arbitratge.exam.SeededRng
import dev.lluissunol.arbitratge.support.TestEnv
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Pruebas de semántica/accesibilidad de pantallas puras (sin ViewModel): 04 §8.2, 03 §10.4. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h640dp-xxhdpi")
class ComposeScreensTest {
    @get:Rule val rule = createComposeRule()

    private val session = ExamEngine.start(TestEnv.bank, TestEnv.bank.rules, SeededRng(3))
    private val questions = session.snapshot.questions

    private fun questionState(
        type: QuestionType,
        selected: Set<String> = emptySet(),
        saveError: SaveError? = null,
        remaining: Long = 600_000,
        dialog: QuestionDialog? = null,
        index: Int = 1,
        answered: Set<Int> = emptySet(),
        level: TimeBanner? = null,
        hidden: Boolean = false,
    ) = Stage.Question(
        index = index, total = 5, question = questions.first { it.type == type }, selected = selected, answered = answered,
        remainingMs = remaining, timeHidden = hidden, banner = null, dialog = dialog, saving = false, saveError = saveError,
        level = level,
    )

    private fun hasRole(role: Role) = SemanticsMatcher("rol $role") { SemanticsProperties.Role in it.config && it.config[SemanticsProperties.Role] == role }

    @Composable
    private fun Scaled(scale: Float, dark: Boolean = false, content: @Composable () -> Unit) {
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) { ArbitratgeTheme(darkTheme = dark, content = content) }
    }

    private fun show(st: Stage.Question, scale: Float = 1f, dark: Boolean = false, a: QuestionActions = QuestionActions()) =
        rule.setContent { Scaled(scale, dark) { QuestionScreen(st, a) } }

    // ------------------------------------------------------------------ tipos de pregunta

    @Test fun singleChoiceUsesRadioRoleLabelAndReportsSelection() {
        val st = questionState(QuestionType.SINGLE_CHOICE)
        val clicked = mutableListOf<String>()
        show(st, a = QuestionActions(onSelect = { clicked += it }))
        rule.onAllNodes(hasRole(Role.RadioButton)).assertCountEquals(st.question.options.size)
        rule.onAllNodes(hasRole(Role.Checkbox)).assertCountEquals(0)
        rule.onNodeWithText(st.question.options.first().text).performClick()
        assertEquals(listOf(st.question.options.first().id), clicked)
        rule.onNodeWithText("Pregunta 2 de 5").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("Una resposta").fetchSemanticsNodes().isEmpty(), "sense etiqueta de tipus")
        // pista visible retirada (spec-acceptada): queda per a TalkBack dins la descripció de l'enunciat
        assertTrue(rule.onAllNodesWithText("Tria una resposta.").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithContentDescription("${st.question.text}. Tria una resposta.").assertExists()
        rule.onNodeWithText("Següent").assertIsDisplayed()
        rule.onNodeWithText("Anterior").assertIsDisplayed()
    }

    @Test fun trueFalseHasLargeVfTreatmentAndTheStatementQuestion() {
        val st = questionState(QuestionType.TRUE_FALSE)
        show(st)
        rule.onAllNodes(hasRole(Role.RadioButton)).assertCountEquals(2)
        assertTrue(rule.onAllNodesWithText("Cert o fals").fetchSemanticsNodes().isEmpty(), "sense etiqueta de tipus")
        assertTrue(rule.onAllNodesWithText("Aquesta afirmació és certa o falsa?").fetchSemanticsNodes().isEmpty(), "pista només per a TalkBack")
        rule.onNodeWithContentDescription("${st.question.text}. Aquesta afirmació és certa o falsa?").assertExists()
        // «Vertader» es MOSTRA com «Cert» (només presentació; l'id i el text del banc no canvien)
        rule.onNodeWithText("Cert").assertIsDisplayed()
        rule.onNodeWithText("Fals").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("Vertader").fetchSemanticsNodes().isEmpty())
        // la insignia de lletra gran (V / F) es decorativa: no es duplica en TalkBack
        assertTrue(rule.onAllNodesWithText("V").fetchSemanticsNodes().isEmpty(), "la lletra decorativa no entra a l'arbre de semàntica")
        assertTrue(rule.onAllNodesWithText("Tria una resposta.").fetchSemanticsNodes().isEmpty())
    }

    @Test fun multipleChoiceUsesCheckboxHighlightedPanelAndLiveCount() {
        val st = questionState(QuestionType.MULTIPLE_CHOICE, selected = setOf("a", "b"))
        show(st)
        rule.onAllNodes(hasRole(Role.Checkbox)).assertCountEquals(st.question.options.size)
        rule.onAllNodes(hasRole(Role.RadioButton)).assertCountEquals(0)
        // un únic avís destacat amb el text exacte; cap etiqueta de tipus ni recompte de marcades
        assertTrue(rule.onAllNodesWithText("N'hi ha més d'una resposta correcta. Marca-les totes.").fetchSemanticsNodes().isEmpty(), "ajuda llarga només per a TalkBack")
        rule.onNodeWithText("Resposta múltiple").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("respostes marcades", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithText("Cap resposta marcada").fetchSemanticsNodes().isEmpty())
    }

    @Test fun emptyMultipleSelectionShowsZeroCountAndNextHasNoDialog() {
        var next = 0
        show(questionState(QuestionType.MULTIPLE_CHOICE), a = QuestionActions(onNext = { next++ }))
        assertTrue(rule.onAllNodesWithText("Cap resposta marcada").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("Següent").performClick()
        assertEquals(1, next)
        assertTrue(rule.onAllNodesWithText("No has marcat cap resposta.").fetchSemanticsNodes().isEmpty())
    }

    @Test fun selectedStateDiffersInShapeAndColorFromUnselectedAndFromCorrectness() {
        // «Marca de bolígraf» (I2): la selecció és tinta, diferent del verd de «correcta» i del vermell de «incorrecta» (UX F4).
        assertNotEquals(LightArbColors.mark, LightArbColors.correct)
        assertNotEquals(LightArbColors.mark, LightArbColors.wrong)
        assertNotEquals(DarkArbColors.mark, DarkArbColors.correct)
        assertNotEquals(DarkArbColors.mark, DarkArbColors.wrong)
        assertNotEquals(LightArbColors.mark, LightScheme.surface)
        assertNotEquals(DarkArbColors.mark, DarkScheme.surface)
    }

    // ------------------------------------------------------------------ barra Anterior / Següent

    @Test fun firstQuestionShowsADisabledPreviousButtonAndNextKeepsItsHalf() {
        show(questionState(QuestionType.SINGLE_CHOICE, index = 0))
        rule.onNodeWithText("Anterior").assertIsDisplayed().assertIsNotEnabled()
        rule.onNodeWithText("Següent").assertIsDisplayed()
    }

    @Test fun lastQuestionButtonOpensReviewNotFinish() {
        show(questionState(QuestionType.SINGLE_CHOICE).copy(index = 4))
        rule.onNodeWithText("Revisar").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("Finalitzar l'examen").fetchSemanticsNodes().isEmpty())
    }

    @Test fun previousAndOverviewButtonsFireCallbacks() {
        var prev = 0
        var over = 0
        show(questionState(QuestionType.SINGLE_CHOICE), a = QuestionActions(onPrevious = { prev++ }, onOverview = { over++ }))
        rule.onNodeWithText("Anterior").performClick()
        // el resum és ara una icona de la capçalera amb el recompte a la descripció (la fila «Respostes N de M» s'ha retirat)
        rule.onNodeWithContentDescription("Veure el resum: 0 de 5 respostes").performClick()
        assertEquals(1, prev)
        assertEquals(1, over)
        assertTrue(rule.onAllNodesWithText("Respostes:", substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test fun saveFailureShowsModalDialogWithRetryAndKeepsSelectionVisible() {
        val st = questionState(QuestionType.SINGLE_CHOICE, selected = setOf("a"), saveError = SaveError.ANSWER)
        var retried = 0
        show(st, a = QuestionActions(onRetrySave = { retried++ }))
        rule.onNodeWithText("No s'ha pogut desar la teva resposta. Torna-ho a provar; la teva resposta continua marcada.").assertIsDisplayed()
        rule.onNodeWithText("Tornar-ho a provar").performClick()
        assertEquals(1, retried)
        rule.onNodeWithText("Sortir de l'examen").assertIsDisplayed()
    }

    @Test fun exitDialogReassuresAnswersAreSaved() {
        show(questionState(QuestionType.SINGLE_CHOICE, dialog = QuestionDialog.EXIT))
        rule.onNodeWithText("Vols sortir de l'examen?").assertIsDisplayed()
        rule.onNodeWithText("El temps continua passant. Les respostes ja estan desades: quan tornis a obrir l'app, podràs continuar.").assertIsDisplayed()
        rule.onNodeWithText("Continuar l'examen").assertIsDisplayed()
        rule.onNodeWithText("Sortir").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ 200 % i fosc

    private fun multiAt200(dark: Boolean) {
        val st = questionState(QuestionType.MULTIPLE_CHOICE)
        show(st, scale = 2f, dark = dark)
        // [Anterior | Següent] 50/50 segueix visible; les opcions són accessibles amb scroll
        rule.onNodeWithText("Següent").assertIsDisplayed()
        rule.onNodeWithText("Anterior").assertIsDisplayed()
        st.question.options.forEach { rule.onNodeWithText(it.text).performScrollTo().assertIsDisplayed() }
    }

    @Test fun usableAtFontScale200PercentLight() = multiAt200(dark = false)

    @Test fun usableAtFontScale200PercentDark() = multiAt200(dark = true)

    @Test fun trueFalseUsableAtFontScale200Percent() {
        show(questionState(QuestionType.TRUE_FALSE), scale = 2f, dark = true)
        rule.onNodeWithText("Cert").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Fals").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Següent").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ cronòmetre

    @Test fun timerShowsMinutesUntilTheLastMinuteWithSpokenDescription() {
        show(questionState(QuestionType.SINGLE_CHOICE, remaining = 18 * 60_000L))
        rule.onNodeWithText("18 min").assertIsDisplayed()
        rule.onNodeWithContentDescription("Et queden 18 minuts").assertIsDisplayed()
    }

    @Test fun timerRoundsUpMinutesAndWarnsUnderFiveMinutes() {
        show(questionState(QuestionType.SINGLE_CHOICE, remaining = 4 * 60_000L + 59_000L, level = TimeBanner.FIVE_MIN))
        rule.onNodeWithText("5 min").assertIsDisplayed()
        rule.onNodeWithContentDescription("Et queden 5 minuts").assertIsDisplayed()
    }

    @Test fun timerSwitchesToClockInTheLastMinute() {
        show(questionState(QuestionType.SINGLE_CHOICE, remaining = 59_000L, level = TimeBanner.ONE_MIN))
        rule.onNodeWithText("00:59").assertIsDisplayed()
        rule.onNodeWithContentDescription("Et queden 0 minuts i 59 segons").assertIsDisplayed()
    }

    @Test fun hiddenTimerOffersToShowItAndTogglesOnClick() {
        var toggled = 0
        show(questionState(QuestionType.SINGLE_CHOICE, hidden = true), a = QuestionActions(onToggleTime = { toggled++ }))
        rule.onNodeWithText("Mostrar el temps").performClick()
        assertEquals(1, toggled)
        assertTrue(rule.onAllNodesWithText("min", substring = true).fetchSemanticsNodes().isEmpty(), "el temps no es veu")
    }

    @Test fun timerPillHasAClickLabelForTalkBack() {
        show(questionState(QuestionType.SINGLE_CHOICE))
        val nodes = rule.onAllNodes(SemanticsMatcher("onClick amb etiqueta «amagar el temps»") {
            it.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.OnClick)?.label == "amagar el temps"
        })
        nodes.assertCountEquals(1)
    }

    // ------------------------------------------------------------------ resum (C4b)

    private fun overview(answered: List<Boolean>, current: Int = 1, dialog: QuestionDialog? = null) = Stage.Overview(
        items = questions.mapIndexed { i, q -> OverviewItem(i, q.text, answered[i]) },
        current = current, remainingMs = 600_000, timeHidden = false, banner = null, dialog = dialog, saveError = null,
    )

    @Test fun overviewListsEveryQuestionWithStateAndFinishButton() {
        val st = overview(listOf(true, false, true, false, false))
        var gone = -1
        rule.setContent {
            ArbitratgeTheme {
                OverviewScreen(st, QuestionActions(), {}, { gone = it }, {}, {})
            }
        }
        rule.onNodeWithText("Preguntes").assertIsDisplayed()
        rule.onNodeWithText("2 contestades · en falten 3").assertIsDisplayed()
        rule.onNodeWithText("Pregunta 1 · Contestada").assertIsDisplayed()
        rule.onNodeWithText("Pregunta 2 · Sense resposta").assertIsDisplayed()
        rule.onNodeWithText("Ets aquí").assertIsDisplayed()
        rule.onNodeWithText("Anar a la primera sense resposta").assertIsDisplayed()
        rule.onNodeWithText("Avaluar").assertIsDisplayed()
        rule.onNodeWithContentDescription("Pregunta 4, Sense resposta. ${questions[3].text}. Toca per anar-hi.").performScrollTo().performClick()
        assertEquals(3, gone)
    }

    @Test fun finishConfirmationStatesTheUnansweredCountAndTheSafeActionIsPrimary() {
        val st = overview(listOf(true, false, true, false, false), dialog = QuestionDialog.FINISH)
        var dismissed = 0
        var confirmed = 0
        rule.setContent { ArbitratgeTheme { OverviewScreen(st, QuestionActions(onDialogDismiss = { dismissed++ }, onDialogConfirm = { confirmed++ }), {}, {}, {}, {}) } }
        rule.onNodeWithText("Vols avaluar l'examen?").assertIsDisplayed()
        rule.onNodeWithText("Tens 3 preguntes sense resposta: comptaran com a fallades.").assertIsDisplayed()
        rule.onNodeWithText("Un cop avaluat, ja no podràs canviar cap resposta.").assertIsDisplayed()
        rule.onNodeWithText("Tornar a l'examen").performClick()
        assertEquals(1, dismissed)
        rule.onAllNodesWithText("Avaluar").onLast().performClick() // el del diàleg (el primari del resum és sota)
        assertEquals(1, confirmed)
    }

    @Test fun finishConfirmationWithEverythingAnswered() {
        val st = overview(List(5) { true }, dialog = QuestionDialog.FINISH)
        rule.setContent { ArbitratgeTheme { OverviewScreen(st, QuestionActions(), {}, {}, {}, {}) } }
        // al resum («Has respost totes…») i al diàleg de confirmació
        rule.onAllNodesWithText("Has respost totes les preguntes.", substring = true).assertCountEquals(2)
        assertTrue(rule.onAllNodesWithText("Anar a la primera sense resposta").fetchSemanticsNodes().isEmpty())
    }

    @Test fun finishFailureDialogIsUnambiguousAndNonDismissible() {
        val st = overview(List(5) { true }).copy(saveError = SaveError.RESULT)
        var retried = 0
        rule.setContent { ArbitratgeTheme { OverviewScreen(st, QuestionActions(onRetrySave = { retried++ }), {}, {}, {}, {}) } }
        rule.onNodeWithText("No s'ha pogut avaluar").assertIsDisplayed()
        rule.onNodeWithText("No hem pogut desar el resultat. Les teves respostes continuen desades. Allibera espai al mòbil i torna-ho a provar.").assertIsDisplayed()
        rule.onNodeWithText("Tornar-ho a provar").performClick()
        assertEquals(1, retried)
    }

    @Test fun overviewUsableAtFontScale200PercentDark() {
        val st = overview(listOf(true, false, true, false, false))
        rule.setContent { Scaled(2f, dark = true) { OverviewScreen(st, QuestionActions(), {}, {}, {}, {}) } }
        rule.onNodeWithText("Avaluar").assertIsDisplayed()
        rule.onNodeWithText("Preguntes").assertIsDisplayed()
        rule.onNodeWithText("Pregunta 5 · Sense resposta").performScrollTo().assertIsDisplayed()
    }

    // ------------------------------------------------------------------ Home / resultat / repàs (200 %, fosc)

    private val cred = StoredCredential(
        "7f3a92c1-4b5d-4e6f-8a9b-000000000001", "s1", TestEnv.realIssuingToken(), "Maria Puig Vila", "2026-11-20", "dev", "sample-1", "0.0.0-sample",
    )

    @Test fun homeAt200PercentDarkShowsFactsCredentialAndNoPracticeClaim() {
        val st = Stage.Home(RulesSummary(5, 5, 3), cred, LastResult("s1", 2, 5, false))
        rule.setContent { Scaled(2f, dark = true) { HomeScreen(st, {}, {}, {}) } }
        // I9: a ≥ 1,5× les accions baixen al final del contingut (sense barra fixa)
        rule.onNodeWithText("Veure la credencial").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Tornar a fer l'examen").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("preguntes").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Tens la credencial d'aptitud.").performScrollTo().assertIsDisplayed()
        // credencial i últim intent, separats (UX F11)
        rule.onNodeWithText("Últim examen: 2 de 5 encerts (no aprovat)").performScrollTo().assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("practicar", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())
    }

    @Test fun homeEmptyDoesNotClaimPractice() {
        rule.setContent { ArbitratgeTheme { HomeScreen(Stage.Home(RulesSummary(5, 5, 3), null, null), {}, {}, {}) } }
        rule.onNodeWithText("Encara no has fet cap examen. El pots fer tantes vegades com vulguis.").assertIsDisplayed()
        rule.onNodeWithText("Començar l'examen").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("practicar", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())
    }

    private fun resultOf(answers: Map<Int, List<String>>): ExamResult {
        val s = session.copy(answers = answers.map { (i, ids) -> AnswerRecord(i, ids, 1000) })
        return ExamEngine.grade(s, 106_000L)
    }

    @Test fun resultSeparatesUnansweredFromWrongAndShowsTimeAtDark200() {
        // 3 correctes, 1 incorrecta, 1 sense resposta
        val q = questions
        val answers = mutableMapOf<Int, List<String>>()
        for (i in 0..2) answers[i] = q[i].correctIds
        answers[3] = listOf(q[3].options.first { it.id !in q[3].correctIds }.id)
        val r = resultOf(answers)
        assertEquals(3, r.correct); assertEquals(1, r.wrong); assertEquals(1, r.unanswered)
        rule.setContent { Scaled(2f, dark = true) { ResultScreen(Stage.Result("s1", r, CredentialPhase.Ready(cred)), {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("Has aprovat!").assertIsDisplayed()
        rule.onNodeWithText("Veure la credencial").performScrollTo().assertIsDisplayed()
        // Incorrectes = només les contestades malament (1, singular); «Sense resposta» apareix a part perquè n'hi ha
        rule.onNodeWithText("Incorrecta").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Sense resposta").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("1:46").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("3 encerts de 5").assertExists()
        // sense el títol «Resultat»: el veredicte fa d'encapçalament
        assertTrue(rule.onAllNodesWithText("Resultat").fetchSemanticsNodes().isEmpty())
    }

    @Test fun passedResultOffersTheCredentialMiniAboveTheFullWidthButtonsAndNoBanner() {
        val q = questions
        val r = resultOf(q.indices.associateWith { q[it].correctIds })
        rule.setContent { ArbitratgeTheme { ResultScreen(Stage.Result("s1", r, CredentialPhase.Ready(cred)), {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("Has aprovat!").assertIsDisplayed()
        rule.onNodeWithText("Maria Puig Vila").assertIsDisplayed()
        // sense fallades ni preguntes en blanc: Encerts, Incorrectes (0) i Temps; «Sense resposta» no es mostra (UI-05, 3 requadres)
        rule.onNodeWithText("Encerts", useUnmergedTree = true).assertExists()
        assertTrue(rule.onAllNodesWithText("Sense resposta").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithContentDescription("5 encerts de 5").assertExists()
        val mini = rule.onNodeWithText("Maria Puig Vila").fetchSemanticsNode().boundsInRoot.top
        val primary = rule.onNodeWithText("Veure la credencial").fetchSemanticsNode().boundsInRoot.top
        assertTrue(mini < primary, "la miniatura de la credencial va damunt del botó")
    }

    @Test fun resultVerdictIsTheScreenHeadingFitsOnOneLineAndButtonsAreFullWidth() {
        val r = resultOf(emptyMap())
        rule.setContent { ArbitratgeTheme { ResultScreen(Stage.Result("s1", r, null), {}, {}, {}, {}, {}) } }
        rule.onNode(hasText("No has aprovat") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("Et van faltar", substring = true).fetchSemanticsNodes().isEmpty())
        // «No has aprovat» en UNA línia (TextLayoutResult)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        rule.onNodeWithText("No has aprovat").fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertEquals(1, layouts.single().lineCount, "«No has aprovat» ocupa una sola línia")
        // tres botons a tot l'ample del contingut (amplada de la pantalla − 2 × marge) i ≥ 56 dp
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot.width
        val expected = root - 2 * ArbDimens.gutter.value * 3f
        listOf("Repassar les 5 preguntes fallades", "Tornar-ho a provar", "Tornar a l'inici").forEach { t ->
            val b = rule.onNode(hasClickAction() and hasText(t)).fetchSemanticsNode().boundsInRoot
            assertEquals(expected, b.width, 2f, "«$t» a tot l'ample")
            assertTrue(b.height >= 56f * 3f - 1f, "«$t» ≥ 56 dp")
        }
    }

    @Test fun resultNumbersNeverContradictEachOtherWhenTimeRanOut() {
        // temps esgotat: cap resposta → 0 incorrectes, 5 sense resposta, botó de 5 (el filtre «Fallades 5» del repàs no es comprova aquí)
        val r = resultOf(emptyMap())
        assertEquals(0, r.wrong); assertEquals(5, r.unanswered); assertEquals(5, r.failures)
        rule.setContent { ArbitratgeTheme { ResultScreen(Stage.Result("s1", r, null), {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("Incorrectes").assertExists()
        rule.onNodeWithText("Sense resposta").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Repassar les 5 preguntes fallades").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("Fallades", substring = true).fetchSemanticsNodes().isEmpty(), "el bloc de xifres no diu «Fallades»")
        rule.onNodeWithContentDescription("0 incorrectes, 5 sense resposta. Temps: 1 min 46 s").assertExists()
    }

    @Test fun resultWithOneWrongAnswerUsesSingularForms() {
        val q = questions
        val answers = mutableMapOf<Int, List<String>>()
        for (i in 1..4) answers[i] = q[i].correctIds
        answers[0] = listOf(q[0].options.first { it.id !in q[0].correctIds }.id)
        val r = resultOf(answers)
        rule.setContent { ArbitratgeTheme { ResultScreen(Stage.Result("s1", r, CredentialPhase.Ready(cred)), {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("Incorrecta").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Repassar la pregunta fallada").assertIsDisplayed()
        rule.onNodeWithContentDescription("4 encerts de 5").assertExists()
    }

    @Test fun failedResultKeepsAHopefulToneAndReviewIsPrimary() {
        val r = resultOf(emptyMap())
        rule.setContent { ArbitratgeTheme { ResultScreen(Stage.Result("s1", r, null), {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("No has aprovat").assertIsDisplayed()
        rule.onNodeWithText("Repassar les 5 preguntes fallades").assertIsDisplayed()
        rule.onNodeWithText("Sense resposta").performScrollTo().assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("Cap fallada", substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test fun reviewDetailExplainsAMissedCorrectOptionOfAMultipleQuestion() {
        val multi = questions.indexOfFirst { it.type == QuestionType.MULTIPLE_CHOICE }
        val q = questions[multi]
        assertTrue(q.correctIds.size >= 2)
        val r = resultOf(mapOf(multi to listOf(q.correctIds.first())))
        val st = Stage.Review("s1", r, onlyWrong = true, openIndex = r.items.filter { !it.isCorrect }.indexOfFirst { it.questionId == q.id }, fromResult = true)
        rule.setContent { ArbitratgeTheme { ReviewScreen(st, {}, {}, {}) } }
        rule.onNodeWithText("Fallada").assertIsDisplayed()
        rule.onNodeWithText("Resposta múltiple").assertIsDisplayed()
        // frase de motivo (calculada) i TOTES les opcions amb el veredicte de cadascuna
        val missed = q.correctIds.size - 1
        val reason = if (missed == 1) "No vas marcar 1 resposta correcta." else "No vas marcar $missed respostes correctes."
        rule.onNodeWithText(reason).assertIsDisplayed()
        rule.onNodeWithText("Correcta · la vas marcar").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Correcta · no la vas marcar").onFirst().performScrollTo().assertIsDisplayed()
        q.options.forEach { rule.onNodeWithText(it.text).performScrollTo().assertIsDisplayed() }
        assertTrue(rule.onAllNodesWithText("Incorrecta · no la vas marcar").fetchSemanticsNodes().isNotEmpty(), "també es veuen les incorrectes no marcades")
    }

    @Test fun reviewDetailOfASingleChoiceMistakeHasNoBannerAndNoAmber() {
        val single = questions.indexOfFirst { it.type == QuestionType.SINGLE_CHOICE }
        val q = questions[single]
        val wrong = q.options.first { it.id !in q.correctIds }.id
        val r = resultOf(mapOf(single to listOf(wrong)))
        val st = Stage.Review("s1", r, onlyWrong = true, openIndex = r.items.filter { !it.isCorrect }.indexOfFirst { it.questionId == q.id }, fromResult = true)
        rule.setContent { ArbitratgeTheme { ReviewScreen(st, {}, {}, {}) } }
        rule.onNodeWithText("Fallada").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("La teva resposta no era la correcta", substring = true).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("Incorrecta · la vas marcar").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Correcta · no la vas marcar").performScrollTo().assertIsDisplayed()
    }

    @Test fun reviewListAt200PercentDarkIsInNaturalOrderWithStatusAndNoDisabledNavigation() {
        val q = questions
        val r = resultOf(mapOf(0 to q[0].correctIds, 1 to listOf(q[1].options.first { it.id !in q[1].correctIds }.id)))
        rule.setContent { Scaled(2f, dark = true) { ReviewScreen(Stage.Review("s1", r, onlyWrong = false, openIndex = null, fromResult = true), {}, {}, {}) } }
        rule.onNodeWithText("Pregunta 1 de 5 · Encertada").assertIsDisplayed()
        rule.onNodeWithText("Pregunta 2 de 5 · Fallada").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Pregunta 3 de 5 · Sense resposta").performScrollTo().assertIsDisplayed()
    }

    @Test fun reviewDetailWithASingleItemHasNoNavigationBar() {
        val r = resultOf(emptyMap())
        val st = Stage.Review("s1", r, onlyWrong = true, openIndex = 0, fromResult = true).copy(result = r.copy(items = r.items.take(1)))
        rule.setContent { ArbitratgeTheme { ReviewScreen(st, {}, {}, {}) } }
        assertTrue(rule.onAllNodesWithText("Anterior").fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithText("Següent").fetchSemanticsNodes().isEmpty())
    }

    // ------------------------------------------------------------------ credencial

    // UI-08: sense barra superior, segell APTE (no chip), sense «Examen», «Codi» ni avís d'autoservei.
    @Test fun credentialScreenHasNoTopBarShowsStampAndDateAndNeverClaimsValidity() {
        rule.setContent { ArbitratgeTheme { CredentialScreen(cred, {}, {}, {}) } }
        rule.onNodeWithText("Maria Puig Vila").assertIsDisplayed()
        rule.onNodeWithText("20/11/2026").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Data de l'examen").assertIsDisplayed()
        rule.onNodeWithTag("apte_stamp").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Resultat: APTE").assertExists()
        rule.onNodeWithContentDescription("Codi QR de la credencial de Maria Puig Vila").performScrollTo().assertIsDisplayed()
        for (gone in listOf("La teva credencial", "Examen", "reglament sample-1", "7F3A-92C1", "Emesa en autoservei", "No és un certificat oficial")) {
            assertTrue(rule.onAllNodesWithText(gone, substring = true).fetchSemanticsNodes().isEmpty(), "ha d'haver desaparegut: $gone")
        }
        assertTrue(rule.onAllNodesWithText("VÀLIDA", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("Mostrar a un organitzador").assertIsDisplayed()
    }

    @Test fun credentialFollowsTheOrderNameStampQrAndTheStampNeverCoversTheQr() {
        rule.setContent { ArbitratgeTheme { CredentialScreen(cred, {}, {}, {}) } }
        fun box(n: androidx.compose.ui.test.SemanticsNodeInteraction) = n.performScrollTo().fetchSemanticsNode().boundsInRoot
        val name = box(rule.onNodeWithText("Maria Puig Vila"))
        val stamp = box(rule.onNodeWithTag("apte_stamp"))
        val qr = box(rule.onNodeWithContentDescription("Codi QR de la credencial de Maria Puig Vila"))
        assertTrue(name.top < stamp.top && stamp.top < qr.top, "nom < segell < QR")
        assertTrue(stamp.bottom <= qr.top, "el segell no pot tapar el QR (UI-08b)")
        rule.onNodeWithText("Credencial d'aptitud").assertExists()
    }

    @Test fun timeUpScreenHasASingleCtaAndReportsTheClick() {
        var seen = 0
        rule.setContent { ArbitratgeTheme { TimeUpScreen(Stage.TimeUp(3, 5, away = false)) { seen++ } } }
        rule.onNodeWithText("S'ha acabat el temps").assertIsDisplayed()
        rule.onNodeWithText("Has respost 3 de 5 preguntes.").assertIsDisplayed()
        rule.onNodeWithText("Veure el resultat").performClick()
        assertEquals(1, seen)
        rule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test fun timeUpAwayShowsTheAwayNotice() {
        rule.setContent { ArbitratgeTheme { TimeUpScreen(Stage.TimeUp(3, 5, away = true)) {} } }
        rule.onNodeWithText("El temps ja s'havia acabat mentre eres fora.").assertIsDisplayed()
    }

    @Test fun oneMinuteWarningShowsItsTextAndTheDismissButtonCloses() {
        var closed = 0
        rule.setContent { ArbitratgeTheme { TimeBannerCard(TimeBanner.ONE_MIN) { closed++ } } }
        rule.onNodeWithText("Només queda 1 minut.").assertIsDisplayed()
        rule.onNodeWithContentDescription("Tancar l'avís").performClick()
        assertEquals(1, closed)
    }

    @Test fun credentialAt200PercentDarkStillShowsTheDocument() {
        rule.setContent { Scaled(2f, dark = true) { CredentialScreen(cred, {}, {}, {}) } }
        rule.onNodeWithText("Maria Puig Vila").assertIsDisplayed()
        rule.onNodeWithText("20/11/2026").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Mostrar a un organitzador").performScrollTo().assertIsDisplayed()
    }

    @Test fun presentationShowsLargeQrNameCodeAndNoRemovePrefixHack() {
        rule.setContent { ArbitratgeTheme { PresentationScreen(cred, {}) } }
        rule.onNodeWithContentDescription("Codi QR de la credencial de Maria Puig Vila").assertIsDisplayed()
        rule.onNodeWithText("Maria Puig Vila").assertIsDisplayed()
        rule.onNodeWithText("APTE").performScrollTo().assertIsDisplayed()
        for (gone in listOf("Mantén la pantalla", "Per comprovar-la", "Emesa en autoservei", "Codi:", "reglament")) {
            assertTrue(rule.onAllNodesWithText(gone, substring = true).fetchSemanticsNodes().isEmpty(), "ha d'haver desaparegut: $gone")
        }
        rule.onNodeWithText("Tancar").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ scroll, foco, estado de guardado

    private fun longQuestion(i: Int, base: Stage.Question) = base.copy(
        index = i,
        question = base.question.copy(
            text = "Pregunta llarga $i ".repeat(12).trim(),
            options = List(8) { k -> dev.lluissunol.arbitratge.exam.Option("o$k", "Opció $i-$k ".repeat(4).trim()) },
        ),
        selected = emptySet(), committed = emptySet(),
    )

    @Test fun scrollStateIsNotReusedBetweenQuestions() {
        val base = questionState(QuestionType.SINGLE_CHOICE)
        var st by androidx.compose.runtime.mutableStateOf(longQuestion(1, base))
        rule.setContent { Scaled(2f) { QuestionScreen(st, QuestionActions()) } }
        rule.onNodeWithText("Opció 1-7 ".repeat(4).trim()).performScrollTo() // al final de la pregunta 2
        st = longQuestion(2, base)
        rule.waitForIdle()
        // la pregunta nueva empieza arriba: su enunciado es visible sin hacer scroll
        rule.onNodeWithText("Pregunta llarga 2 ".repeat(12).trim()).assertIsDisplayed()
    }

    @Test fun longOptionRowsGrowInsteadOfClipping() {
        val q = questionState(QuestionType.SINGLE_CHOICE)
        val long = "Opció amb una explicació molt llarga que ha d'ocupar quatre línies sense tallar-se ".repeat(3).trim()
        val opts = q.question.options.mapIndexed { i, o -> if (i == 0) o.copy(text = long) else o }
        show(q.copy(question = q.question.copy(options = opts)))
        val row = rule.onNode(hasText(long)).fetchSemanticsNode().boundsInRoot
        assertTrue(row.height >= 56f * 3, "el text llarga ha de créixer a diverses línies: ${row.height}")
        rule.onNodeWithText(long).assertIsDisplayed()
    }

    @Test fun focusMovesToTheQuestionHeaderWhenTheIndexChanges() {
        val base = questionState(QuestionType.SINGLE_CHOICE, index = 0)
        var st by androidx.compose.runtime.mutableStateOf(base)
        rule.setContent { Scaled(1f) { QuestionScreen(st, QuestionActions()) } }
        rule.waitForIdle()
        rule.onNodeWithText("Pregunta 1 de 5").assertIsFocused()
        st = base.copy(index = 3)
        rule.waitForIdle()
        rule.onNodeWithText("Pregunta 4 de 5").assertIsFocused()
        // la cabecera cuenta pregunta, tipo y estado para TalkBack
        rule.onNodeWithContentDescription("Pregunta 4 de 5. Una resposta. Sense resposta").assertExists()
    }

    @Test fun withTalkBackOnAnExplicitAnnouncementIsSentWhenTheQuestionChanges() {
        val am = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val shadow = org.robolectric.Shadows.shadowOf(am)
        shadow.setEnabled(true)
        shadow.setTouchExplorationEnabled(true)
        val base = questionState(QuestionType.SINGLE_CHOICE, index = 0)
        var st by androidx.compose.runtime.mutableStateOf(base)
        rule.setContent { Scaled(1f) { QuestionScreen(st, QuestionActions()) } }
        rule.waitForIdle()
        st = base.copy(index = 3)
        rule.waitForIdle()
        val texts = shadow.sentAccessibilityEvents
            .filter { it.eventType == android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT }
            .map { it.text.joinToString(" ") }
        assertTrue(texts.any { it.contains("Pregunta 4 de 5. Una resposta. Sense resposta") }, "anuncis: $texts")
    }

    @Test fun savedStateComesOnlyFromTheCommittedSelection() {
        val q = questionState(QuestionType.SINGLE_CHOICE)
        val id = q.question.options.first().id
        // seleccionada pero SIN confirmar: no se anuncia «desada»
        show(q.copy(selected = setOf(id), committed = emptySet(), saving = false))
        assertTrue(rule.onAllNodesWithText("Resposta desada").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithContentDescription("Pregunta 2 de 5. Una resposta. Sense resposta").assertExists()
    }

    @Test fun noVisibleSaveStatusLineIsShown() {
        val q = questionState(QuestionType.SINGLE_CHOICE)
        val id = q.question.options.first().id
        show(q.copy(selected = setOf(id), committed = setOf(id), saving = true))
        assertTrue(rule.onAllNodesWithText("Desant…").fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithText("Resposta desada").fetchSemanticsNodes().isEmpty())
    }

    @Test fun headerIsASingleRowWithSegmentedProgressAndTheProgressLabelSitsAboveTheStem() {
        // UI-05: «Pregunta N de M» passa de la capçalera a antetítol de l'enunciat (mateix heading i foco); la capçalera
        // conserva el rellotge i el resum (zona tàctil ≥ 48 dp) en UNA fila.
        val st = questionState(QuestionType.SINGLE_CHOICE, index = 3, answered = setOf(0, 2))
        show(st)
        assertTrue(rule.onAllNodesWithContentDescription("Posició de l'examen: pregunta 4 de 5").fetchSemanticsNodes().isEmpty())
        rule.onNodeWithContentDescription("Veure el resum: 2 de 5 respostes").assertIsDisplayed()
        val label = rule.onNodeWithText("Pregunta 4 de 5").fetchSemanticsNode().boundsInRoot
        val stem = rule.onNodeWithText(st.question.text).fetchSemanticsNode().boundsInRoot
        val icon = rule.onNodeWithTag(OVERVIEW_TAG).fetchSemanticsNode().boundsInRoot
        val timer = rule.onNodeWithContentDescription("Et queden 10 minuts").fetchSemanticsNode().boundsInRoot
        assertTrue(icon.width >= 48f * 3 - 1f && icon.height >= 48f * 3 - 1f, "icona ≥ 48 dp: ${icon.width}x${icon.height}")
        assertTrue(label.bottom <= stem.top + 1f, "l'antetítol va damunt de l'enunciat")
        assertTrue(icon.bottom <= label.top + 1f, "la capçalera (resum) va damunt de l'antetítol")
        assertEquals(timer.top + timer.height / 2, icon.top + icon.height / 2, 3f, "rellotge i resum, mateixa fila")
        rule.onNode(hasText("Pregunta 4 de 5") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
    }

    @Test fun optionCardsKeepTheirBoundsWhenSelectedBecauseTheBorderIsNotLayout() {
        val q = questionState(QuestionType.SINGLE_CHOICE)
        val first = q.question.options.first()
        var st by androidx.compose.runtime.mutableStateOf(q)
        rule.setContent { Scaled(1f) { QuestionScreen(st, QuestionActions()) } }
        val before = rule.onAllNodes(hasRole(Role.RadioButton)).onFirst().fetchSemanticsNode().boundsInRoot
        st = q.copy(selected = setOf(first.id))
        rule.waitForIdle()
        val after = rule.onAllNodes(hasRole(Role.RadioButton)).onFirst().fetchSemanticsNode().boundsInRoot
        assertEquals(before, after, "UI-05: marcar l'opció no mou ni redimensiona la targeta")
    }

    private fun bounds(text: String) =
        rule.onNode(hasText(text) and hasClickAction()).fetchSemanticsNode().boundsInRoot

    private fun controlLeft(type: QuestionType) {
        run {
            val st = questionState(type)
            rule.setContent { Scaled(1f) { QuestionScreen(st, QuestionActions()) } }
            val role = if (type == QuestionType.MULTIPLE_CHOICE) Role.Checkbox else Role.RadioButton
            val row = rule.onAllNodes(hasRole(role)).onFirst().fetchSemanticsNode().boundsInRoot
            val txt = rule.onAllNodesWithText(
                if (type == QuestionType.TRUE_FALSE) "Cert" else st.question.options.first().text, useUnmergedTree = true,
            ).onFirst().fetchSemanticsNode().boundsInRoot
            // el text arrenca després del control (ocupa ≥ 28 dp = 84 px a l'esquerra de la fila)
            assertTrue(txt.left - row.left >= 28f * 3, "$type: text a ${txt.left - row.left}px de la vora")
        }
    }

    @Test fun controlLeftSingle() = controlLeft(QuestionType.SINGLE_CHOICE)
    @Test fun controlLeftMultiple() = controlLeft(QuestionType.MULTIPLE_CHOICE)
    @Test fun controlLeftTrueFalse() = controlLeft(QuestionType.TRUE_FALSE)

    /** UI-01: «Anterior» i «Següent» fan EXACTAMENT el mateix ample i alt (±1 px) a 100 %, 130 % i 200 %. */
    private fun assertNavIsFiftyFifty(index: Int, scale: Float, last: Boolean = false) {
        show(questionState(QuestionType.SINGLE_CHOICE, index = index).let { if (last) it.copy(index = 4) else it }, scale = scale)
        val prev = navBounds("Anterior")
        val next = navBounds(if (last) "Revisar" else "Següent")
        assertEquals(prev.width, next.width, 1f, "UI-01 ample a escala $scale (índex $index): ${prev.width} vs ${next.width}")
        assertEquals(prev.height, next.height, 1f, "UI-01 alt a escala $scale (índex $index): ${prev.height} vs ${next.height}")
        assertEquals(prev.top, next.top, 1f, "una fila")
        assertTrue(prev.left < next.left, "Anterior a l'esquerra, Següent a la dreta")
        assertTrue(prev.height >= 52f * 3 - 1f, "botons ≥ 52 dp: ${prev.height}")
    }

    private fun navBounds(text: String) =
        rule.onNode(hasText(text) and hasRole(Role.Button)).fetchSemanticsNode().boundsInRoot

    @Test fun ui01NavButtonsAreFiftyFiftyAtScale100() = assertNavIsFiftyFifty(1, 1f)
    @Test fun ui01NavButtonsAreFiftyFiftyAtScale130() = assertNavIsFiftyFifty(1, 1.3f)
    @Test fun ui01NavButtonsAreFiftyFiftyAtScale200() = assertNavIsFiftyFifty(1, 2f)
    @Test fun ui01FirstQuestionDisabledPreviousHasTheSameBoundsAtScale100() = assertNavIsFiftyFifty(0, 1f)
    @Test fun ui01FirstQuestionDisabledPreviousHasTheSameBoundsAtScale130() = assertNavIsFiftyFifty(0, 1.3f)
    @Test fun ui01FirstQuestionDisabledPreviousHasTheSameBoundsAtScale200() = assertNavIsFiftyFifty(0, 2f)
    @Test fun ui01LastQuestionKeepsFiftyFiftyAtScale100() = assertNavIsFiftyFifty(4, 1f, last = true)
    @Test fun ui01LastQuestionKeepsFiftyFiftyAtScale200() = assertNavIsFiftyFifty(4, 2f, last = true)

    @Test fun navButtonsAreWeightedEquallyAndNeverReplacedByAnIconAtExtraLargeText() {
        show(questionState(QuestionType.SINGLE_CHOICE, index = 1), scale = 2f)
        assertTrue(rule.onAllNodesWithContentDescription("Pregunta anterior").fetchSemanticsNodes().isEmpty(), "sense botó-fletxa")
        rule.onNodeWithText("Anterior").assertIsDisplayed()
        val prev = navBounds("Anterior")
        val next = navBounds("Següent")
        assertEquals(prev.width, next.width, 1f)
    }

    @Test fun lastQuestionOffersReviewAndFinish() {
        show(questionState(QuestionType.SINGLE_CHOICE).copy(index = 4))
        val prev = navBounds("Anterior")
        val next = navBounds("Revisar")
        assertEquals(prev.width, next.width, 1f)
        assertTrue(prev.left < next.left)
    }

    private fun overflows(text: String): Boolean {
        val node = rule.onNode(hasText(text), useUnmergedTree = true).fetchSemanticsNode()
        val r = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        node.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult)?.action?.invoke(r)
        // (hasVisualOverflow no és fiable amb les mètriques de text de Robolectric: es comprova una línia i sense el·lipsi)
        return r.any { it.lineCount != 1 || it.isLineEllipsized(0) }
    }

    private fun assertNoNavOverflow(scale: Float, last: Boolean) {
        rule.setContent { Scaled(scale) { QuestionScreen(questionState(QuestionType.SINGLE_CHOICE, index = if (last) 4 else 1), QuestionActions()) } }
        // «Revisar i finalitzar» pot ocupar dues línies (UX Q6); «Següent» i «Anterior», una de sola.
        if (!last) assertTrue(!overflows("Següent"), "Següent desborda a escala $scale")
        assertTrue(!overflows("Anterior"), "Anterior desborda a escala $scale")
    }

    @Test fun navLabelsDoNotOverflowAt320dpScale1() = assertNoNavOverflow(1f, false)
    @Test fun navLabelsDoNotOverflowAt320dpScale1LastQuestion() = assertNoNavOverflow(1f, true)
    @Test fun navLabelsDoNotOverflowAt320dpScale115() = assertNoNavOverflow(1.15f, false)
    @Test fun navLabelsDoNotOverflowAt320dpScale129() = assertNoNavOverflow(1.29f, false)

    @Test fun multiPanelSitsBelowTheQuestionStem() {
        val st = questionState(QuestionType.MULTIPLE_CHOICE)
        show(st)
        val stem = rule.onNodeWithText(st.question.text).fetchSemanticsNode().boundsInRoot
        val panel = rule.onNodeWithText("Resposta múltiple").fetchSemanticsNode().boundsInRoot
        assertTrue(panel.top >= stem.bottom, "panel ${panel.top} < enunciat ${stem.bottom}")
    }

    // ------------------------------------------------------------------ UI-02: insígnia d'avís amb «!»

    @Test fun ui02MultiBadgeHasAnExclamationIconAndIsNotACheckboxOrClickable() {
        val st = questionState(QuestionType.MULTIPLE_CHOICE)
        show(st)
        val badge = rule.onNodeWithTag(MULTI_BADGE_TAG)
        badge.assertIsDisplayed().assertHasNoClickAction()
        rule.onNodeWithTag(MULTI_BADGE_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        val cfg = badge.fetchSemanticsNode().config
        assertTrue(SemanticsProperties.Role !in cfg, "la insígnia no té rol (ni Checkbox ni RadioButton)")
        assertTrue(SemanticsProperties.Selected !in cfg && SemanticsProperties.ToggleableState !in cfg, "no és seleccionable")
        // cap node amb rol Checkbox/RadioButton DINS de la insígnia
        val inside = rule.onNodeWithTag(MULTI_BADGE_TAG, useUnmergedTree = true).fetchSemanticsNode().children
        assertTrue(inside.none { SemanticsProperties.Role in it.config }, "cap descendent amb rol de casella")
        // només hi ha tantes caselles com opcions: la insígnia no n'és una
        rule.onAllNodes(hasRole(Role.Checkbox)).assertCountEquals(st.question.options.size)
        // el glyph de casella ja no existeix com a recurs
        assertTrue(dev.lluissunol.arbitratge.R.drawable::class.java.fields.none { it.name == "ic_check_box_outline" })
        assertEquals(Color(0xFFF5C443), LightArbColors.warnBadge)
        assertEquals(Color(0xFFF5C443), DarkArbColors.warnBadge)
    }

    @Test fun ui02LongHelperLivesOnlyInTheBadgeContentDescription() {
        show(questionState(QuestionType.MULTIPLE_CHOICE))
        val desc = rule.onNodeWithTag(MULTI_BADGE_TAG).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString(" ")
        assertTrue(desc.contains("Marca-les totes"), desc)
        assertTrue(rule.onAllNodesWithText("Marca-les totes", substring = true).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("Resposta múltiple").assertIsDisplayed()
    }

    @Test fun ui02BadgeSizesToItsContentNotFullWidth() {
        show(questionState(QuestionType.MULTIPLE_CHOICE))
        val badge = rule.onNodeWithTag(MULTI_BADGE_TAG).fetchSemanticsNode().boundsInRoot
        val option = rule.onAllNodes(hasRole(Role.Checkbox)).onFirst().fetchSemanticsNode().boundsInRoot
        assertTrue(badge.width < option.width * 0.8f, "la insígnia no és una fila d'ample complet: ${badge.width} vs ${option.width}")
    }

    @Test fun homeFactStripHasThreeEqualColumnsWithFullLabelsAndFullSpeech() {
        rule.setContent { ArbitratgeTheme { HomeScreen(Stage.Home(RulesSummary(5, 5, 3), null, null), {}, {}, {}) } }
        val labels = listOf("preguntes", "minuts", "encerts per aprovar")
        labels.forEach { rule.onNodeWithText(it, substring = true, useUnmergedTree = true).assertIsDisplayed() }
        assertTrue(rule.onAllNodesWithText("preg.", useUnmergedTree = true).fetchSemanticsNodes().isEmpty(), "sense abreviatures")
        val w = labels.map { rule.onNodeWithText(it, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot }
        assertEquals(w[0].top, w[1].top, 1f, "tres columnes a la mateixa altura")
        assertEquals(w[1].top, w[2].top, 1f)
        rule.onNodeWithContentDescription("5 preguntes, 5 minuts, 3 encerts per aprovar").assertExists()
    }

    // ------------------------------------------------------------------ tema

    @Test fun everyColorRoleIsDefinedNotTheDefaultPurplePalette() {
        fun same(a: List<androidx.compose.ui.graphics.Color>, b: List<androidx.compose.ui.graphics.Color>) = a.zip(b).count { (x, y) -> x == y }
        // només poden coincidir amb els valors per defecte rols neutres (blanc/negre)
        val l = same(LightScheme.allRoles(), lightColorScheme().allRoles())
        val d = same(DarkScheme.allRoles(), darkColorScheme().allRoles())
        assertTrue(l <= 8, "light coincide en $l rols")
        assertTrue(d <= 8, "dark coincide en $d rols")
        assertNotEquals(lightColorScheme().primaryContainer, LightScheme.primaryContainer)
        assertNotEquals(lightColorScheme().secondaryContainer, LightScheme.secondaryContainer)
        assertNotEquals(lightColorScheme().surfaceContainerHigh, LightScheme.surfaceContainerHigh)
        assertNotEquals(lightColorScheme().surfaceVariant, LightScheme.surfaceVariant)
        assertNotEquals(darkColorScheme().primaryContainer, DarkScheme.primaryContainer)
        assertNotEquals(darkColorScheme().surfaceContainerHigh, DarkScheme.surfaceContainerHigh)
        assertEquals(36, LightScheme.allRoles().size)
    }
}

private const val ArbHeightTolerance = 4f
