package dev.lluissunol.arbitratge.ui

import android.app.Activity
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.lluissunol.arbitratge.R
import dev.lluissunol.arbitratge.credential.QrMatrix

// ---------------------------------------------------------------------------------------------
// Estructura
// ---------------------------------------------------------------------------------------------

/**
 * Estructura comuna (04 §2): barra superior pròpia (sense alçada fixa: títols llargs al 200 %), contingut amb
 * scroll i barra d'accions ancorada a baix amb separador quan el contingut es pot desplaçar. Respecta els insets
 * (edge-to-edge, 03 §10.3).
 */
@Composable
fun ScreenScaffold(
    title: String?,
    onBack: (() -> Unit)? = null,
    topBar: (@Composable () -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    /** El estado de scroll es PROPIO de cada clave (p. ej. el índice de la pregunta): no se reutiliza entre preguntas. */
    scrollKey: Any? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = key(scrollKey) { rememberScrollState() }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (topBar != null) topBar() else if (title != null) ArbTopBar(title, onBack)
        },
        bottomBar = {
            if (bottom != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column {
                        if (scroll.canScrollForward) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(
                            Modifier.fillMaxWidth().navigationBarsPadding()
                                .padding(horizontal = ArbDimens.gutter, vertical = ArbDimens.s12),
                            verticalArrangement = Arrangement.spacedBy(ArbDimens.s8),
                        ) { bottom() }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(scroll)
                .padding(horizontal = ArbDimens.gutter, vertical = ArbDimens.s16),
            verticalArrangement = Arrangement.spacedBy(ArbDimens.itemGap),
            content = content,
        )
    }
}

/**
 * Barra superior del color del fons, títol 22/600 que pot ocupar diverses línies (200 %). Les accions (cronòmetre) van
 * a la dreta amb el marge de 20 dp; amb text gran baixen a una segona fila perquè el títol no s'aixafi.
 */
@Composable
fun ArbTopBar(title: String, onBack: (() -> Unit)?, actions: (@Composable () -> Unit)? = null) {
    val stacked = actions != null && isLargeText()
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    .padding(start = if (onBack != null) 4.dp else ArbDimens.gutter, end = if (actions != null) ArbDimens.gutter else ArbDimens.s8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, Modifier.size(ArbDimens.touchMin)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Spacer(Modifier.size(ArbDimens.s4))
                }
                Text(
                    title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f).padding(vertical = ArbDimens.s8).semantics { heading() },
                )
                if (!stacked) actions?.invoke()
            }
            if (stacked) Box(Modifier.padding(start = ArbDimens.gutter, end = ArbDimens.gutter, bottom = ArbDimens.s8)) { actions?.invoke() }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Botons (14 dp, text centrat, ≥ 56 dp)
// ---------------------------------------------------------------------------------------------

@Composable
private fun ButtonLabel(text: String, icon: Painter?) {
    if (icon != null) {
        ArbIcon(icon, null, size = 22.sp.toScaledDp())
        Spacer(Modifier.size(ArbDimens.s8))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: Painter? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().heightIn(min = ArbDimens.buttonMin),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) { ButtonLabel(text, icon) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: Painter? = null) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.5.dp, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth().heightIn(min = ArbDimens.buttonMin),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) { ButtonLabel(text, icon) }
}

@Composable
fun TertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Painter? = null, fillWidth: Boolean = true) {
    TextButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = (if (fillWidth) modifier.fillMaxWidth() else modifier).heightIn(min = ArbDimens.touchMin),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) { ButtonLabel(text, icon) }
}

// ---------------------------------------------------------------------------------------------
// Text i icones
// ---------------------------------------------------------------------------------------------

@Composable
fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = modifier.semantics { heading() })
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = color, modifier = modifier)
}

@Composable
fun Gap(h: Int = 8) = Spacer(Modifier.height(h.dp))

/** Mida en dp que escala amb la mida de lletra (l'icona al costat d'un text no queda minúscula al 200 %). */
@Composable
fun androidx.compose.ui.unit.TextUnit.toScaledDp(max: Dp = 56.dp): Dp = with(LocalDensity.current) { toDp() }.coerceAtMost(max)

@Composable
fun vector(v: ImageVector): Painter = rememberVectorPainter(v)

@Composable
fun drawable(@DrawableRes id: Int): Painter = painterResource(id)

@Composable
fun ArbIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.sp.toScaledDp(),
) {
    Icon(painter, contentDescription, modifier.size(size), tint)
}

/** Cercle de 40 dp amb una icona (decoratiu: el text del costat ja diu el mateix). */
@Composable
fun IconBadge(painter: Painter, container: Color, content: Color, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val d = 40.sp.toScaledDp(max = 64.dp)
    Box(
        modifier.size(d).background(container, CircleShape).then(
            if (contentDescription == null) Modifier.clearAndSetSemantics { } else Modifier.semantics { this.contentDescription = contentDescription },
        ),
        contentAlignment = Alignment.Center,
    ) { ArbIcon(painter, null, tint = content, size = 24.sp.toScaledDp(max = 40.dp)) }
}

// ---------------------------------------------------------------------------------------------
// Targetes, avisos i diàlegs
// ---------------------------------------------------------------------------------------------

/** Targeta plana (sense ombra): es separa per to i per un filet d'1 dp. */
@Composable
fun ArbCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = container, border = border) {
        Column(Modifier.padding(ArbDimens.s16), verticalArrangement = Arrangement.spacedBy(ArbDimens.s8), content = content)
    }
}

/** Avís amb icona (informació, temps, bateria…). */
@Composable
fun Callout(
    icon: Painter,
    container: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = container, contentColor = contentColor) {
        Row(Modifier.padding(ArbDimens.s16), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12), verticalAlignment = Alignment.Top) {
            ArbIcon(icon, null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4), content = content)
        }
    }
}

