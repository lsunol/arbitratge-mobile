package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.State
import android.provider.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.focusable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lluissunol.arbitratge.R
import dev.lluissunol.arbitratge.exam.QuestionType

/** Tot el bloc de callbacks de la pantalla de pregunta (agrupa per llegibilitat i tests). */
class QuestionActions(
    val onSelect: (String) -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onOverview: () -> Unit = {},
    val onToggleTime: () -> Unit = {},
    val onDismissBanner: () -> Unit = {},
    val onDialogConfirm: () -> Unit = {},
    val onDialogDismiss: () -> Unit = {},
    val onRetrySave: () -> Unit = {},
    val onAbandon: () -> Unit = {},
)

/** "mm:ss" del restant (04 §2.4). */
fun formatClock(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) + 999) / 1000 // redondeo hacia arriba: 00:00 solo al agotarse
    return "%02d:%02d".format(total / 60, total % 60)
}

@Composable
fun isLargeText(): Boolean = LocalDensity.current.fontScale >= 1.15f

@Composable
internal fun isExtraLargeText(): Boolean = LocalDensity.current.fontScale >= 1.5f

@Composable
fun QuestionScreen(st: Stage.Question, a: QuestionActions) {
    KeepScreenOn()
    val multiple = st.question.type == QuestionType.MULTIPLE_CHOICE
    val tf = st.question.type == QuestionType.TRUE_FALSE
    val density = LocalDensity.current
    val scroll = key(st.index) { rememberScrollState() }

    // Densitat adaptable (UI-04): nivell N (normal) per defecte; si el contingut no hi cap sense scroll, nivell C (compacte,
    // 18/16 sp, mínim 16 sp). El nivell es recorda per pregunta i escala de lletra (no salta en tornar-hi). L'avís de temps
    // NO compta en el càlcul (no ha de fer saltar el nivell a mig examen). Si C tampoc hi cap, queda l'scroll.
    val levels = remember(density.fontScale) { mutableStateMapOf<Int, Boolean>() }
    val compact = levels[st.index] == true
    var bannerPx by remember { mutableIntStateOf(0) }
    val bannerExtra = if (st.banner != null) bannerPx + with(density) { QDimens.sectionGap.roundToPx() } else 0
    val bannerExtraNow by rememberUpdatedState(bannerExtra)
    LaunchedEffect(st.index, density.fontScale) {
        snapshotFlow { scroll.maxValue }.collect { max ->
            // `maxValue` val Int.MAX_VALUE fins que es mesura el contingut.
            if (max != Int.MAX_VALUE && max - bannerExtraNow > 0) levels[st.index] = true
        }
    }

    ScreenScaffold(
        title = null,
        topBar = { QuestionTopBar(st, a) },
        bottom = { NavBar(st, a) },
        scrollKey = st.index, // cada pregunta empieza arriba: el scroll no se reutiliza entre preguntas
        scrollState = scroll,
        spacing = QDimens.sectionGap,
        bottomVPad = QDimens.navVPad,
        gutter = QDimens.gutter,
        contentTop = QDimens.contentTop,
        contentBottom = QDimens.contentBottom,
    ) {
        st.banner?.let { b -> Box(Modifier.onSizeChanged { bannerPx = it.height }) { TimeBannerCard(b, a.onDismissBanner) } }
        // Pistes visibles retirades (spec-acceptada): el control ja diu «una» o «moltes»; la pista queda per a TalkBack.
        val hint = when {
            multiple -> null
            tf -> stringResource(R.string.tf_hint)
            else -> stringResource(R.string.single_hint)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            QuestionProgressLabel(st)
            Text(
                st.question.text,
                style = if (compact) ArbText.questionC else ArbText.questionN,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics {
                    heading()
                    if (hint != null) contentDescription = "${st.question.text}. $hint"
                },
            )
            if (multiple) Box(Modifier.padding(top = 4.dp)) { MultiAnswerBadge() }
        }
        val kind = when { multiple -> OptionKind.CHECK; tf -> OptionKind.TRUE_FALSE; else -> OptionKind.RADIO }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(if (compact) QDimens.optionGapC else QDimens.optionGapN)) {
            st.question.options.forEach { opt ->
                OptionCard(
                    text = displayOptionText(st.question.type, opt.text),
                    selected = opt.id in st.selected,
                    kind = kind,
                    enabled = st.saveError == null,
                    onClick = { a.onSelect(opt.id) },
                    compact = compact,
                )
            }
        }
        // Sin línea visual de estado de guardado (feedback del usuario): el fallo se comunica con el diálogo modal y la
        // navegación queda bloqueada mientras hay un guardado en vuelo.
    }
    ExamDialogs(st.dialog, st.saveError, unanswered = st.total - st.answeredCount, a = a)
}

