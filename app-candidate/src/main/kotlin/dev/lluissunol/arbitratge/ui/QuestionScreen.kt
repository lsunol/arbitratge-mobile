package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.BorderStroke
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

/** Fracció de la barra: posició de navegació (índex + 1) / total. */
internal fun navProgress(index: Int, total: Int): Float = (index + 1).toFloat() / total

@Composable
fun isLargeText(): Boolean = LocalDensity.current.fontScale >= 1.15f

@Composable
internal fun isExtraLargeText(): Boolean = LocalDensity.current.fontScale >= 1.5f

@Composable
fun QuestionScreen(st: Stage.Question, a: QuestionActions) {
    KeepScreenOn()
    val multiple = st.question.type == QuestionType.MULTIPLE_CHOICE
    val tf = st.question.type == QuestionType.TRUE_FALSE
    ScreenScaffold(
        title = null,
        topBar = { QuestionTopBar(st, a) },
        bottom = { NavBar(st, a) },
        scrollKey = st.index, // cada pregunta empieza arriba: el scroll no se reutiliza entre preguntas
        spacing = ArbDimens.s12,
        bottomVPad = 10.dp, // I9: la barra de la pregunta es més baixa
    ) {
        if (isExtraLargeText()) QuestionSummaryRow(st, a)
        st.banner?.let { b -> TimeBannerCard(b, a.onDismissBanner) }
        // Enunciat + indicació juntets; l'aviso de múltiple va SOTA l'enunciat (només per a resposta múltiple).
        Column(verticalArrangement = Arrangement.spacedBy(ArbDimens.s8)) {
            Text(
                st.question.text,
                style = ArbText.question,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            when {
                multiple -> MultiChip()
                tf -> Body(stringResource(R.string.tf_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Body(stringResource(R.string.single_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val kind = when { multiple -> OptionKind.CHECK; tf -> OptionKind.TRUE_FALSE; else -> OptionKind.RADIO }
        Column(verticalArrangement = Arrangement.spacedBy(ArbDimens.s12)) {
            st.question.options.forEach { opt ->
                OptionCard(
                    text = displayOptionText(st.question.type, opt.text),
                    selected = opt.id in st.selected,
                    kind = kind,
                    enabled = st.saveError == null,
                    onClick = { a.onSelect(opt.id) },
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
 * Anterior / Següent (UX P1.3): «Següent» pesa 1,4 i «Anterior» 1; a la P1 no hi ha «Anterior» (no es mostra un botó
 * inactiu) i «Següent» ocupa tot l'ample. A l'última pregunta, «Revisar i finalitzar». Amb lletra extra-gran (≥ 1,5) una
 * sola fila: [fletxa «Anterior» 56 dp | Següent]; mai es parteix una paraula.
 */
@Composable
private fun NavBar(st: Stage.Question, a: QuestionActions) {
    val prev = stringResource(R.string.btn_prev_q)
    val next = stringResource(if (st.isLast) R.string.btn_review_finish else R.string.btn_next)
    val scheme = MaterialTheme.colorScheme
    if (isExtraLargeText()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8), verticalAlignment = Alignment.CenterVertically) {
            if (st.canGoBack) {
                OutlinedIconButton(
                    onClick = a.onPrevious,
                    modifier = Modifier.size(ArbDimens.buttonMin),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(2.dp, scheme.primary),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.btn_prev_q_a11y), tint = scheme.primary)
                }
            }
            PrimaryButton(next, a.onNext, Modifier.weight(1f))
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8)) {
            if (st.canGoBack) SecondaryButton(prev, a.onPrevious, Modifier.weight(1f), maxLines = 1, hPad = 12.dp)
            PrimaryButton(next, a.onNext, Modifier.weight(if (st.canGoBack) 1.4f else 1f), maxLines = 1, hPad = 12.dp)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Capçalera, cronòmetre i avisos
// ---------------------------------------------------------------------------------------------

@Composable
private fun QuestionTopBar(st: Stage.Question, a: QuestionActions) {
    val scheme = MaterialTheme.colorScheme
    val typeLabel = typeLabel(st.question.type)
    val stateLabel = saveStateText(st).ifEmpty { stringResource(R.string.q_state_empty) }
    val a11y = stringResource(R.string.q_status_a11y, st.index + 1, st.total, typeLabel, stateLabel)
    Surface(color = scheme.background) {
        // Edge-to-edge: esta barra propia no es TopAppBar, así que respeta ella misma la barra de estado y el recorte.
        Column(
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = ArbDimens.gutter).padding(top = ArbDimens.s8, bottom = ArbDimens.s8),
            verticalArrangement = Arrangement.spacedBy(ArbDimens.s4),
        ) {
            // Al cambiar de pregunta (04 §8.2): 1) el foco de ENTRADA va a la cabecera (FocusRequester: en Compose no hay
            // API pública para el foco de accesibilidad; TalkBack suele seguir al foco de entrada) y 2) con TalkBack
            // activo se emite además un anuncio explícito «Pregunta N de M. Tipus. Estat». [PV] comprobar en dispositivo
            // real con TalkBack que no se lee dos veces y que el foco acaba en la cabecera.
            val focus = remember { FocusRequester() }
            val view = LocalView.current
            val spoken by rememberUpdatedState(a11y)
            LaunchedEffect(st.index) {
                focus.requestFocus()
                val am = view.context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                @Suppress("DEPRECATION")
                if (am != null && am.isEnabled && am.isTouchExplorationEnabled) view.announceForAccessibility(spoken)
            }
            val label: @Composable (Modifier) -> Unit = { m ->
                Text(
                    stringResource(if (isExtraLargeText()) R.string.progress_short else R.string.progress, st.index + 1, st.total),
                    style = ArbText.progressLabel,
                    color = scheme.onBackground,
                    modifier = m.focusRequester(focus).focusable().semantics {
                        heading()
                        contentDescription = a11y
                    },
                )
            }
            // Fila del títol: «Pregunta N de M» + rellotge (també al 200 %, UX P1.3: «N de M» curt, a11y sencer).
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                label(Modifier.weight(1f))
                TimerPill(st.remainingMs, st.timeHidden, st.level, a.onToggleTime)
            }
            // Barra de POSICIÓ: va amb «Pregunta N de M», separada del recompte de respostes.
            val progressA11y = stringResource(R.string.question_progress_a11y, st.index + 1, st.total)
            LinearProgressIndicator(
                progress = { navProgress(st.index, st.total) }, // POSICIÓN de navegación, no respuestas
                modifier = Modifier.fillMaxWidth().height(ArbDimens.progressHeight).semantics {
                    contentDescription = progressA11y
                    progressBarRangeInfo = ProgressBarRangeInfo((st.index + 1).toFloat(), 0f..st.total.toFloat())
                },
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            if (!isExtraLargeText()) QuestionSummaryRow(st, a)
        }
    }
}

@Composable
private fun QuestionSummaryRow(st: Stage.Question, a: QuestionActions) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            stringResource(R.string.answered_progress, st.answeredCount, st.total),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        val desc = stringResource(R.string.btn_overview_a11y)
        TextButton(
            onClick = a.onOverview,
            contentPadding = PaddingValues(start = 12.dp, end = 0.dp, top = 0.dp, bottom = 0.dp),
            modifier = Modifier.heightIn(min = ArbDimens.touchMin).semantics { contentDescription = desc },
        ) { Text(stringResource(R.string.btn_overview), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
    }
}

@Composable
fun TimerPill(remainingMs: Long, hidden: Boolean, level: TimeBanner?, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when {
        hidden -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
        level == TimeBanner.ONE_MIN -> scheme.error to scheme.onError
        level == TimeBanner.FIVE_MIN -> BrandColors.WarnAmber to BrandColors.OnWarnAmber
        else -> scheme.surfaceContainerHigh to scheme.onSurface
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
    Row(
        modifier.heightIn(min = ArbDimens.touchMin).clip(CircleShape).background(bg)
            // No se anuncia cada segundo (sin liveRegion): el usuario consulta tocando. Pista de la acción (F8/F16).
            .clickable(onClickLabel = action, role = Role.Button, onClick = onToggle)
            .semantics(mergeDescendants = true) { contentDescription = if (hidden) action.replaceFirstChar { it.uppercase() } else spoken }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8),
    ) {
        if (icon != null) ArbIcon(drawable(icon), null, tint = fg, size = 22.sp.toScaledDp())
        else ArbIcon(vector(Icons.Outlined.Warning), null, tint = fg, size = 22.sp.toScaledDp())
        // Mono (xifres tabulars) només per a mm:ss; «N min» i el text van en Atkinson Next.
        Text(text, style = if (lastMinute && !hidden) ArbText.timer else MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
fun TimeBannerCard(b: TimeBanner, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container = if (b == TimeBanner.ONE_MIN) scheme.errorContainer else BrandColors.WarnAmber
    val content = if (b == TimeBanner.ONE_MIN) scheme.onErrorContainer else BrandColors.OnWarnAmber
    Surface(
        Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
        shape = MaterialTheme.shapes.large, color = container, contentColor = content,
    ) {
        Row(Modifier.padding(start = ArbDimens.s16), verticalAlignment = Alignment.CenterVertically) {
            ArbIcon(vector(Icons.Outlined.Warning), null)
            Text(
                stringResource(if (b == TimeBanner.ONE_MIN) R.string.warn_1 else R.string.warn_5),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(horizontal = ArbDimens.s12, vertical = ArbDimens.s12),
            )
            IconButton(onClick = onDismiss, Modifier.size(ArbDimens.buttonMin)) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.btn_dismiss))
            }
        }
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
 * Xip compacte de la resposta múltiple, SOTA l'enunciat: casella **buida** + «Resposta múltiple» i una línia d'ajuda curta
 * (I2: una casella marcada sense res seleccionat enganyava). Transparent, vora d'1,5 dp en tinta. Les preguntes d'una
 * resposta i de cert/fals no porten etiqueta de tipus.
 */
@Composable
private fun MultiChip() {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
        Row(
            Modifier.clip(MaterialTheme.shapes.medium).border(BorderStroke(1.5.dp, scheme.onSurface), MaterialTheme.shapes.medium)
                .padding(horizontal = ArbDimens.s12, vertical = ArbDimens.s8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8),
        ) {
            ArbIcon(drawable(R.drawable.ic_check_box_outline), null, tint = scheme.onSurface, size = 20.sp.toScaledDp())
            Text(stringResource(R.string.q_type_multi), style = MaterialTheme.typography.labelMedium, color = scheme.onSurface)
        }
        Text(stringResource(R.string.multi_helper), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

enum class OptionKind { RADIO, CHECK, TRUE_FALSE }

/** Animacions de 120 ms, o canvi immediat si l'usuari ha reduït/eliminat les animacions del sistema. */
@Composable
private fun reduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/**
 * Targeta de resposta: tota la fila és el destí tàctil (≥ 64 dp, V/F ≥ 96 dp), contingut CENTRAT verticalment, indicador
 * de 28 dp. «Marca de bolígraf» (I2): marcada = bloc en tinta (`mark`), vora de 3 dp i text `onMark` a 600; sense marcar =
 * `surface` amb vora d'1,5 dp `outline`. La vora es dibuixa cap a dins i el padding es compensa: la targeta no canvia de mida.
 * A més del color canvien el pes, el control i el rol (mai només el color, 04 §8.2). El rol natiu (RadioButton/Checkbox)
 * anuncia l'estat; la semàntica no canvia.
 */
@Composable
fun OptionCard(text: String, selected: Boolean, kind: OptionKind, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val arb = MaterialTheme.arb
    val shape = MaterialTheme.shapes.medium
    val spec = if (reduceMotion()) snap<Color>() else tween<Color>(120)
    val container by animateColorAsState(if (selected) arb.mark else scheme.surface, spec, label = "optionBg")
    val borderColor by animateColorAsState(if (selected) arb.mark else scheme.outline, spec, label = "optionBorder")
    val textColor by animateColorAsState(if (selected) arb.onMark else scheme.onSurface, spec, label = "optionText")
    val bw = if (selected) 3.dp else 1.5.dp
    val base = Modifier.fillMaxWidth()
        .then(if (enabled) Modifier else Modifier.alpha(0.38f))
        .heightIn(min = if (kind == OptionKind.TRUE_FALSE) ArbDimens.optionMinTf else ArbDimens.optionMin)
        .clip(shape).background(container, shape).border(BorderStroke(bw, borderColor), shape)
    val mod = if (kind == OptionKind.CHECK) {
        base.toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        base.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    }
    Box(
        mod.padding(horizontal = ArbDimens.s16 - bw, vertical = ArbDimens.s12 - bw),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.fillMaxWidth().align(Alignment.CenterStart),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12),
        ) {
            // A l'esquerra per a tots els tipus; en text llarg, queda alineat amb la primera línia.
            Box(Modifier.padding(top = ArbDimens.s4)) { Indicator(selected, kind == OptionKind.CHECK) }
            val ts = if (kind == OptionKind.TRUE_FALSE) ArbText.optionTf else ArbText.option
            Text(
                text,
                style = if (kind == OptionKind.TRUE_FALSE) ts else ts.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                color = textColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Control visual de 28 dp (sense semàntica pròpia: la posa la fila). Sense marcar: anell de 2,5 dp `outline`. Marcat: radi =
 * anell `onMark` amb punt de 14 dp; casella = quadrat de radi 6 ple en `onMark` amb ✓ de 20 dp en `mark`.
 */
@Composable
private fun Indicator(selected: Boolean, square: Boolean) {
    val arb = MaterialTheme.arb
    val scheme = MaterialTheme.colorScheme
    val shape = if (square) RoundedCornerShape(6.dp) else CircleShape
    val ring = if (selected) arb.onMark else scheme.outline
    Box(
        Modifier.size(ArbDimens.indicator).clip(shape)
            .background(if (square && selected) arb.onMark else Color.Transparent, shape)
            .border(2.5.dp, ring, shape).clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            if (square) Icon(Icons.Outlined.Check, null, Modifier.size(20.dp), tint = arb.mark)
            else Box(Modifier.size(14.dp).clip(CircleShape).background(arb.onMark))
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
