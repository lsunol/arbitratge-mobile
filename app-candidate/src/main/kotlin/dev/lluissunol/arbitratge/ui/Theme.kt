package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lluissunol.arbitratge.R

// ---------------------------------------------------------------------------------------------
// Identitat visual provisional «Centre del camp» (ruta A) — pendent d'aprovació de CFBB.
// Paleta, tipografia i formes: docs/design/reviews/visual-review-app-m1.md §2; marca: docs/brand.
// Sense color dinàmic (04 §8.1): la identitat no ha de canviar amb el fons de pantalla.
// ---------------------------------------------------------------------------------------------

/** Rols M3 COMPLETS (cap rol queda al valor per defecte, que és el lila de Google). Tokens: ui-designer-spec §3.1. */
internal val LightScheme = lightColorScheme(
    primary = Color(0xFF0F3D26), onPrimary = Color.White, // [DESV-1] gespa com a botó primari
    primaryContainer = Color(0xFFDCEBE1), onPrimaryContainer = Color(0xFF0F3D26),
    inversePrimary = Color(0xFFA6DDB8),
    secondary = Color(0xFF1D6F42), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1EFE5), onSecondaryContainer = Color(0xFF0F3D26),
    tertiary = Color(0xFF7A5900), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08A), onTertiaryContainer = Color(0xFF2B1F00),
    background = Color.White, onBackground = Color(0xFF18241D),
    surface = Color.White, onSurface = Color(0xFF18241D),
    surfaceVariant = Color(0xFFE3EAE5), onSurfaceVariant = Color(0xFF3F4B44),
    surfaceTint = Color(0xFF0F3D26),
    inverseSurface = Color(0xFF18241D), inverseOnSurface = Color(0xFFF2F5F3),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFCE4E1), onErrorContainer = Color(0xFF5C0E09),
    outline = Color(0xFF66736B), outlineVariant = Color(0xFFD3DCD5), scrim = Color.Black,
    surfaceBright = Color.White, surfaceDim = Color(0xFFDCE3DE),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F9F7),
    surfaceContainer = Color(0xFFF2F5F3), surfaceContainerHigh = Color(0xFFE9EEEA),
    surfaceContainerHighest = Color(0xFFDEE5E0),
)

/** Tema fosc «nit de camp» (§3.2): fons verd nit, no negre; botons de cal, no menta. */
internal val DarkScheme = darkColorScheme(
    primary = Color(0xFFE3F1E7), onPrimary = Color(0xFF0F3D26),
    primaryContainer = Color(0xFF1B4A31), onPrimaryContainer = Color(0xFFD5EEDD),
    inversePrimary = Color(0xFF0F3D26),
    secondary = Color(0xFF9FD8B1), onSecondary = Color(0xFF0A3A20),
    secondaryContainer = Color(0xFF1B4A31), onSecondaryContainer = Color(0xFFD5EEDD),
    tertiary = Color(0xFFF0C64A), onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFFFFE08A), onTertiaryContainer = Color(0xFF2B1F00),
    background = Color(0xFF0C1A12), onBackground = Color(0xFFE9F0EA),
    surface = Color(0xFF12241A), onSurface = Color(0xFFE9F0EA),
    surfaceVariant = Color(0xFF223C2D), onSurfaceVariant = Color(0xFFBCCBC0),
    surfaceTint = Color(0xFFE3F1E7),
    inverseSurface = Color(0xFFE9F0EA), inverseOnSurface = Color(0xFF18241D),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF4E1A16), onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8DA194), outlineVariant = Color(0xFF2C4536), scrim = Color.Black,
    surfaceBright = Color(0xFF2A4535), surfaceDim = Color(0xFF0C1A12),
    surfaceContainerLowest = Color(0xFF08130D), surfaceContainerLow = Color(0xFF0F2017),
    surfaceContainer = Color(0xFF152A1E), surfaceContainerHigh = Color(0xFF1B3325),
    surfaceContainerHighest = Color(0xFF223C2D),
)

/** Colors de marca fora dels rols M3 (especificació de marca §3; ui-designer-spec §3.3). */
object BrandColors {
    val Pitch = Color(0xFF0F3D26)

    /** Línies de cal decoratives (a α 0,18 sobre la gespa). */
    val Chalk = Color(0xFFFFFFFF)
    val PitchSubtitle = Color(0xFFE2E8E5)
    val RefCard = Color(0xFFF2C230)
    val Green = Color(0xFF1D6F42)

    /** El carnet és SEMPRE paper clar (document, QR sobre blanc, igual que el PDF). */
    val Paper = Color.White
    val PaperInk = Color(0xFF18241D)
    val PaperMuted = Color(0xFF3F4B44)
    val PaperLine = Color(0xFFC5CFC8)
    val PaperFoot = Color(0xFFEEF2EF)

    /** Xip «APTE» del carnet (sempre sobre paper blanc, també amb tema fosc). */
    val PaperChip = Color(0xFFDCEBE1)

    /** Avís de temps (5 min): ambre als dos temes, perquè destaqui. */
    val WarnAmber = Color(0xFFFFE08A)
    val OnWarnAmber = Color(0xFF2B1F00)
}

