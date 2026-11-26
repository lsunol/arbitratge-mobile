package dev.lluissunol.arbitratge.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import dev.lluissunol.arbitratge.R
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Línies de camp de la banda (I3): cercle central de r 44 dp CONCÈNTRIC amb el símbol i línia de mig camp vertical que
 * passa pel seu centre, en dos trams (a dalt i a baix del cercle). Cal α 0,18, traç 1,5 dp, decoratiu. S'aplica al
 * contenidor del símbol (el seu centre és el centre del cercle) i la banda ho retalla. [onlyTop]: només el tram superior. [upLength]: llargada del tram superior (px) des del centre; la banda el limita per sota de la barra d'estat perquè la línia no creui l'hora.
 */
fun Modifier.centreLines(onlyTop: Boolean = false, upLength: Float = 4000f): Modifier = this.drawBehind {
    val w = 1.5.dp.toPx()
    val c = Offset(size.width / 2, size.height / 2)
    val r = 44.dp.toPx()
    val color = BrandColors.Chalk.copy(alpha = 0.18f)
    drawCircle(color, radius = r, center = c, style = Stroke(w))
    drawLine(color, Offset(c.x, c.y - maxOf(upLength, r)), Offset(c.x, c.y - r), strokeWidth = w)
    if (!onlyTop) drawLine(color, Offset(c.x, c.y + r), Offset(c.x, c.y + 4000f), strokeWidth = w)
}

/** Icones de la barra d'estat en clar mentre hi ha una banda de gespa darrere; en sortir es restaura l'estat previ. */
@Composable
fun LightStatusBarIcons(light: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(view, light) {
        val window = (view.context as? Activity)?.window
        if (window == null || !light) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previous = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = false
            onDispose { controller.isAppearanceLightStatusBars = previous }
        }
    }
}

/**
 * Banda de marca «Centre del camp» (I3): gespa a sangre, per darrere de la barra d'estat (els icones de la barra van en
 * clar mentre és a la vista). Símbol de 64 dp + «Arbitratge» + subtítol; amb lletra ≥ 1,5× el símbol va a dalt. Amb
 * [minHeightFraction] > 0 (només a l'inici i amb lletra < 1,3×) té una alçada mínima i el contingut centrat.
 */
@Composable
fun BrandBand(title: String, subtitle: String, modifier: Modifier = Modifier, minHeightFraction: Float = 0f) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // Los iconos de la barra de estado los gestiona ScreenScaffold (según la banda esté o no tras la barra).
    val stacked = isExtraLargeText()
    val insetPx = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
    var bandCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var up by remember { mutableFloatStateOf(4000f) }
    val markBox = Modifier.size(64.dp).onGloballyPositioned { m ->
        val b = bandCoords
        if (b != null && b.isAttached && m.isAttached) up = b.localPositionOf(m, Offset.Zero).y + m.size.height / 2f - insetPx
    }
    val minH = if (minHeightFraction > 0f && !isLargeText()) (LocalConfiguration.current.screenHeightDp * minHeightFraction).dp else 0.dp
    Box(
        modifier.fillMaxWidth().background(BrandColors.Pitch).heightIn(min = minH).clipToBounds()
            .onGloballyPositioned { bandCoords = it }
            .then(
                if (dark) Modifier.drawBehind {
                    // Filet inferior d'1 dp (cal α 0,24): sobre el fons verd nit la banda només dóna 1,46:1.
                    val y = size.height - 0.5.dp.toPx()
                    drawLine(BrandColors.Chalk.copy(alpha = 0.24f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                } else Modifier,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val pad = Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(start = ArbDimens.gutter, end = ArbDimens.gutter, top = ArbDimens.s24, bottom = 28.dp)
        val text: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(ArbDimens.s8)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = Color.White, modifier = Modifier.semantics { heading() })
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = BrandColors.PitchSubtitle)
            }
        }
        if (stacked) {
            Column(pad, verticalArrangement = Arrangement.spacedBy(ArbDimens.s16)) {
                Box(markBox.centreLines(onlyTop = true, upLength = up)) { BrandMark(64.dp, onPitch = true) }
                text(Modifier.fillMaxWidth())
            }
        } else {
            Row(pad, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Box(markBox.centreLines(upLength = up)) { BrandMark(64.dp, onPitch = true) }
                text(Modifier.weight(1f))
            }
        }
    }
}

/** Una cel·la de [FactStrip]: valor gran i etiqueta completa. */
class FactCell(val value: String, val label: String)

