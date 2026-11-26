package dev.lluissunol.arbitratge.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lluissunol.arbitratge.R

// ---------------------------------------------------------------------------------------------
// Components del nou estil (UI-05; docs/design/reviews/ui-nou-estil-2026-11-24/ui-designer-spec.md)
// ---------------------------------------------------------------------------------------------

/** Progrés segmentat de 6 dp (§3.2). Decoratiu: «Pregunta N de M» ja ho diu el text. [current] = índex 0-based. */
@Composable
fun SegmentedProgress(current: Int, total: Int, modifier: Modifier = Modifier, continuous: Boolean = false) {
    val arb = MaterialTheme.arb
    val done by animateColorAsState(arb.progressDone, if (reduceMotion()) snap() else tween(200), label = "seg")
    Canvas(modifier.height(ArbDimens.progressSeg).clearAndSetSemantics { }) {
        val gap = 4.dp.toPx()
        val r = CornerRadius(3.dp.toPx())
        val seg = (size.width - gap * (total - 1)) / total
        if (continuous || total <= 1 || seg < 6.dp.toPx()) {
            drawRoundRect(arb.progressTodo, cornerRadius = r)
            drawRoundRect(done, size = Size(size.width * (current + 1) / total, size.height), cornerRadius = r)
        } else {
            for (i in 0 until total) {
                drawRoundRect(
                    if (i <= current) done else arb.progressTodo,
                    topLeft = Offset(i * (seg + gap), 0f), size = Size(seg, size.height), cornerRadius = r,
                )
            }
        }
    }
}

/** Requadre d'icona (radi 14, `iconTile`), decoratiu: el text del costat ja diu el mateix. */
@Composable
fun IconTile(painter: Painter, modifier: Modifier = Modifier, size: Dp = ArbDimens.iconTile) {
    val arb = MaterialTheme.arb
    Box(
        modifier.size(size).background(arb.iconTile, ArbShape.iconTile).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) { ArbIcon(painter, null, tint = arb.onIconTile, size = if (size >= 44.dp) 22.dp else 20.dp) }
}

/** Fila d'una llista de característiques: requadre d'icona + títol + subtítol opcional. */
@Composable
fun FeatureRow(icon: Painter, title: String, modifier: Modifier = Modifier, subtitle: String? = null, tileSize: Dp = ArbDimens.iconTile) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconTile(icon, size = tileSize)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title, style = if (subtitle != null) ArbText.featureTitle else MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Fila de marca de 56 dp (substitueix la banda a sang): símbol de 36 dp + «Arbitratge». Respecta la barra d'estat. */
@Composable
fun BrandTopRow() {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = ArbDimens.topBar).padding(horizontal = ArbDimens.gutter, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BrandMark(36.dp, onPitch = dark)
        Text(stringResource(R.string.brand_name), style = ArbText.brandWord, color = MaterialTheme.colorScheme.primary)
    }
}

/** Les tres dades del hero. */
class HeroFact(val value: String, val label: String)

/**
 * Hero verd de gespa (radi 28) amb el «centre del camp» decoratiu i, opcionalment, 3 dades. El antetítol va en `#D7E6DC`,
 * no en groc (Q1). Amb lletra gran o paraules que no hi caben, les dades passen a files «valor · etiqueta» (mai es parteix una paraula).
 */
@Composable
fun HeroCard(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    body: String? = null,
    facts: List<HeroFact>? = null,
    factsSpoken: String = "",
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val line = MaterialTheme.arb.heroLine
    val shape = MaterialTheme.shapes.extraLarge
    Column(
        modifier.fillMaxWidth()
            .clip(shape).background(BrandColors.Pitch)
            .then(if (dark) Modifier.border(1.dp, BrandColors.Chalk.copy(alpha = 0.24f), shape) else Modifier)
            .drawBehind {
                // «Centre del camp»: cercle r 88 dp, línia de mig camp i punt (decoratiu, sense semàntica).
                val c = Offset(size.width - 50.dp.toPx(), size.height / 2)
                val r = 88.dp.toPx()
                drawCircle(line, radius = r, center = c, style = Stroke(2.dp.toPx()))
                drawLine(line, Offset(c.x, 0f), Offset(c.x, size.height), strokeWidth = 2.dp.toPx())
                drawCircle(line, radius = 6.dp.toPx(), center = c)
            }
            .clipToBounds()
            .heightIn(min = if (facts != null) 200.dp else 0.dp)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = if (facts != null) Arrangement.SpaceBetween else Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            if (overline != null) Text(overline, style = ArbText.heroOverline, color = BrandColors.PitchSoft)
            Text(
                title, style = ArbText.heroTitle, color = Color.White,
                modifier = Modifier.semantics { heading() },
            )
            if (body != null) Text(body, style = MaterialTheme.typography.bodyLarge, color = BrandColors.PitchSoft, modifier = Modifier.widthIn(max = 260.dp))
        }
        if (facts != null) HeroFacts(facts, factsSpoken)
    }
}