/** Estado de guardado de la pregunta actual derivado del estado CONFIRMADO (no de la selección visible). */
@Composable
private fun saveStateText(st: Stage.Question): String = when {
    st.saveError != null -> stringResource(R.string.q_state_save_error)
    st.saving -> stringResource(R.string.q_state_saving)
    st.committed.isNotEmpty() -> stringResource(R.string.q_state_answered)
    else -> ""
}

/**
 * Anterior / Següent (UI-01): dos botons `weight(1f)` idèntics (50 % exacte) i la mateixa alçada a TOTES les escales de
 * lletra; la jerarquia és només d'estil (Següent ple, Anterior tonal). A la P1 «Anterior» es veu **deshabilitat** per
 * conservar el 50/50. Una sola fila de 2 fills: l'ús d'`IntrinsicSize.Min` és barat i fa que, si un botó creix a 2 línies,
 * l'altre l'iguali. L'etiqueta mai parteix una paraula: auto-mida de 18 a 16 sp i, si cal, 2 línies.
 */
@Composable
private fun NavBar(st: Stage.Question, a: QuestionActions) {
    val prev = stringResource(R.string.btn_prev_q)
    val next = stringResource(if (st.isLast) R.string.btn_review_finish else R.string.btn_next)
    val pad = PaddingValues(horizontal = QDimens.navHPad, vertical = QDimens.navVPad)
    val arb = MaterialTheme.arb
    // Les fletxes van als DOS botons alhora i només amb lletra normal: l'alçada i l'ample són idèntics (UI-01).
    val arrows = !isLargeText()
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(QDimens.navGap)) {
        val mod = Modifier.weight(1f).fillMaxHeight().heightIn(min = QDimens.navButton)
        OutlinedButton(
            onClick = a.onPrevious, enabled = st.canGoBack, shape = ArbShape.pill, modifier = mod, contentPadding = pad,
            border = BorderStroke(1.5.dp, if (st.canGoBack) arb.buttonOutline else arb.buttonOutline.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (st.canGoBack) arb.card else Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
        ) { NavLabel(prev, if (arrows) vector(Icons.AutoMirrored.Outlined.ArrowBack) else null, leading = true) }
        Button(onClick = a.onNext, shape = ArbShape.pill, modifier = mod, contentPadding = pad) {
            NavLabel(next, if (arrows) vector(Icons.AutoMirrored.Outlined.ArrowForward) else null, leading = false)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavLabel(text: String, icon: androidx.compose.ui.graphics.painter.Painter?, leading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (icon != null && leading) ArbIcon(icon, null, size = 20.dp)
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Clip,
            autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 18.sp),
            modifier = Modifier.weight(1f, fill = false),
        )
        if (icon != null && !leading) ArbIcon(icon, null, size = 20.dp)
    }
}

// ---------------------------------------------------------------------------------------------
// Capçalera, cronòmetre i avisos
// ---------------------------------------------------------------------------------------------

/**
 * «Pregunta N de M» (antetítol de l'enunciat, UI-05): és l'encapçalament d'accessibilitat de la pantalla. Al canviar de
 * pregunta (04 §8.2): 1) el foco d'ENTRADA hi va (FocusRequester: a Compose no hi ha API pública per al foco d'accessibilitat;
 * TalkBack sol seguir el foco d'entrada) i 2) amb TalkBack actiu s'emet a més un anunci explícit «Pregunta N de M. Tipus.
 * Estat». [PV] comprovar en dispositiu real amb TalkBack que no es llegeix dues vegades.
 */
@Composable
private fun QuestionProgressLabel(st: Stage.Question) {
    val typeLabel = typeLabel(st.question.type)
    val stateLabel = saveStateText(st).ifEmpty { stringResource(R.string.q_state_empty) }
    val a11y = stringResource(R.string.q_status_a11y, st.index + 1, st.total, typeLabel, stateLabel)
    val focus = remember { FocusRequester() }
    val view = LocalView.current
    val spoken by rememberUpdatedState(a11y)
    LaunchedEffect(st.index) {
        focus.requestFocus()
        val am = view.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        @Suppress("DEPRECATION")
        if (am != null && am.isEnabled && am.isTouchExplorationEnabled) view.announceForAccessibility(spoken)
    }
    Text(
        stringResource(if (isExtraLargeText()) R.string.progress_short else R.string.progress, st.index + 1, st.total),
        style = ArbText.qProgress,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.focusRequester(focus).focusable().semantics {
            heading()
            contentDescription = a11y
        },
    )
}

