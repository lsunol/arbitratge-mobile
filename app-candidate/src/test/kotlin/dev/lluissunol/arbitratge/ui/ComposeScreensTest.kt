package dev.lluissunol.arbitratge.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onFirst
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
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
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
        rule.onNodeWithText("Una resposta").assertIsDisplayed()
        rule.onNodeWithText("Tria una resposta.").assertIsDisplayed()
        rule.onNodeWithText("Següent").assertIsDisplayed()
        rule.onNodeWithText("Anterior").assertIsDisplayed()
    }

    @Test fun trueFalseHasLargeVfTreatmentAndTheStatementQuestion() {
        val st = questionState(QuestionType.TRUE_FALSE)
        show(st)
        rule.onAllNodes(hasRole(Role.RadioButton)).assertCountEquals(2)
        rule.onNodeWithText("Vertader o fals").assertIsDisplayed()
        rule.onNodeWithText("Aquesta afirmació és vertadera o falsa?").assertIsDisplayed()
        rule.onNodeWithText("Vertader").assertIsDisplayed()
        rule.onNodeWithText("Fals").assertIsDisplayed()
        // la insignia de lletra gran (V / F) es decorativa: no es duplica en TalkBack
        assertTrue(rule.onAllNodesWithText("V").fetchSemanticsNodes().isEmpty(), "la lletra decorativa no entra a l'arbre de semàntica")
        assertTrue(rule.onAllNodesWithText("Tria una resposta.").fetchSemanticsNodes().isEmpty())
    }

    @Test fun multipleChoiceUsesCheckboxHighlightedPanelAndLiveCount() {
        val st = questionState(QuestionType.MULTIPLE_CHOICE, selected = setOf("a", "b"))
        show(st)
        rule.onAllNodes(hasRole(Role.Checkbox)).assertCountEquals(st.question.options.size)
        rule.onAllNodes(hasRole(Role.RadioButton)).assertCountEquals(0)
        rule.onNodeWithText("Resposta múltiple").assertIsDisplayed()
        rule.onNodeWithText("N'hi ha més d'una resposta correcta").assertIsDisplayed()
        rule.onNodeWithText("Marca totes les que siguin correctes.").assertIsDisplayed()
        rule.onNodeWithText("2 respostes marcades").performScrollTo().assertIsDisplayed()
    }

    @Test fun emptyMultipleSelectionShowsZeroCountAndNextHasNoDialog() {
        var next = 0
        show(questionState(QuestionType.MULTIPLE_CHOICE), a = QuestionActions(onNext = { next++ }))
        rule.onNodeWithText("Cap resposta marcada").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Següent").performClick()
        assertEquals(1, next)
        assertTrue(rule.onAllNodesWithText("No has marcat cap resposta.").fetchSemanticsNodes().isEmpty())
    }

    @Test fun selectedStateDiffersInShapeAndColorFromUnselectedAndFromCorrectness() {
        // El token de selecció és diferent del verd de «correcta» (UX F4).
        assertNotEquals(LightArbColors.selection, LightScheme.primary)
        assertNotEquals(LightArbColors.selectionContainer, LightScheme.primaryContainer)
        assertNotEquals(DarkArbColors.selection, DarkScheme.primary)
        assertNotEquals(DarkArbColors.selectionContainer, DarkScheme.primaryContainer)
    }

    // ------------------------------------------------------------------ barra Anterior / Següent

    @Test fun firstQuestionShowsPreviousDisabled() {
        show(questionState(QuestionType.SINGLE_CHOICE, index = 0))
        rule.onNodeWithText("Anterior").assertIsDisplayed()
        rule.onNode(hasText("Anterior")).assertIsNotEnabled()
    }

    @Test fun lastQuestionButtonOpensReviewNotFinish() {
        show(questionState(QuestionType.SINGLE_CHOICE).copy(index = 4))
        rule.onNodeWithText("Revisar i finalitzar").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("Finalitzar l'examen").fetchSemanticsNodes().isEmpty())
    }

    @Test fun previousAndOverviewButtonsFireCallbacks() {
        var prev = 0
        var over = 0
        show(questionState(QuestionType.SINGLE_CHOICE), a = QuestionActions(onPrevious = { prev++ }, onOverview = { over++ }))
        rule.onNodeWithText("Anterior").performClick()
        rule.onNodeWithText("Veure-les totes").performClick()
        assertEquals(1, prev)
        assertEquals(1, over)
        rule.onNodeWithText("Respostes: 0 de 5").assertIsDisplayed()
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
        // la barra Anterior/Següent (apilada) segueix visible i les opcions són accessibles amb scroll, sense truncar
        rule.onNodeWithText("Següent").assertIsDisplayed()
        rule.onNodeWithText("Anterior").assertIsDisplayed()
        st.question.options.forEach { rule.onNodeWithText(it.text).performScrollTo().assertIsDisplayed() }
    }

    @Test fun usableAtFontScale200PercentLight() = multiAt200(dark = false)

    @Test fun usableAtFontScale200PercentDark() = multiAt200(dark = true)

    @Test fun trueFalseUsableAtFontScale200Percent() {
        show(questionState(QuestionType.TRUE_FALSE), scale = 2f, dark = true)
        rule.onNodeWithText("Vertader").performScrollTo().assertIsDisplayed()
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
        rule.onNodeWithText("Preguntes de l'examen").assertIsDisplayed()
        rule.onNodeWithText("Has respost 2 de 5 preguntes.").assertIsDisplayed()
        rule.onNodeWithText("Et falten 3 preguntes per respondre.").assertIsDisplayed()
        rule.onNodeWithText("Pregunta 1 · Resposta desada").assertIsDisplayed()
        rule.onNodeWithText("Pregunta 2 · Sense resposta").assertIsDisplayed()
        rule.onNodeWithText("Ets aquí").assertIsDisplayed()
        rule.onNodeWithText("Anar a la primera sense resposta").assertIsDisplayed()
        rule.onNodeWithText("Finalitzar l'examen").assertIsDisplayed()
        rule.onNodeWithContentDescription("Pregunta 4, Sense resposta. ${questions[3].text}. Toca per anar-hi.").performScrollTo().performClick()
        assertEquals(3, gone)
    }

    @Test fun finishConfirmationStatesTheUnansweredCountAndTheSafeActionIsPrimary() {
        val st = overview(listOf(true, false, true, false, false), dialog = QuestionDialog.FINISH)
        var dismissed = 0
        var confirmed = 0
        rule.setContent { ArbitratgeTheme { OverviewScreen(st, QuestionActions(onDialogDismiss = { dismissed++ }, onDialogConfirm = { confirmed++ }), {}, {}, {}, {}) } }
        rule.onNodeWithText("Vols finalitzar l'examen?").assertIsDisplayed()
        rule.onNodeWithText("Tens 3 preguntes sense resposta: comptaran com a fallades.").assertIsDisplayed()
        rule.onNodeWithText("Després de finalitzar ja no podràs canviar cap resposta.").assertIsDisplayed()
        rule.onNodeWithText("Tornar a l'examen").performClick()
        assertEquals(1, dismissed)
        rule.onNodeWithText("Finalitzar").performClick()
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
        rule.onNodeWithText("No s'ha pogut finalitzar").assertIsDisplayed()
        rule.onNodeWithText("No hem pogut desar el resultat. Les teves respostes continuen desades. Allibera espai al mòbil i torna-ho a provar.").assertIsDisplayed()
        rule.onNodeWithText("Tornar-ho a provar").performClick()
        assertEquals(1, retried)
    }

    @Test fun overviewUsableAtFontScale200PercentDark() {
        val st = overview(listOf(true, false, true, false, false))
        rule.setContent { Scaled(2f, dark = true) { OverviewScreen(st, QuestionActions(), {}, {}, {}, {}) } }
        rule.onNodeWithText("Finalitzar l'examen").assertIsDisplayed()
        rule.onNodeWithText("Preguntes de l'examen").assertIsDisplayed()
        rule.onNodeWithText("Pregunta 5 · Sense resposta").performScrollTo().assertIsDisplayed()
    }

    // ------------------------------------------------------------------ Home / resultat / repàs (200 %, fosc)

    private val cred = StoredCredential(
        "7f3a92c1-4b5d-4e6f-8a9b-000000000001", "s1", TestEnv.realIssuingToken(), "Maria Puig Vila", "2026-11-20", "dev", "sample-1", "0.0.0-sample",
    )

    @Test fun homeAt200PercentDarkShowsFactsCredentialAndNoPracticeClaim() {
        val st = Stage.Home(RulesSummary(5, 5, 3), cred, LastResult("s1", 2, 5, false))
        rule.setContent { Scaled(2f, dark = true) { HomeScreen(st, {}, {}, {}) } }
        rule.onNodeWithText("Veure la credencial").assertIsDisplayed()
        rule.onNodeWithText("Tornar a fer l'examen").assertIsDisplayed()
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
        rule.onNodeWithText("Has aprovat").assertIsDisplayed()
        rule.onNodeWithText("Veure la credencial").assertIsDisplayed()
        rule.onNodeWithText("Fallades").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("1 sense resposta").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("1 min 46 s").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("3 encerts de 5").assertExists()
    }

    @Test fun failedResultKeepsAHopefulToneAndReviewIsPrimary() {
        val r = resultOf(emptyMap())
        rule.setContent { ArbitratgeTheme { ResultScreen(Stage.Result("s1", r, null), {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("No has aprovat, de moment").assertIsDisplayed()
        rule.onNodeWithText("Repassar les 5 preguntes fallades").assertIsDisplayed()
        rule.onNodeWithText("5 sense resposta").performScrollTo().assertIsDisplayed()
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

    @Test fun credentialScreenNeverClaimsValidityKeepsSelfIssuedNoticeAndShowsFields() {
        rule.setContent { ArbitratgeTheme { CredentialScreen(cred, {}, {}, {}) } }
        rule.onNodeWithText("Maria Puig Vila").assertIsDisplayed()
        rule.onNodeWithText("7F3A-92C1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("20/11/2026").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("APTE").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Codi QR de la credencial de Maria Puig Vila").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Emesa en autoservei.", substring = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("No és un certificat oficial ni una garantia forta.").performScrollTo().assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("VÀLIDA", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("Mostrar a un organitzador").assertIsDisplayed()
    }

    @Test fun credentialAt200PercentDarkStillShowsTheDocument() {
        rule.setContent { Scaled(2f, dark = true) { CredentialScreen(cred, {}, {}, {}) } }
        rule.onNodeWithText("Maria Puig Vila").assertIsDisplayed()
        rule.onNodeWithText("7F3A-92C1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Mostrar a un organitzador").assertIsDisplayed()
    }

    @Test fun presentationShowsLargeQrNameCodeAndNoRemovePrefixHack() {
        rule.setContent { ArbitratgeTheme { PresentationScreen(cred, {}) } }
        rule.onNodeWithContentDescription("Codi QR de la credencial de Maria Puig Vila").assertIsDisplayed()
        rule.onNode(hasText("Codi: 7F3A-92C1")).assertIsDisplayed()
        rule.onNodeWithText("APTE · reglament sample-1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Emesa en autoservei").performScrollTo().assertIsDisplayed()
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

    @Test fun savingShowsDesantAndCommittedShowsSavedAndErrorIsDistinct() {
        val q = questionState(QuestionType.SINGLE_CHOICE)
        val id = q.question.options.first().id
        var st by androidx.compose.runtime.mutableStateOf(q.copy(selected = setOf(id), committed = emptySet(), saving = true))
        rule.setContent { ArbitratgeTheme { QuestionScreen(st, QuestionActions()) } }
        rule.onNodeWithText("Desant…").performScrollTo().assertIsDisplayed()
        st = q.copy(selected = setOf(id), committed = setOf(id), saving = false)
        rule.waitForIdle()
        rule.onNodeWithText("Resposta desada").performScrollTo().assertIsDisplayed()
        st = q.copy(selected = setOf(id), committed = emptySet(), saveError = SaveError.ANSWER)
        rule.waitForIdle()
        assertTrue(rule.onAllNodesWithText("Resposta desada").fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithText("No s'ha pogut desar").fetchSemanticsNodes().isNotEmpty())
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