@Composable
private fun HeroFacts(facts: List<HeroFact>, spoken: String) {
    // = `#F2F5F3` α 0,12 sobre la gespa, però OPAC: el cercle decoratiu no es veu a través del text.
    val tint = Color(0xFF2A533F)
    val measurer = rememberTextMeasurer()
    val style = ArbText.factLabel
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 12.dp).semantics(mergeDescendants = true) { contentDescription = spoken }) {
        val density = LocalDensity.current
        // Ample útil d'un requadre: (total − 2 separacions de 6) / 3 − padding horitzontal 2 × 8.
        val cell = (maxWidth - 12.dp) / facts.size - 16.dp
        val widest = facts.maxOf { f -> f.label.split(' ').maxOf { with(density) { measurer.measure(it, style).size.width.toDp() } } }
        if (isLargeText() || widest > cell) {
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(tint).padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                facts.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(f.value, style = ArbText.factValue, color = Color.White, modifier = Modifier.width(40.dp))
                        Text(f.label, style = ArbText.factLabel, color = BrandColors.PitchSoft, modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                facts.forEach { f ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(MaterialTheme.shapes.medium).background(tint).padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(f.value, style = ArbText.factValue, color = Color.White)
                        Text(f.label, style = ArbText.factLabel, color = BrandColors.PitchSoft)
                    }
                }
            }
        }
    }
}

/**
 * Anell d'encerts (substitueix el marcador de botons). Aprovat: pista `iconTile` + arc `secondary`; suspès: pista
 * `outlineVariant` + arc `onSurfaceVariant` (SENSE vermell, 04 §2.8). Sense arc si no hi ha cap encert (la punta rodona pintaria un punt).
 * Decoratiu per a TalkBack: [description] ho diu tot.
 */
@Composable
fun ScoreRing(correct: Int, total: Int, passed: Boolean, description: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val arb = MaterialTheme.arb
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    val d = (132f * fontScale).dp
    val target = if (total > 0) correct.toFloat() / total else 0f
    val still = reduceMotion()
    val sweep = remember(correct, total) { Animatable(if (still) target else 0f) }
    LaunchedEffect(correct, total, still) {
        if (!still) sweep.animateTo(target, tween(600, easing = FastOutSlowInEasing)) else sweep.snapTo(target)
    }
    val track = if (passed) arb.iconTile else scheme.outlineVariant
    val arc = if (passed) scheme.secondary else scheme.onSurfaceVariant
    Box(modifier.size(d).clearAndSetSemantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(d)) {
            val w = 12.dp.toPx()
            val inset = w / 2
            val sz = Size(size.width - w, size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), sz, style = Stroke(w))
            if (correct > 0) drawArc(arc, -90f, 360f * sweep.value, false, Offset(inset, inset), sz, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text("$correct/$total", style = ArbText.score, color = scheme.primary, textAlign = TextAlign.Center)
    }
}

/** Una dada del resultat. [accent]: valor en `secondary` (encerts). */
class StatCell(val value: String, val label: String, val accent: Boolean = false)

/**
 * Tres requadres de dada (`tile`, radi 16) dins de la targeta de resultat; amb lletra ≥ 1,15× passen a files. [spoken] és la
 * descripció fusionada per a TalkBack.
 */
@Composable
fun StatTiles(cells: List<StatCell>, spoken: String, modifier: Modifier = Modifier) {
    val arb = MaterialTheme.arb
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = spoken }) {
        // Ample mínim de cada requadre: la paraula més ampla (valor o etiqueta) + padding 2 × 10. Si no hi caben, files.
        val need = cells.map { c ->
            val words = c.label.split(' ').map { measurer.measure(it, ArbText.factLabel).size.width } + measurer.measure(c.value, ArbText.factValue).size.width
            with(density) { words.max().toDp() } + 20.dp
        }
        val fits = need.fold(0.dp) { acc, d -> acc + d } + 8.dp * (cells.size - 1) <= maxWidth
        if (isLargeText() || !fits) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cells.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().clip(shape).background(arb.tile).heightIn(min = ArbDimens.buttonMin).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(c.value, style = ArbText.factValue, color = if (c.accent) scheme.secondary else scheme.onSurface, modifier = Modifier.widthIn(min = 56.dp))
                        Text(c.label, style = ArbText.factLabel, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cells.forEachIndexed { i, c ->
                    Column(
                        // Amples proporcionals al contingut (cap paraula es parteix) i mateixa alçada.
                        Modifier.weight(need[i].value).fillMaxHeight().clip(shape).background(arb.tile).padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(c.value, style = ArbText.factValue, color = if (c.accent) scheme.secondary else scheme.onSurface)
                        Text(c.label, style = ArbText.factLabel, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