/**
 * Capçalera d'UNA fila (48 dp + 4/4): progrés segmentat (M ≤ 12 i lletra < 1,5×; amb M > 12, barra contínua fina; amb lletra
 * ≥ 1,5× res) + rellotge + botó de resum (cercle blanc de 44 dp en una zona tàctil de 48). «Pregunta N de M» va a sobre de
 * l'enunciat ([QuestionProgressLabel]).
 */
@Composable
private fun QuestionTopBar(st: Stage.Question, a: QuestionActions) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.background) {
        // Edge-to-edge: esta barra propia no es TopAppBar, así que respeta ella misma la barra de estado y el recorte.
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = QDimens.gutter, vertical = QDimens.headerVPad)
                .heightIn(min = QDimens.headerRow),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8),
        ) {
            if (!isExtraLargeText()) {
                SegmentedProgress(st.index, st.total, Modifier.weight(1f).padding(end = 4.dp), continuous = st.total > 12)
            } else Spacer(Modifier.weight(1f))
            TimerPill(st.remainingMs, st.timeHidden, st.level, a.onToggleTime)
            val desc = stringResource(R.string.btn_overview_count_a11y, st.answeredCount, st.total)
            val arb = MaterialTheme.arb
            IconButton(onClick = a.onOverview, Modifier.size(QDimens.headerRow).testTag(OVERVIEW_TAG)) {
                Box(
                    Modifier.size(44.dp).softShadow(CircleShape, ShadowLevel.Option).clip(CircleShape).background(arb.card, CircleShape)
                        .cardBorder(CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(drawable(R.drawable.ic_checklist), contentDescription = desc, modifier = Modifier.size(22.dp), tint = scheme.primary) }
            }
        }
    }
}

const val OVERVIEW_TAG = "overview_button"

@Composable
fun TimerPill(remainingMs: Long, hidden: Boolean, level: TimeBanner?, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when {
        hidden -> scheme.primaryContainer to scheme.onPrimaryContainer
        level == TimeBanner.ONE_MIN -> scheme.error to scheme.onError
        level == TimeBanner.FIVE_MIN -> BrandColors.WarnAmber to BrandColors.OnWarnAmber
        else -> scheme.primaryContainer to scheme.onPrimaryContainer
    }
    val minutes = (remainingMs / 60_000L).toInt()
    val seconds = ((remainingMs / 1000L) % 60L).toInt()
    val lastMinute = remainingMs <= 60_000L
    val spoken = if (lastMinute) stringResource(R.string.time_left_a11y, minutes, seconds)
    else stringResource(R.string.time_left_min_a11y, ((remainingMs + 59_999L) / 60_000L).toInt())
    val action = stringResource(if (hidden) R.string.time_show else R.string.time_hide)
    val text = if (hidden) stringResource(R.string.time_show_label) else
        if (lastMinute) formatClock(remainingMs) else stringResource(R.string.time_minutes, ((remainingMs + 59_999L) / 60_000L).toInt())
    val icon = when {
        hidden -> R.drawable.ic_visibility_off
        level != null || lastMinute -> null
        else -> R.drawable.ic_timer
    }
    // Zona tàctil de 48 dp; la píndola visual en fa 36 (spec-acceptada: el rellotge no competeix amb l'enunciat).
    Box(
        modifier.heightIn(min = ArbDimens.touchMin)
            // No se anuncia cada segundo (sin liveRegion): el usuario consulta tocando. Pista de la acción (F8/F16).
            .clickable(onClickLabel = action, role = Role.Button, onClick = onToggle)
            .semantics(mergeDescendants = true) { contentDescription = if (hidden) action.replaceFirstChar { it.uppercase() } else spoken },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.heightIn(min = 40.dp).clip(CircleShape).background(bg).padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) ArbIcon(drawable(icon), null, tint = fg, size = 18.sp.toScaledDp())
            else ArbIcon(vector(Icons.Outlined.Warning), null, tint = fg, size = 18.sp.toScaledDp())
            // Mono (xifres tabulars) només per a mm:ss; «N min» i el text van en Atkinson Next.
            Text(text, style = if (lastMinute && !hidden) ArbText.timer.copy(fontSize = 16.sp, lineHeight = 22.sp) else MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = fg)
        }
    }
}

/**
 * Avís de temps (5 i 1 min): `InfoBanner` d'AVÍS (ambre + ⚠) amb «Tancar l'avís» a la dreta (≥ 56 dp). L'últim minut ja
 * té el rellotge en vermell; l'avís no es converteix en error (UX P3.1/P3.2).
 */