/**
 * Bloc únic de dades (I4): un sol bloc `surfaceContainer` de radi 20, sense vora. Amb lletra < 1,3× són columnes
 * iguals separades per filets verticals; a partir d'1,3× són files (valor a l'esquerra, etiqueta a la dreta) amb filets
 * horitzontals. [spoken] és la descripció fusionada per a TalkBack.
 */
@Composable
fun FactStrip(cells: List<FactCell>, spoken: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val cardShape = MaterialTheme.shapes.large
    // UI-05: bloc blanc amb ombra suau (`surfaceContainer` sobre el fons nou quedaria invisible).
    Surface(
        modifier.fillMaxWidth().softShadow(cardShape).cardBorder(cardShape).semantics(mergeDescendants = true) { contentDescription = spoken },
        shape = cardShape, color = MaterialTheme.arb.card,
    ) {
        if (isLargeText()) {
            // Columna de valors d'amplada comuna (el valor més ample, mínim 56 dp): totes les etiquetes comencen a la mateixa x.
            val measurer = androidx.compose.ui.text.rememberTextMeasurer()
            val style = MaterialTheme.typography.headlineSmall
            val density = LocalDensity.current
            val valueW = with(density) { maxOf(56.dp, cells.maxOf { measurer.measure(it.value, style).size.width }.toDp()) }
            Column {
                cells.forEachIndexed { i, c ->
                    if (i > 0) HorizontalDivider(color = scheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = ArbDimens.buttonMin).padding(ArbDimens.s16),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ArbDimens.s16),
                    ) {
                        Text(c.value, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface, modifier = Modifier.width(valueW))
                        Text(c.label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                cells.forEachIndexed { i, c ->
                    if (i > 0) VerticalDivider(color = scheme.outlineVariant)
                    Column(Modifier.weight(1f).padding(ArbDimens.s16), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
                        Text(c.value, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
                        Text(c.label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Fila tocable de 72 dp (UI-05): targeta blanca de radi 18, ombra suau, sense vora; requadre d'icona i xeró. */
@Composable
fun ActionRow(icon: Painter, title: String, support: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = ArbShape.option
    Row(
        modifier.fillMaxWidth().softShadow(shape, ShadowLevel.Option).heightIn(min = ArbDimens.rowMin).clip(shape)
            .background(MaterialTheme.arb.card, shape).cardBorder(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = ArbDimens.s16, vertical = ArbDimens.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            if (support != null) Text(support, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        ArbIcon(vector(Icons.AutoMirrored.Outlined.KeyboardArrowRight), null, tint = scheme.onSurfaceVariant)
    }
}

/**
 * Banda del carnet (I8): gespa, mínim 88 dp, símbol de 48 dp amb les línies de camp, «Credencial d'aptitud» (24 sp) i el
 * subtítol, i a sota un **filet groc de 4 dp** (`RefCard`, ús exclusiu del carnet). Amb lletra ≥ 1,5× el símbol va a
 * dalt (sense línies, que tocarien el text). [topInset]: a sang per darrere de la barra d'estat (mode presentació);
 * [compact]: sense subtítol (presentació). No és cap marca de validesa.
 */
@Composable
fun CredentialBand(modifier: Modifier = Modifier, topInset: Boolean = false, compact: Boolean = false) {
    if (topInset) LightStatusBarIcons()
    val stacked = isExtraLargeText()
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().background(BrandColors.Pitch).heightIn(min = 88.dp).clipToBounds(),
            contentAlignment = Alignment.CenterStart,
        ) {
            val pad = (if (topInset) Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)) else Modifier)
                .padding(horizontal = ArbDimens.gutter, vertical = ArbDimens.gutter)
            val text: @Composable (Modifier) -> Unit = { m ->
                Column(m) {
                    Text(
                        stringResource(R.string.cred_title), style = MaterialTheme.typography.titleLarge, color = Color.White,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (!compact) Text(stringResource(R.string.cred_subtitle), style = MaterialTheme.typography.bodyMedium, color = BrandColors.PitchSubtitle)
                }
            }
            if (stacked) {
                Column(pad, verticalArrangement = Arrangement.spacedBy(ArbDimens.s12)) {
                    BrandMark(48.dp, onPitch = true)
                    text(Modifier.fillMaxWidth())
                }
            } else {
                Row(pad, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    // A sang (presentació) sense línies de camp: la vertical creuaria l'hora de la barra d'estat.
                    Box(if (topInset) Modifier.size(48.dp) else Modifier.size(48.dp).centreLines()) { BrandMark(48.dp, onPitch = true) }
                    text(Modifier.weight(1f))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(BrandColors.RefCard))
    }
}
