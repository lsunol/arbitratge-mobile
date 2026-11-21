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

/** Rols M3 COMPLETS (cap rol queda al valor per defecte, que és el lila de Google). */
internal val LightScheme = lightColorScheme(
    primary = Color(0xFF1D6F42), onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EEDF), onPrimaryContainer = Color(0xFF0A3A20),
    inversePrimary = Color(0xFF86D7A4),
    secondary = Color(0xFF3F5F4B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E6D9), onSecondaryContainer = Color(0xFF0F2A1A),
    tertiary = Color(0xFF7A5900), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE08A), onTertiaryContainer = Color(0xFF2B1F00),
    background = Color(0xFFF2F5F3), onBackground = Color(0xFF18241D),
    surface = Color.White, onSurface = Color(0xFF18241D),
    surfaceVariant = Color(0xFFDFE7E1), onSurfaceVariant = Color(0xFF4A5750),
    surfaceTint = Color(0xFF1D6F42),
    inverseSurface = Color(0xFF2B332E), inverseOnSurface = Color(0xFFEEF2EF),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFBE3E0), onErrorContainer = Color(0xFF5C0E09),
    outline = Color(0xFF6B7770), outlineVariant = Color(0xFFC5CFC8), scrim = Color.Black,
    surfaceBright = Color.White, surfaceDim = Color(0xFFD8DFDA),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F9F7),
    surfaceContainer = Color(0xFFEEF2EF), surfaceContainerHigh = Color(0xFFE8EDE9),
    surfaceContainerHighest = Color(0xFFE1E8E3),
)

internal val DarkScheme = darkColorScheme(
    primary = Color(0xFF86D7A4), onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF1A5235), onPrimaryContainer = Color(0xFFC6EED3),
    inversePrimary = Color(0xFF1D6F42),
    secondary = Color(0xFFB5CCBC), onSecondary = Color(0xFF213528),
    secondaryContainer = Color(0xFF2E4637), onSecondaryContainer = Color(0xFFD3E6D9),
    tertiary = Color(0xFFF0C64A), onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5B4300), onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF0F1512), onBackground = Color(0xFFE2EAE4),
    surface = Color(0xFF151C18), onSurface = Color(0xFFE2EAE4),
    surfaceVariant = Color(0xFF3A453E), onSurfaceVariant = Color(0xFFBAC6BE),
    surfaceTint = Color(0xFF86D7A4),
    inverseSurface = Color(0xFFE2EAE4), inverseOnSurface = Color(0xFF2B332E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF87938B), outlineVariant = Color(0xFF3A453E), scrim = Color.Black,
    surfaceBright = Color(0xFF333B36), surfaceDim = Color(0xFF0F1512),
    surfaceContainerLowest = Color(0xFF0B100D), surfaceContainerLow = Color(0xFF151C18),
    surfaceContainer = Color(0xFF1A221D), surfaceContainerHigh = Color(0xFF222B26),
    surfaceContainerHighest = Color(0xFF2C3530),
)

/** Colors de marca fora dels rols M3 (especificació de marca §3). */
object BrandColors {
    val Pitch = Color(0xFF0F3D26)
    val PitchLine = Color.White.copy(alpha = 0.16f)
    val Chalk = Color(0xFFF2F5F3)
    val RefCard = Color(0xFFF2C230)
    val Green = Color(0xFF1D6F42)

    /** El carnet és SEMPRE paper clar (document, QR sobre blanc, igual que el PDF). */
    val Paper = Color.White
    val PaperInk = Color(0xFF18241D)
    val PaperMuted = Color(0xFF4A5750)
    val PaperLine = Color(0xFFC5CFC8)
    val PaperFoot = Color(0xFFEEF2EF)

    /** Avís de temps (5 min): ambre als dos temes, perquè destaqui. */
    val WarnAmber = Color(0xFFFFE08A)
    val OnWarnAmber = Color(0xFF2B1F00)
}

/**
 * Colors semàntics de l'examen. La selecció (que NO és un veredicte) és neutra-blava; el verd queda reservat per a
 * «correcta» i el vermell per a «incorrecta» (UX F4: «marcada» no pot confondre's amb «encertada»).
 */
@Immutable
class ArbColors(
    val selection: Color,
    val selectionContainer: Color,
    val correct: Color,
    val correctContainer: Color,
    val onCorrectContainer: Color,
)

internal val LightArbColors = ArbColors(
    selection = Color(0xFF2B5C8A), selectionContainer = Color(0xFFE3EEF9),
    correct = Color(0xFF1D6F42), correctContainer = Color(0xFFD7EEDF), onCorrectContainer = Color(0xFF0A3A20),
)
internal val DarkArbColors = ArbColors(
    selection = Color(0xFF9DC3EA), selectionContainer = Color(0xFF1F3447),
    correct = Color(0xFF86D7A4), correctContainer = Color(0xFF1A5235), onCorrectContainer = Color(0xFFC6EED3),
)

val LocalArbColors = staticCompositionLocalOf { LightArbColors }

/** Retícula de 8 dp i mides tàctils (4 §8.1). */
object ArbDimens {
    val gutter = 20.dp
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val sectionGap = 24.dp
    val itemGap = 12.dp
    val touchMin = 48.dp
    val buttonMin = 56.dp
    val optionMin = 64.dp
    val optionMinTf = 80.dp
    val indicator = 28.dp
    val iconInline = 24.dp
    val iconBadge = 40.dp
    val progressHeight = 8.dp
}

val ArbShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
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
    fun t(size: Int, line: Int, weight: Int) =
        TextStyle(fontFamily = Atkinson, fontSize = size.sp, lineHeight = line.sp, fontWeight = FontWeight(weight))
    return Typography(
        displayLarge = t(44, 52, 700), displayMedium = t(40, 48, 700), displaySmall = t(34, 42, 700),
        headlineLarge = t(30, 38, 800), headlineMedium = t(28, 36, 700), headlineSmall = t(24, 32, 600),
        titleLarge = t(22, 28, 600), titleMedium = t(20, 28, 400), titleSmall = t(18, 26, 600),
        bodyLarge = t(18, 27, 400), bodyMedium = t(16, 24, 400), bodySmall = t(16, 24, 400),
        labelLarge = t(18, 24, 600), labelMedium = t(16, 20, 500), labelSmall = t(16, 20, 500),
    )
}

/** Estils propis fora de `Typography`. */
object ArbText {
    /** L'enunciat és el més gran de l'examen (la jerarquia ja no la lidera la capçalera). */
    val question = TextStyle(fontFamily = Atkinson, fontSize = 24.sp, lineHeight = 33.sp, fontWeight = FontWeight(600))
    val progressLabel = TextStyle(fontFamily = Atkinson, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight(600))
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
