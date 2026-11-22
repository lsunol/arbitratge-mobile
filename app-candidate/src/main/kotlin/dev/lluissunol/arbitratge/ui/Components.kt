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
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
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
 * Estructura comuna (04 §2): barra superior pròpia de 56 dp (només «Enrere» i, al resum, el rellotge), el **títol com a
 * primer element del contingut** (I5: es desplaça amb el contingut, així al 200 % no ocupa espai fix), contingut amb
 * scroll i barra d'accions ancorada a baix amb separador quan el contingut es pot desplaçar. Respecta els insets
 * (edge-to-edge, 03 §10.3).
 *
 * Amb [pinBottom] = false les accions baixen al **final del contingut** quan la lletra és extra-gran (≥ 1,5×), perquè la
 * barra fixa no es mengi el 25-30 % de l'alçada (I9). Es fan servir a benvinguda, inici, normes, resultat i credencial;
 * pregunta, resum, detall de repàs i nom les mantenen fixes.
 */
@Composable
fun ScreenScaffold(
    title: String?,
    onBack: (() -> Unit)? = null,
    topBar: (@Composable () -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    /** El estado de scroll es PROPIO de cada clave (p. ej. el índice de la pregunta): no se reutiliza entre preguntas. */
    scrollKey: Any? = null,
    spacing: Dp = ArbDimens.itemGap,
    pinBottom: Boolean = true,
    bottomVPad: Dp = ArbDimens.s12,
    /** Banda a sangre (I3) por detrás de la barra de estado: va FUERA del margen lateral y de los insets superiores. */
    header: (@Composable () -> Unit)? = null,
    /** Color de la banda [header] y si sus iconos de la barra de estado van en claro (banda oscura). */
    headerColor: Color = BrandColors.Pitch,
    headerLightIcons: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = key(scrollKey) { rememberScrollState() }
    val pinned = pinBottom || !isExtraLargeText()
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        // Con banda, el inset superior lo gestiona la propia banda (edge-to-edge): el contenido arranca en y = 0.
        contentWindowInsets = if (header != null) WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            when {
                topBar != null -> topBar()
                onBack != null -> ArbTopBar(onBack)
                title != null -> StatusInset()
            }
        },
        bottomBar = {
            if (bottom != null && pinned) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column {
                        if (scroll.canScrollForward) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(
                            Modifier.fillMaxWidth().navigationBarsPadding()
                                .padding(horizontal = ArbDimens.gutter, vertical = bottomVPad),
                            verticalArrangement = Arrangement.spacedBy(ArbDimens.s8),
                        ) { bottom() }
                    }
                }
            }
        },
    ) { padding ->
        // Con banda: un fondo FIJO de la altura de la barra de estado por encima del scroll. Mientras la banda sigue por
        // detrás de la barra (scroll en reposo o casi) es del color de la banda y los iconos se adaptan a ella; cuando ya
        // ha salido, es el fondo de pantalla y los iconos vuelven al tema (el contenido nunca pasa bajo la hora).
        var headerH by remember { mutableIntStateOf(0) }
        val statusH = WindowInsets.statusBars.getTop(LocalDensity.current)
        val bandBehind = header != null && isBandBehindStatusBar(headerH, scroll.value, statusH)
        if (header != null) LightStatusBarIcons(light = bandBehind && headerLightIcons)
        Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            if (header != null) Box(Modifier.onSizeChanged { headerH = it.height }) { header() }
            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = ArbDimens.gutter)
                    .padding(top = if (header != null) ArbDimens.s24 else ArbDimens.s12, bottom = ArbDimens.s12),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (title != null) ScreenTitle(title)
                content()
                if (bottom != null && !pinned) {
                    Spacer(Modifier.height(ArbDimens.s16))
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ArbDimens.s8)) { bottom() }
                }
            }
        }
        if (header != null) {
            Box(
                Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(if (bandBehind) headerColor else MaterialTheme.colorScheme.background)
                    .testTag(STATUS_CAP_TAG),
            )
        }
        }
    }
}

const val STATUS_CAP_TAG = "status_cap"

/**
 * La banda (de alto [headerH] px) cubre aún toda la barra de estado (alto [statusH] px) con el scroll en [scroll] px.
 * Antes de medirla (`headerH == 0`, primer frame) se asume que está detrás, para no parpadear al color de pantalla.
 */
internal fun isBandBehindStatusBar(headerH: Int, scroll: Int, statusH: Int): Boolean = headerH == 0 || headerH - scroll >= statusH


/** Títol de pantalla dins del contingut (28 sp/700, en tinta, `heading()`): 8 dp a dalt i 16 a baix (via `spacing`). */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.fillMaxWidth().padding(top = ArbDimens.s8).semantics { heading() },
    )
}

/**
 * Barra superior de 56 dp: només el botó d'enrere (48 dp) i, al resum, el rellotge a la dreta amb el marge de 20 dp.
 * El títol ja no hi viu (I5).
 */