@Composable
fun TimeBannerCard(b: TimeBanner, onDismiss: () -> Unit) {
    InfoBanner(
        BannerKind.WARNING,
        Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        contentPadding = PaddingValues(start = ArbDimens.s16, top = ArbDimens.s4, bottom = ArbDimens.s4, end = ArbDimens.s4),
        centered = true,
        trailing = {
            IconButton(onClick = onDismiss, Modifier.size(ArbDimens.buttonMin)) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.btn_dismiss))
            }
        },
    ) {
        Text(
            stringResource(if (b == TimeBanner.ONE_MIN) R.string.warn_1 else R.string.warn_5),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = ArbDimens.s8),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Tipus de pregunta
// ---------------------------------------------------------------------------------------------

@Composable
private fun typeLabel(t: QuestionType): String = stringResource(
    when (t) {
        QuestionType.SINGLE_CHOICE -> R.string.q_type_single
        QuestionType.MULTIPLE_CHOICE -> R.string.q_type_multi
        QuestionType.TRUE_FALSE -> R.string.q_type_tf
    },
)

/**
 * Insígnia d'avís «Resposta múltiple» (UI-02): píndola plena en ambre (`warnBadge`) amb un disc fosc amb «!» (mateixa
 * família d'icones; sense casella, sense vora d'opció). Ocupa NOMÉS l'ample del contingut, no és interactiva (cap rol ni
 * clic) i el text d'ajuda llarg viu només a `contentDescription` (TalkBack). Dues línies a 200 % (mai es trunca).
 */
@Composable
private fun MultiAnswerBadge() {
    val arb = MaterialTheme.arb
    val a11y = stringResource(R.string.multi_badge_a11y)
    Row(
        Modifier.testTag(MULTI_BADGE_TAG)
            .background(arb.warnBadge, RoundedCornerShape(QDimens.badgeRadius))
            .heightIn(min = 32.dp)
            .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = a11y },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.testTag(MULTI_BADGE_ICON_TAG).size(20.sp.toScaledDp(max = 32.dp)).background(arb.onWarnBadge, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(drawable(R.drawable.ic_priority_high), contentDescription = null, modifier = Modifier.size(14.sp.toScaledDp(max = 24.dp)), tint = arb.warnBadge)
        }
        Text(stringResource(R.string.q_type_multi), style = ArbText.badge, color = arb.onWarnBadge, maxLines = 2)
    }
}

const val MULTI_BADGE_TAG = "multi_badge"
const val MULTI_BADGE_ICON_TAG = "multi_badge_icon"

enum class OptionKind { RADIO, CHECK, TRUE_FALSE }

/** Animacions de 120 ms, o canvi immediat si l'usuari ha reduït/eliminat les animacions del sistema. */
@Composable
internal fun reduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/**
 * Targeta de resposta (UI-05): tota la fila és el destí tàctil (≥ 52-56 dp, V/F ≥ 64 dp). Sense marcar = `card` amb ombra suau
 * (al fosc, filet d'1 dp); marcada = `selectContainer` + vora de 2 dp `select`, sense ombra, + indicador ple. La vora es dibuixa
 * amb `Modifier.border` (no ocupa layout), el pes del text és sempre 400 i el padding és fix: la targeta no salta en marcar-la.
 * A més del color canvien el gruix de vora, l'indicador i el rol (mai només el color, 04 §8.2).
 * [compact] = nivell C de densitat (16 sp, mínim 52 dp, padding vertical 8 dp).
 */