/**
 * Colors semàntics de l'examen. «Marca de bolígraf»: el que l'usuari marca es pinta en tinta ([mark]), que NO és un
 * veredicte; el verd queda per a «correcta» i el vermell per a «incorrecta» (UX F4). Sense blau.
 */
@Immutable
class ArbColors(
    val mark: Color,
    val onMark: Color,
    val correct: Color,
    val correctContainer: Color,
    val onCorrectContainer: Color,
    val wrong: Color,
    val wrongContainer: Color,
    val onWrongContainer: Color,
)

internal val LightArbColors = ArbColors(
    mark = Color(0xFF18241D), onMark = Color.White,
    correct = Color(0xFF1D6F42), correctContainer = Color(0xFFDDF0E3), onCorrectContainer = Color(0xFF0A3A20),
    wrong = LightScheme.error, wrongContainer = LightScheme.errorContainer, onWrongContainer = LightScheme.onErrorContainer,
)
internal val DarkArbColors = ArbColors(
    mark = Color(0xFFE9F0EA), onMark = Color(0xFF0C1A12),
    correct = Color(0xFF8FD6A6), correctContainer = Color(0xFF163F27), onCorrectContainer = Color(0xFFCFEFDA),
    wrong = DarkScheme.error, wrongContainer = DarkScheme.errorContainer, onWrongContainer = DarkScheme.onErrorContainer,
)

val LocalArbColors = staticCompositionLocalOf { LightArbColors }

/** Retícula de 8 dp i mides tàctils (4 §8.1). Ritme: 8 dins un component, 16 dins un grup, 32 entre grups. */
object ArbDimens {
    val gutter = 20.dp
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val sectionGap = 32.dp
    val itemGap = 16.dp
    val touchMin = 48.dp
    val buttonMin = 56.dp
    val optionMin = 64.dp
    val optionMinTf = 96.dp
    val rowMin = 72.dp
    val indicator = 28.dp
    val iconInline = 24.dp
    val iconBadge = 40.dp
    val progressHeight = 8.dp
    val topBar = 56.dp
}

/** *Shape lock* (§3.5): controls 12, blocs 20, diàlegs 28; píndola només per al rellotge i els xips d'estat. */
val ArbShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun next(weight: Int) = Font(
    R.font.atkinson_next, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun mono(weight: Int) = Font(
    R.font.atkinson_mono, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Atkinson Hyperlegible Next (OFL 1.1), variable, empaquetada: sense descàrregues. */
val Atkinson = FontFamily(next(400), next(500), next(600), next(700), next(800))
val AtkinsonMono = FontFamily(mono(400), mono(600))

/** Cap rol baixa de 16 sp (04 §8.1). */
internal fun arbTypography(): Typography {
    fun t(size: Int, line: Int, weight: Int, tnum: Boolean = false) =
        TextStyle(
            fontFamily = Atkinson, fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight(weight),
            fontFeatureSettings = if (tnum) "tnum" else null,
        )
    return Typography(
        displayLarge = t(56, 64, 700, tnum = true), displayMedium = t(40, 48, 700), displaySmall = t(34, 42, 700),
        headlineLarge = t(34, 40, 800), headlineMedium = t(30, 38, 700), headlineSmall = t(28, 36, 700),
        titleLarge = t(24, 32, 700), titleMedium = t(20, 28, 600), titleSmall = t(18, 26, 700),
        bodyLarge = t(18, 28, 400), bodyMedium = t(16, 24, 400), bodySmall = t(16, 24, 400),
        labelLarge = t(18, 24, 700), labelMedium = t(16, 22, 600), labelSmall = t(16, 22, 600),
    )
}

/** Estils propis fora de `Typography`. */
object ArbText {
    /** L'enunciat és el més gran de l'examen (la jerarquia ja no la lidera la capçalera). */
    val question = TextStyle(fontFamily = Atkinson, fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight(600))
    val progressLabel = TextStyle(fontFamily = Atkinson, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight(700))
    val option = TextStyle(fontFamily = Atkinson, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight(400))
    val optionTf = TextStyle(fontFamily = Atkinson, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight(600))
    val timer = TextStyle(
        fontFamily = AtkinsonMono, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight(600),
        fontFeatureSettings = "tnum",
    )
    val code = TextStyle(
        fontFamily = AtkinsonMono, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight(600),
        letterSpacing = 1.sp,
    )
}

@Composable
fun ArbitratgeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalArbColors provides if (darkTheme) DarkArbColors else LightArbColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = arbTypography(),
            shapes = ArbShapes,
            content = content,
        )
    }
}

val MaterialTheme.arb: ArbColors
    @Composable get() = LocalArbColors.current

/** Tots els rols M3 del esquema (per al test que impedeix que torni el lila per defecte). */
internal fun ColorScheme.allRoles(): List<Color> = listOf(
    primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary, secondary, onSecondary, secondaryContainer,
    onSecondaryContainer, tertiary, onTertiary, tertiaryContainer, onTertiaryContainer, background, onBackground, surface,
    onSurface, surfaceVariant, onSurfaceVariant, surfaceTint, inverseSurface, inverseOnSurface, error, onError,
    errorContainer, onErrorContainer, outline, outlineVariant, scrim, surfaceBright, surfaceDim, surfaceContainerLowest,
    surfaceContainerLow, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest,
)