@Composable
fun ArbTopBar(onBack: (() -> Unit)?, actions: (@Composable () -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .heightIn(min = ArbDimens.topBar)
                .padding(start = if (onBack != null) 4.dp else ArbDimens.gutter, end = if (actions != null) ArbDimens.gutter else ArbDimens.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, Modifier.size(ArbDimens.touchMin)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
            Spacer(Modifier.weight(1f))
            actions?.invoke()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Botons (14 dp, text centrat, ≥ 56 dp)
// ---------------------------------------------------------------------------------------------

@Composable
private fun androidx.compose.foundation.layout.RowScope.ButtonLabel(text: String, icon: Painter?, maxLines: Int = Int.MAX_VALUE) {
    if (icon != null) {
        ArbIcon(icon, null, size = 22.sp.toScaledDp())
        Spacer(Modifier.size(ArbDimens.s8))
    }
    // Con icono, el texto ocupa el resto de la fila (a 200 % en 2 líneas queda a la derecha del icono, no desalineado).
    Text(
        text, style = MaterialTheme.typography.labelLarge, textAlign = if (icon != null) TextAlign.Start else TextAlign.Center,
        maxLines = maxLines, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = if (icon != null) Modifier.weight(1f, fill = false) else Modifier,
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: Painter? = null, maxLines: Int = Int.MAX_VALUE, hPad: Dp = 20.dp) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().heightIn(min = ArbDimens.buttonMin),
        contentPadding = PaddingValues(horizontal = hPad, vertical = 14.dp),
    ) { ButtonLabel(text, icon, maxLines) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: Painter? = null, maxLines: Int = Int.MAX_VALUE, hPad: Dp = 20.dp) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        // I9: borde de 2 dp en `primary`; desactivat, vora i text a `onSurface` α 0,38 (es llegeix com a inactiu).
        border = BorderStroke(2.dp, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = ArbDimens.buttonMin),
        contentPadding = PaddingValues(horizontal = hPad, vertical = 14.dp),
    ) { ButtonLabel(text, icon, maxLines) }
}

@Composable
fun TertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Painter? = null, fillWidth: Boolean = true, minHeight: Dp = ArbDimens.touchMin) {
    TextButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        modifier = (if (fillWidth) modifier.fillMaxWidth() else modifier).heightIn(min = minHeight),
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

/** Avís amb icona (informació, temps, bateria…). [trailing]: acció a la dreta (p. ex. «Tancar l'avís»). */
@Composable
fun Callout(
    icon: Painter,
    container: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(ArbDimens.s16),
    centered: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = container, contentColor = contentColor) {
        Row(
            Modifier.padding(contentPadding), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12),
            verticalAlignment = if (centered) Alignment.CenterVertically else Alignment.Top,
        ) {
            ArbIcon(icon, null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4), content = content)
            trailing?.invoke()
        }
    }
}

/** Gramàtica dels avisos (UX P3.1): cada tipus té color I icona propis (mai només color). */
enum class BannerKind { NEUTRAL, SUCCESS, WARNING, ERROR }

/**
 * Avís amb tipus: neutre (cal + «i»), èxit (verd clar + ✓), avís (ambre + ⚠) i error (vermell clar + ⚠). [icon] permet
 * una icona pròpia del mateix estil (p. ex. bateria) sense canviar els colors.
 */
@Composable
fun InfoBanner(
    kind: BannerKind,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    contentPadding: PaddingValues = PaddingValues(ArbDimens.s16),
    centered: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, onContainer, glyph) = when (kind) {
        BannerKind.NEUTRAL -> Triple(scheme.surfaceContainer, scheme.onSurface, Icons.Outlined.Info)
        BannerKind.SUCCESS -> Triple(scheme.primaryContainer, scheme.onPrimaryContainer, Icons.Outlined.CheckCircle)
        BannerKind.WARNING -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, Icons.Outlined.Warning)
        BannerKind.ERROR -> Triple(scheme.errorContainer, scheme.onErrorContainer, Icons.Outlined.Warning)
    }
    Callout(icon ?: vector(glyph), container, onContainer, modifier, contentPadding, centered, trailing, content)
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
        // Scrim α 0,6 (I1), explícit: no depèn del tema de la plataforma.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialogWindow?.setDimAmount(0.6f)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ArbDimens.s24),
            shape = MaterialTheme.shapes.extraLarge,
            // Clar: blanc (surfaceContainerLowest); fosc: surfaceContainerHigh; radi 28 (I1).
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
                // Un únic botó ple (la via segura); la irreversible, amb vora.
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

/**
 * Etiqueta MOSTRADA d'una opció. Només presentació: l'id (`t`/`f`) i el text del banc («Vertader»/«Fals», contracte de
 * `BankValidator`) no canvien; a la pantalla (i a TalkBack) «Vertader» es mostra com «Cert».
 */
@Composable
fun displayOptionText(type: dev.lluissunol.arbitratge.exam.QuestionType, text: String): String =
    if (type == dev.lluissunol.arbitratge.exam.QuestionType.TRUE_FALSE && text == dev.lluissunol.arbitratge.exam.BankValidator.TRUE_TEXT) stringResource(R.string.tf_true_display) else text