/**
 * Diàleg amb accions apilades a tot l'ample: la SEGURA és el primari ple; la irreversible, el secundari (04 §2.4).
 * Amb [dismissible] = false (fallada de desat) ni Enrere ni el toc a fora el tanquen.
 */
@Composable
fun ArbDialog(
    title: String,
    icon: Painter,
    iconTint: Color,
    onDismissRequest: () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    dismissible: Boolean = true,
    body: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible, usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ArbDimens.s24),
            shape = MaterialTheme.shapes.extraLarge,
            // Clar: blanc (surfaceContainerLowest); fosc: surfaceContainerHigh (visual review §4.4).
            color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 6.dp,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(ArbDimens.s24),
                verticalArrangement = Arrangement.spacedBy(ArbDimens.s12),
            ) {
                ArbIcon(icon, null, tint = iconTint, size = 32.sp.toScaledDp(max = 56.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Column(verticalArrangement = Arrangement.spacedBy(ArbDimens.s8)) {
                    CompositionLocalProvidesBody(body)
                }
                Spacer(Modifier.height(ArbDimens.s8))
                PrimaryButton(primaryText, onPrimary)
                SecondaryButton(secondaryText, onSecondary)
            }
        }
    }
}

@Composable
private fun ColumnScope.CompositionLocalProvidesBody(body: @Composable ColumnScope.() -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) { body() }
}

// ---------------------------------------------------------------------------------------------
// Marca
// ---------------------------------------------------------------------------------------------

/**
 * Símbol de marca «Botó al centre» (especificació de marca §2, `svg/arbitratge-icona.svg`): disc de gespa Ø64 u i, a
 * dins, la graella de 64 u ESCALADA AL 86 %: dos trams de la línia de mig camp (de la vora fins al cercle central, sense
 * creuar-lo), cercle central r19, botó (anell r10,5 de gruix 6) i punt groc. [onPitch] afegeix un filet de cal quan el
 * símbol es posa sobre una banda de gespa (si no, el disc es confon amb el fons).
 */
@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier, contentDescription: String? = null, onPitch: Boolean = false) {
    Canvas(
        modifier.size(size).then(
            if (contentDescription == null) Modifier.clearAndSetSemantics { } else Modifier.semantics { this.contentDescription = contentDescription },
        ),
    ) {
        val u = this.size.minDimension / 64f
        val k = u * 0.86f // la graella interior, escalada al 86 %
        val c = Offset(this.size.width / 2, this.size.height / 2)
        drawCircle(BrandColors.Pitch, radius = 32f * u, center = c)
        if (onPitch) drawCircle(BrandColors.Chalk, radius = 31f * u, center = c, style = Stroke(2f * u))
        val edge = 32f * u // la línia de mig camp arriba a la VORA del disc (no al 86 %); només cercle i botó s'escalen
        drawLine(BrandColors.Chalk, Offset(c.x, c.y - edge), Offset(c.x, c.y - 19f * k), strokeWidth = 3f * k)
        drawLine(BrandColors.Chalk, Offset(c.x, c.y + 19f * k), Offset(c.x, c.y + edge), strokeWidth = 3f * k)
        drawCircle(BrandColors.Chalk, radius = 19f * k, center = c, style = Stroke(3f * k))
        drawCircle(BrandColors.Chalk, radius = 10.5f * k, center = c, style = Stroke(6f * k))
        drawCircle(BrandColors.RefCard, radius = 4.5f * k, center = c)
    }
}

/** Línies de cal decoratives (mig camp + cercle central) al darrere d'un contenidor; sense semàntica. */
fun Modifier.pitchLines(): Modifier = this.clipToBounds().drawBehind {
    val x = size.width * 0.80f
    val w = 1.5.dp.toPx()
    drawLine(BrandColors.PitchLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = w)
    drawCircle(BrandColors.PitchLine, radius = size.height * 0.85f, center = Offset(x, size.height / 2), style = Stroke(w))
}

// ---------------------------------------------------------------------------------------------
// QR i finestra
// ---------------------------------------------------------------------------------------------

/**
 * QR amb fons blanc pur, mòduls negres i zona de silenci de 4 mòduls (03 §6.5; 04 §3.1), amb mòduls en píxels ENTERS
 * (cap vora antialiasada). Sempre #000/#FFF, sense logotip a dins (no es toca ARB1).
 */
@Composable
fun QrCode(matrix: QrMatrix, description: String, modifier: Modifier = Modifier) {
    val quiet = 4
    Canvas(
        modifier.aspectRatio(1f).background(Color.White).semantics { contentDescription = description },
    ) {
        val total = matrix.size + 2 * quiet
        val dim = size.minDimension.toInt()
        val cell = (dim / total).coerceAtLeast(1)
        val origin = ((dim - cell * total) / 2).coerceAtLeast(0)
        val side = Size(cell.toFloat(), cell.toFloat())
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix[x, y]) {
                    drawRect(Color.Black, Offset((origin + (x + quiet) * cell).toFloat(), (origin + (y + quiet) * cell).toFloat()), side)
                }
            }
        }
    }
}

/** `FLAG_KEEP_SCREEN_ON` mientras hay examen o presentación (03 §4.5). */
@Composable
fun KeepScreenOn() {
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(window) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/** Brillo al máximo mientras se muestra el QR; se restaura al salir (04 §3.3). Sin permisos, sin FLAG_SECURE. */
@Composable
fun MaxBrightness() {
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(window) {
        val previous = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window?.let { w ->
            val a = w.attributes
            a.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            w.attributes = a
        }
        onDispose {
            window?.let { w ->
                val a = w.attributes
                a.screenBrightness = previous
                w.attributes = a
            }
        }
    }
}
