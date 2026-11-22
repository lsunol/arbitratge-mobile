package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Componentes de la revisión de look&feel (fase 1): tokens, título en el contenido, pinBottom, InfoBanner, selección. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h640dp-xxhdpi")
class ComponentsTest {
    @get:Rule val rule = createComposeRule()

    @Composable
    private fun Scaled(scale: Float, dark: Boolean = false, content: @Composable () -> Unit) {
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density, scale)) { ArbitratgeTheme(darkTheme = dark, content = content) }
    }

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    // ------------------------------------------------------------------ tokens

    @Test fun lightAndDarkSchemesKeepTheApprovedTokens() {
        assertEquals(Color(0xFF0F3D26), LightScheme.primary) // gespa [DESV-1]
        assertEquals(Color.White, LightScheme.background)
        assertTrue(DarkScheme.background.luminance() < 0.05f, "el fons fosc és verd nit, no blanc")
        assertNotEquals(DarkScheme.background, Color.Black, "no és negre pur")
        assertEquals(DarkScheme.error, DarkArbColors.wrong)
        assertEquals(LightScheme.errorContainer, LightArbColors.wrongContainer)
    }

    @Test fun typographyFollowsTheScaleAndNeverGoesBelow16sp() {
        val t = arbTypography()
        assertEquals(28f, t.headlineSmall.fontSize.value)
        assertEquals(56f, t.displayLarge.fontSize.value)
        assertEquals(18f, t.labelLarge.fontSize.value)
        listOf(t.bodyMedium, t.bodySmall, t.labelMedium, t.labelSmall).forEach { assertTrue(it.fontSize.value >= 16f) }
    }

    // ------------------------------------------------------------------ títol al contingut + barra de 56 dp

    @Test fun screenTitleIsAHeadingInsideTheScrollingContent() {
        rule.setContent { ArbitratgeTheme { ScreenScaffold(title = "Abans de començar", onBack = {}) { Text("Cos") } } }
        rule.onNodeWithText("Abans de començar").assertIsDisplayed()
        rule.onNode(isHeading and androidx.compose.ui.test.hasText("Abans de començar")).assertExists()
        // la barra només porta «Enrere»: el títol no hi és
        rule.onNodeWithContentDescription("Enrere").assertIsDisplayed()
    }

    @Test fun backButtonIsAtLeast48dpTall() {
        rule.setContent { ArbitratgeTheme { ScreenScaffold(title = "T", onBack = {}) { Text("Cos") } } }
        val back = rule.onNodeWithContentDescription("Enrere").fetchSemanticsNode().boundsInRoot
        assertTrue(back.width >= 48f * 3 - 1f && back.height >= 48f * 3 - 1f, "objectiu tàctil ≥ 48 dp: ${back.width}x${back.height}")
    }

    // ------------------------------------------------------------------ pinBottom

    private fun bottomVsContent(scale: Float, pin: Boolean): Pair<Float, Float> {
        rule.setContent {
            Scaled(scale) {
                ScreenScaffold(title = "T", pinBottom = pin, bottom = { PrimaryButton("Acció", {}) }) {
                    repeat(40) { Text("Línia $it") }
                }
            }
        }
        val btn = rule.onNodeWithText("Acció").fetchSemanticsNode().positionInRoot
        // `boundsInRoot` retalla al viewport: es fa servir la posició sense retallar
        val last = rule.onNodeWithText("Línia 39").fetchSemanticsNode()
        return btn.y to (last.positionInRoot.y + last.size.height)
    }

    @Test fun actionsStayPinnedBelowTheViewportAtNormalScaleEvenWhenNotPinned() {
        // pinBottom = false només actua a ≥ 1,5×: a 1× la barra és fixa i el botó és visible sense desplaçar
        val (btnTop, lastBottom) = bottomVsContent(1f, pin = false)
        rule.onNodeWithText("Acció").assertIsDisplayed()
        assertTrue(btnTop < lastBottom, "a 1× el botó va a la barra fixa, no al final del contingut")
    }

    @Test fun actionsMoveToTheEndOfTheContentAtExtraLargeTextWhenNotPinned() {
        val (btnTop, lastBottom) = bottomVsContent(2f, pin = false)
        assertTrue(btnTop >= lastBottom, "a 2× el botó és després de l'última línia (btn $btnTop, línia $lastBottom)")
    }

    @Test fun pinnedActionsStayInTheBarAtExtraLargeText() {
        val (btnTop, lastBottom) = bottomVsContent(2f, pin = true)
        assertTrue(btnTop < lastBottom, "amb pinBottom = true (pregunta, resum, nom) el botó segueix fix")
        rule.onNodeWithText("Acció").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ InfoBanner

    @Test fun everyBannerKindRendersItsText() {
        rule.setContent {
            ArbitratgeTheme {
                Column {
                    BannerKind.entries.forEach { k -> InfoBanner(k) { Text("avís ${k.name}") } }
                }
            }
        }
        BannerKind.entries.forEach { rule.onNodeWithText("avís ${it.name}").assertIsDisplayed() }
    }

    // ------------------------------------------------------------------ selecció (rol natiu, sense color únic)

    @Test fun selectedRadioOptionExposesSelectedStateAndIsNotJustAColour() {
        rule.setContent {
            ArbitratgeTheme {
                Column {
                    OptionCard("Marcada", selected = true, kind = OptionKind.RADIO, enabled = true, onClick = {})
                    OptionCard("Buida", selected = false, kind = OptionKind.RADIO, enabled = true, onClick = {})
                }
            }
        }
        rule.onNodeWithText("Marcada").assertIsSelected()
        rule.onNodeWithText("Buida").assertIsNotSelected()
        val roles = listOf("Marcada", "Buida").map {
            rule.onNodeWithText(it).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Role)
        }
        assertEquals(listOf(Role.RadioButton, Role.RadioButton), roles)
    }

    @Test fun checkOptionKeepsCheckboxToggleState() {
        rule.setContent {
            ArbitratgeTheme {
                Column {
                    OptionCard("A", selected = true, kind = OptionKind.CHECK, enabled = true, onClick = {})
                    OptionCard("B", selected = false, kind = OptionKind.CHECK, enabled = true, onClick = {})
                }
            }
        }
        rule.onNodeWithText("A").assertIsOn()
        rule.onNodeWithText("B").assertIsOff()
    }

    @Test fun optionCardKeepsItsSizeWhenSelected() {
        rule.setContent {
            ArbitratgeTheme {
                Column {
                    OptionCard("Mateixa", selected = false, kind = OptionKind.RADIO, enabled = true, onClick = {})
                    OptionCard("Mateixa 2", selected = true, kind = OptionKind.RADIO, enabled = true, onClick = {})
                }
            }
        }
        val a = rule.onNodeWithText("Mateixa").fetchSemanticsNode().boundsInRoot
        val b = rule.onNodeWithText("Mateixa 2").fetchSemanticsNode().boundsInRoot
        assertEquals(a.height, b.height, 1f, "la vora de 3 dp es dibuixa cap a dins: la targeta no creix")
        assertEquals(a.width, b.width, 1f)
    }

    // ------------------------------------------------------------------ fase 2: banda, FactStrip, ActionRow, normes

    @Test fun brandBandShowsTitleAsHeadingAndSubtitleAt200Percent() {
        rule.setContent { Scaled(2f) { BrandBand("Arbitratge", "Subtítol") } }
        rule.onNode(isHeading and androidx.compose.ui.test.hasText("Arbitratge")).assertExists()
        rule.onNodeWithText("Subtítol").assertIsDisplayed()
    }

    @Test fun factStripIsColumnsAtScale1() {
        val cells = listOf(FactCell("5", "preguntes"), FactCell("5", "minuts"))
        rule.setContent { Scaled(1f) { FactStrip(cells, "x") } }
        val a1 = rule.onNodeWithText("preguntes", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val b1 = rule.onNodeWithText("minuts", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(a1.top, b1.top, 1f, "columnes a 1×")
    }

    @Test fun factStripStacksVerticallyAtScale130() {
        val cells = listOf(FactCell("5", "preguntes"), FactCell("5", "minuts"))
        rule.setContent { Scaled(1.3f) { FactStrip(cells, "x") } }
        val a = rule.onNodeWithText("preguntes", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val b = rule.onNodeWithText("minuts", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(b.top > a.top, "files a ≥ 1,3×")
    }

    @Test fun actionRowIsAButtonAndFires() {
        var n = 0
        rule.setContent { ArbitratgeTheme { ActionRow(drawable(dev.lluissunol.arbitratge.R.drawable.ic_menu_book), "Repassar", null, { n++ }) } }
        rule.onNode(SemanticsMatcher("rol botó") { it.config.getOrNull(SemanticsProperties.Role) == Role.Button }).performClick()
        assertEquals(1, n)
        val h = rule.onNodeWithText("Repassar").fetchSemanticsNode().let { rule.onNode(SemanticsMatcher("fila") { n2 -> n2.config.getOrNull(SemanticsProperties.Role) == Role.Button }).fetchSemanticsNode().boundsInRoot.height }
        assertTrue(h >= 72f * 3 - 1f, "fila ≥ 72 dp: $h")
    }

    @Test fun welcomeOffersContinuarInsteadOfEntesos() {
        rule.setContent { ArbitratgeTheme { WelcomeScreen {} } }
        rule.onNodeWithText("Continuar").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("Entesos").fetchSemanticsNodes().isEmpty())
    }

    @Test fun rulesShowFactStripFourRulesAndTheAmberLeaveWarning() {
        val st = Stage.Rules(summary = RulesSummary(5, 5, 3), nom = "Maria", starting = false, saveError = false)
        rule.setContent { ArbitratgeTheme { RulesScreen(st, {}, {}, {}) } }
        rule.onNodeWithText("Abans de començar").assertIsDisplayed()
        rule.onNodeWithText("encerts per aprovar", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Si surts de l'app, el temps continua passant.").assertExists()
        rule.onNodeWithText("Maria").assertExists()
        rule.onNodeWithText("Canviar").assertExists()
    }

    // ------------------------------------------------------------------ fase 3: fons fix sota la barra d'estat

    @Test fun statusCapKeepsTheBandColourOnlyWhileTheBandCoversTheStatusBar() {
        // banda de 300 px, barra d'estat de 72 px
        assertTrue(isBandBehindStatusBar(300, 0, 72), "en repòs")
        assertTrue(isBandBehindStatusBar(300, 228, 72), "just abans de deixar la barra al descobert")
        assertTrue(!isBandBehindStatusBar(300, 229, 72), "la banda ja no cobreix la barra: fons de pantalla")
        assertTrue(!isBandBehindStatusBar(300, 1000, 72), "contingut desplaçat (200 %): mai per sota de l'hora")
        assertTrue(isBandBehindStatusBar(0, 0, 72), "primer frame, banda sense mesurar: es tracta com a darrere (sense parpelleig)")
    }

    @Test fun scaffoldWithAHeaderDrawsAFixedStatusCap() {
        rule.setContent { ArbitratgeTheme { ScreenScaffold(title = null, header = { Text("Banda") }) { Text("Cos") } } }
        rule.onNodeWithTag(STATUS_CAP_TAG).assertExists()
    }

    @Test fun scaffoldWithoutHeaderHasNoStatusCap() {
        rule.setContent { ArbitratgeTheme { ScreenScaffold(title = "T") { Text("Cos") } } }
        rule.onAllNodesWithTag(STATUS_CAP_TAG).assertCountEquals(0)
    }
}