@Composable
fun OptionCard(text: String, selected: Boolean, kind: OptionKind, enabled: Boolean, onClick: () -> Unit, compact: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val arb = MaterialTheme.arb
    val shape = ArbShape.option
    val reduce = reduceMotion()
    val spec = if (reduce) snap<Color>() else tween<Color>(120)
    val container by animateColorAsState(if (selected) arb.selectContainer else arb.card, spec, label = "optionBg")
    val borderColor by animateColorAsState(if (selected) arb.select else arb.cardBorder, spec, label = "optionBorder")
    val bw by animateDpAsState(if (selected) 2.dp else 1.dp, if (reduce) snap() else tween(120), label = "optionBorderW")
    val tf = kind == OptionKind.TRUE_FALSE
    val ts = when { tf -> ArbText.optionTf; compact -> ArbText.optionC; else -> ArbText.optionN }
    val base = Modifier.fillMaxWidth()
        .then(if (enabled) Modifier else Modifier.alpha(0.38f))
        .then(if (selected) Modifier else Modifier.softShadow(shape, ShadowLevel.Option)) // la sombra va ANTES del clip
        .heightIn(min = when { tf -> QDimens.optionMinTf; compact -> QDimens.optionMinC; else -> QDimens.optionMinN })
        .clip(shape).background(container, shape).border(BorderStroke(bw, borderColor), shape)
    val mod = if (kind == OptionKind.CHECK) {
        base.toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        base.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    }
    val firstLinePad = with(LocalDensity.current) { ((ts.lineHeight.toDp() - QDimens.indicator) / 2).coerceAtLeast(0.dp) }
    Box(
        mod.padding(horizontal = QDimens.optionHPad, vertical = if (compact) QDimens.optionVPadC else QDimens.optionVPadN),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(QDimens.indicatorGap),
        ) {
            // A l'esquerra per a tots els tipus; en text llarg, queda alineat amb la primera línia.
            Box(Modifier.padding(top = firstLinePad)) { Indicator(selected, kind == OptionKind.CHECK) }
            Text(text, style = ts, color = scheme.onSurface, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Control visual de 24 dp (sense semàntica pròpia: la posa la fila). Sense marcar: anell/casella de 2 dp `optionRing` (≥ 3:1).
 * Marcat: radi = anell `select` amb punt de 12 dp; casella (radi 8) plena en `select` amb ✓ de 18 dp en `onSelect`.
 */
@Composable
private fun Indicator(selected: Boolean, square: Boolean) {
    val arb = MaterialTheme.arb
    val shape = if (square) MaterialTheme.shapes.extraSmall else CircleShape
    val ring = if (selected) arb.select else arb.optionRing
    Box(
        Modifier.size(QDimens.indicator).clip(shape)
            .background(if (square && selected) arb.select else Color.Transparent, shape)
            .border(2.dp, ring, shape).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            if (square) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = arb.onSelect)
            else Box(Modifier.size(12.dp).clip(CircleShape).background(arb.select))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Diàlegs C5 (compartits per la pregunta i el resum)
// ---------------------------------------------------------------------------------------------

@Composable
fun ExamDialogs(dialog: QuestionDialog?, saveError: SaveError?, unanswered: Int, a: QuestionActions) {
    val scheme = MaterialTheme.colorScheme
    when (dialog) {
        QuestionDialog.EXIT -> ArbDialog(
            title = stringResource(R.string.exit_title),
            icon = vector(Icons.AutoMirrored.Outlined.ExitToApp), iconTint = scheme.secondary,
            onDismissRequest = a.onDialogDismiss,
            primaryText = stringResource(R.string.btn_continue_exam), onPrimary = a.onDialogDismiss,
            secondaryText = stringResource(R.string.btn_exit), onSecondary = a.onDialogConfirm,
        ) { Text(stringResource(R.string.exit_body), style = MaterialTheme.typography.bodyLarge) }
        QuestionDialog.FINISH -> ArbDialog(
            title = stringResource(R.string.finish_title),
            icon = drawable(R.drawable.ic_outlined_flag), iconTint = scheme.primary,
            onDismissRequest = a.onDialogDismiss,
            // La acción SEGURA es el primario (04 §2.4); la irreversible, el secundario.
            primaryText = stringResource(R.string.btn_back_exam), onPrimary = a.onDialogDismiss,
            secondaryText = stringResource(R.string.btn_finish_ok), onSecondary = a.onDialogConfirm,
        ) {
            Text(
                if (unanswered > 0) pluralStringResource(R.plurals.finish_body_missing, unanswered, unanswered)
                else stringResource(R.string.finish_body_all),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.finish_body_end), style = MaterialTheme.typography.bodyLarge)
        }
        null -> Unit
    }

    // Fallo de guardado (03 §4.3, B3): diálogo modal; la selección sigue marcada detrás.
    saveError?.let { err ->
        ArbDialog(
            title = stringResource(if (err == SaveError.RESULT) R.string.save_result_title else R.string.save_title),
            icon = vector(Icons.Outlined.Warning), iconTint = scheme.error,
            onDismissRequest = {}, dismissible = false,
            primaryText = stringResource(R.string.btn_retry), onPrimary = a.onRetrySave,
            secondaryText = stringResource(R.string.btn_leave_exam), onSecondary = a.onAbandon,
        ) {
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Assertive }, verticalArrangement = Arrangement.spacedBy(ArbDimens.s8)) {
                Text(
                    stringResource(if (err == SaveError.RESULT) R.string.save_result_err else R.string.save_warn),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(stringResource(R.string.save_fail_exit), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
