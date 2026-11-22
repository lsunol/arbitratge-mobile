package dev.lluissunol.arbitratge.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lluissunol.arbitratge.R
import dev.lluissunol.arbitratge.exam.QuestionResult
import dev.lluissunol.arbitratge.exam.QuestionType
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun TimeUpScreen(st: Stage.TimeUp, onSeeResult: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.timeup_title), bottom = {
        PrimaryButton(stringResource(R.string.btn_see_result), onSeeResult)
    }) {
        InfoBanner(BannerKind.WARNING, icon = drawable(R.drawable.ic_timer)) {
            if (st.away) Body(stringResource(R.string.timeup_away))
            Body(stringResource(R.string.timeup_answered, st.answered, st.total))
            val missing = st.total - st.answered
            if (missing > 0) Body(pluralStringResource(R.plurals.timeup_missing, missing, missing))
        }
    }
}

/** C7. Con error (no se pudo desar/leer el resultado): reintento y abandono (04 §2.10). */
@Composable
fun GradingScreen(st: Stage.Grading, onRetry: () -> Unit, onAbandon: () -> Unit) {
    if (!st.error) {
        ScreenScaffold(title = null, topBar = { StatusInset() }) {
            Column(
                Modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandMark(64.dp)
                Spacer(Modifier.size(ArbDimens.s24))
                CircularProgressIndicator()
                Body(stringResource(R.string.correcting), Modifier.padding(top = ArbDimens.s16))
            }
        }
    } else {
        ScreenScaffold(title = stringResource(R.string.correcting), bottom = {
            PrimaryButton(stringResource(R.string.btn_retry), onRetry)
            SecondaryButton(stringResource(R.string.btn_abandon), onAbandon)
        }) {
            InfoBanner(BannerKind.ERROR, Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) {
                Text(stringResource(R.string.save_result_err), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// C8/C9 Resultat
// ---------------------------------------------------------------------------------------------

/**
 * El «botó» com a marcador: aro de progrés (encerts/total) amb una marca al llindar. Suspès: sense vermell, el llindar
 * en verd ensenya la meta, no el fallo (04 §2.8). Es fusiona en una sola descripció per a TalkBack.
 */
@Composable
private fun ScoreDisc(correct: Int, total: Int, threshold: Int, passed: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val d = max(120f, with(LocalDensity.current) { 64.sp.toDp().value * 1.9f }).dp
    val progress by animateFloatAsState(if (total > 0) correct.toFloat() / total else 0f, tween(600), label = "score")
    val desc = stringResource(R.string.score_a11y, correct, total)
    val fill = if (passed) scheme.primaryContainer else scheme.surfaceContainerHigh
    val arc = if (passed) scheme.primary else scheme.outline
    val tic = scheme.primary
    Box(Modifier.size(d).clearAndSetSemantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(d)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawCircle(fill, radius = size.minDimension / 2)
            drawArc(scheme.outlineVariant, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(arc, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // aro interior = el cant del botó
            drawCircle(scheme.outlineVariant, radius = size.minDimension / 2 - 10.dp.toPx() - stroke, style = Stroke(1.5.dp.toPx()))
            // marca del llindar
            // La marca del llindar només s'ensenya mentre es queda per sota (la meta); un cop aprovat ja no cal.
            if (total > 0 && correct < threshold) {
                val ang = Math.toRadians((-90.0 + 360.0 * threshold / total))
                val r = size.minDimension / 2
                val c = Offset(size.width / 2, size.height / 2)
                val a = Offset(c.x + ((r - stroke - 3.dp.toPx()) * cos(ang)).toFloat(), c.y + ((r - stroke - 3.dp.toPx()) * sin(ang)).toFloat())
                val b = Offset(c.x + ((r + 1.dp.toPx()) * cos(ang)).toFloat(), c.y + ((r + 1.dp.toPx()) * sin(ang)).toFloat())
                drawLine(tic, a, b, strokeWidth = 3.dp.toPx())
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(correct.toString(), style = MaterialTheme.typography.displayLarge, color = scheme.onSurface)
            Text(stringResource(R.string.score_of, total), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null) {
    Surface(
        modifier.fillMaxHeight().semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(ArbDimens.s16), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ResultScreen(
    st: Stage.Result,
    onSeeCredential: (CredentialPhase.Ready) -> Unit,
    onRetryCredential: () -> Unit,
    onReview: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit,
) {
    val r = st.result
    val passed = r.passed
    val failures = r.failures
    val scheme = MaterialTheme.colorScheme
    ScreenScaffold(title = stringResource(R.string.result_title), pinBottom = false, bottom = {
        if (passed) {
            when (val c = st.credential) {
                is CredentialPhase.Ready -> PrimaryButton(stringResource(R.string.btn_see_credential), { onSeeCredential(c) })
                CredentialPhase.Pending -> PrimaryButton(stringResource(R.string.btn_retry), onRetryCredential)
                else -> Unit
            }
            if (failures > 0) SecondaryButton(reviewLabel(failures), onReview)
        } else {
            if (failures > 0) PrimaryButton(reviewLabel(failures), onReview)
            SecondaryButton(stringResource(R.string.btn_retry), onRetry)
        }
        TertiaryButton(stringResource(R.string.btn_home), onHome)
    }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ArbDimens.s12)) {
            ScoreDisc(r.correct, r.total, r.threshold, passed)
            Text(
                stringResource(if (passed) R.string.passed_title else R.string.failed_title),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.result_min, r.threshold),
                style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
        }
        val secs = r.elapsedMs / 1000
        val failuresTile: @Composable (Modifier) -> Unit = { m ->
            StatTile(
                stringResource(R.string.stat_failures), failures.toString(), m,
                sub = if (r.unanswered > 0) stringResource(R.string.stat_unanswered_part, r.unanswered) else null,
            )
        }
        val timeTile: @Composable (Modifier) -> Unit = { m ->
            StatTile(stringResource(R.string.stat_time), stringResource(R.string.result_time, secs / 60, secs % 60), m)
        }
        if (isLargeText()) {
            Column(verticalArrangement = Arrangement.spacedBy(ArbDimens.s8)) {
                failuresTile(Modifier.fillMaxWidth())
                timeTile(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8)) {
                failuresTile(Modifier.weight(1f))
                timeTile(Modifier.weight(1f))
            }
        }
        if (!passed) Body(stringResource(R.string.result_missing, r.threshold - r.correct), color = scheme.onSurfaceVariant)
        if (passed) {
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                when (st.credential) {
                    is CredentialPhase.Ready -> InfoBanner(BannerKind.SUCCESS) {
                        Text(stringResource(R.string.passed_next), style = MaterialTheme.typography.bodyLarge)
                    }
                    CredentialPhase.Pending -> InfoBanner(BannerKind.ERROR) {
                        Text(stringResource(R.string.cred_pending), style = MaterialTheme.typography.bodyLarge)
                    }
                    else -> InfoBanner(BannerKind.NEUTRAL) {
                        Text(stringResource(R.string.cred_issuing), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        } else {
            InfoBanner(BannerKind.NEUTRAL) {
                Text(stringResource(R.string.failed_next), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun reviewLabel(failures: Int) =
    if (failures == 1) stringResource(R.string.btn_review_n_one) else stringResource(R.string.btn_review_n, failures)

// ---------------------------------------------------------------------------------------------
// C10 Repàs
// ---------------------------------------------------------------------------------------------

private enum class QState { RIGHT, WRONG, EMPTY }

private fun qState(q: QuestionResult) = when {
    q.isCorrect -> QState.RIGHT
    q.selectedIds.isEmpty() -> QState.EMPTY
    else -> QState.WRONG
}

@Composable
private fun qStatus(s: QState): String = stringResource(
    when (s) { QState.RIGHT -> R.string.q_right; QState.WRONG -> R.string.q_wrong; QState.EMPTY -> R.string.q_unanswered },
)

/** Insígnia d'estat: icona + text + color (mai només color). */
@Composable
private fun StatusBadge(s: QState) {
    val scheme = MaterialTheme.colorScheme
    when (s) {
        QState.RIGHT -> IconBadge(vector(Icons.Outlined.Check), scheme.primaryContainer, scheme.onPrimaryContainer)
        QState.WRONG -> IconBadge(vector(Icons.Outlined.Close), scheme.errorContainer, scheme.onErrorContainer)
        QState.EMPTY -> IconBadge(drawable(R.drawable.ic_remove), scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    st: Stage.Review,
    onBack: () -> Unit,
    onFilter: (Boolean) -> Unit,
    onOpen: (Int?) -> Unit,
) {
    val all = st.result.items.withIndex().toList()
    // Orden natural 1…N en las dos vistas (UX F7); por defecto solo fallos (incluye las sin responder).
    val list = if (st.onlyWrong) all.filter { !it.value.isCorrect } else all
    val wrongCount = all.count { !it.value.isCorrect }
    val open = st.openIndex?.let { list.getOrNull(it) }
    if (open != null) {
        val pos = st.openIndex!!
        ScreenScaffold(
            title = stringResource(R.string.review_detail_title),
            onBack = onBack,
            bottom = if (list.size > 1) {
                {
                    val prev: @Composable (Modifier) -> Unit = { m ->
                        SecondaryButton(stringResource(R.string.btn_prev_q), { onOpen(pos - 1) }, m, icon = vector(Icons.AutoMirrored.Outlined.KeyboardArrowLeft))
                    }
                    val next: @Composable (Modifier) -> Unit = { m ->
                        PrimaryButton(stringResource(R.string.btn_next), { onOpen(pos + 1) }, m, icon = vector(Icons.AutoMirrored.Outlined.KeyboardArrowRight))
                    }
                    // En los extremos el botón que no aplica se OCULTA (no se deshabilita al 38 %).
                    if (isLargeText()) {
                        if (pos < list.size - 1) next(Modifier)
                        if (pos > 0) prev(Modifier)
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12)) {
                            if (pos > 0) prev(Modifier.weight(1f))
                            if (pos < list.size - 1) next(Modifier.weight(1f))
                        }
                    }
                }
            } else null,
        ) { QuestionDetail(open.index, st.result.total, open.value) }
    } else {
        ScreenScaffold(title = stringResource(R.string.review_title), onBack = onBack) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = st.onlyWrong,
                    onClick = { onFilter(true) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.heightIn(min = ArbDimens.buttonMin),
                ) { Text(stringResource(R.string.filter_wrong, wrongCount), style = MaterialTheme.typography.labelMedium) }
                SegmentedButton(
                    selected = !st.onlyWrong,
                    onClick = { onFilter(false) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.heightIn(min = ArbDimens.buttonMin),
                ) { Text(stringResource(R.string.filter_all, all.size), style = MaterialTheme.typography.labelMedium) }
            }
            if (list.isEmpty()) Body(stringResource(R.string.review_empty))
            list.forEachIndexed { pos, item -> ReviewItem(item.index, st.result.total, item.value) { onOpen(pos) } }
        }
    }
}

@Composable
private fun ReviewItem(index: Int, total: Int, q: QuestionResult, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val state = qState(q)
    val status = qStatus(state)
    val title = stringResource(R.string.progress, index + 1, total)
    val shape = MaterialTheme.shapes.large
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(shape).background(scheme.surface, shape)
            .border(1.dp, scheme.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(ArbDimens.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12),
    ) {
        StatusBadge(state)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
            Text("$title · $status", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Text(q.text, style = MaterialTheme.typography.bodyLarge)
        }
        ArbIcon(vector(Icons.AutoMirrored.Outlined.KeyboardArrowRight), null, tint = scheme.onSurfaceVariant)
    }
}

/** Frase que explica POR QUÉ falló (calculada con lo marcado y lo correcto): UX §3.3. */
@Composable
private fun reasonText(q: QuestionResult): String {
    val sel = q.selectedIds.toSet()
    val cor = q.correctIds.toSet()
    val missed = (cor - sel).size
    val extra = (sel - cor).size
    return when {
        q.isCorrect -> stringResource(R.string.review_ok)
        sel.isEmpty() -> stringResource(R.string.review_empty_q)
        cor.size == 1 -> stringResource(R.string.review_wrong_single)
        missed > 0 && extra > 0 -> stringResource(R.string.review_both, extra, missed)
        missed > 0 -> pluralStringResource(R.plurals.review_missed, missed, missed)
        else -> pluralStringResource(R.plurals.review_extra, extra, extra)
    }
}

/**
 * Detalle (UX §3.3): TODAS las opciones en el orden mostrado con lo que hizo el candidato y el veredicto de cada una,
 * y encima la frase que explica el fallo. La opción marcada y la correcta usan tratamientos distintos
 * (icono + texto + color; la «marcada» de la pregunta era azul, aquí el color solo habla de acierto/error).
 */
@Composable
private fun QuestionDetail(index: Int, total: Int, q: QuestionResult) {
    val scheme = MaterialTheme.colorScheme
    val state = qState(q)
    Text(stringResource(R.string.progress, index + 1, total), style = ArbText.progressLabel, color = scheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8), verticalAlignment = Alignment.CenterVertically) {
        val (container, content) = when (state) {
            QState.RIGHT -> scheme.primaryContainer to scheme.onPrimaryContainer
            QState.WRONG -> scheme.errorContainer to scheme.onErrorContainer
            QState.EMPTY -> scheme.surfaceContainerHighest to scheme.onSurface
        }
        Row(
            Modifier.clip(CircleShape).background(container).padding(horizontal = ArbDimens.s12, vertical = ArbDimens.s8),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8),
        ) {
            ArbIcon(
                when (state) { QState.RIGHT -> vector(Icons.Outlined.Check); QState.WRONG -> vector(Icons.Outlined.Close); QState.EMPTY -> drawable(R.drawable.ic_remove) },
                null, tint = content, size = 20.sp.toScaledDp(),
            )
            Text(qStatus(state), style = MaterialTheme.typography.labelMedium, color = content)
        }
        Text(
            stringResource(when (q.type) { QuestionType.MULTIPLE_CHOICE -> R.string.q_type_multi; QuestionType.TRUE_FALSE -> R.string.q_type_tf; QuestionType.SINGLE_CHOICE -> R.string.q_type_single }),
            style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
        )
    }
    Text(q.text, style = ArbText.question)
    if (!q.isCorrect) {
        InfoBanner(BannerKind.NEUTRAL) {
            Text(reasonText(q), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    } else Body(reasonText(q), color = scheme.onSurfaceVariant)
    q.options.forEach { o -> ResolvedOption(displayOptionText(q.type, o.text), marked = o.id in q.selectedIds, correct = o.id in q.correctIds) }
    // Sin `explanation`: ni bloque ni hueco.
    q.explanation?.takeIf { it.isNotBlank() }?.let {
        Callout(drawable(R.drawable.ic_menu_book), scheme.surfaceContainer, scheme.onSurface) {
            Text(stringResource(R.string.why), style = MaterialTheme.typography.titleSmall)
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ResolvedOption(text: String, marked: Boolean, correct: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val label = stringResource(
        when {
            correct && marked -> R.string.opt_right_marked
            correct -> R.string.opt_right_missed
            marked -> R.string.opt_wrong_marked
            else -> R.string.opt_wrong_unmarked
        },
    )
    val container: Color
    val onContainer: Color
    val borderColor: Color
    val dashed: Boolean
    val borderWidth: Float
    when {
        correct && marked -> { container = scheme.primaryContainer; onContainer = scheme.onPrimaryContainer; borderColor = scheme.primary; dashed = false; borderWidth = 2f }
        correct -> { container = scheme.tertiaryContainer; onContainer = scheme.onTertiaryContainer; borderColor = scheme.tertiary; dashed = true; borderWidth = 2f }
        marked -> { container = scheme.errorContainer; onContainer = scheme.onErrorContainer; borderColor = scheme.error; dashed = false; borderWidth = 2f }
        else -> { container = scheme.surface; onContainer = scheme.onSurface; borderColor = scheme.outlineVariant; dashed = false; borderWidth = 1f }
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = ArbDimens.optionMin).clip(shape).background(container, shape)
            .drawBehind {
                val w = borderWidth.dp.toPx()
                drawRoundRect(
                    borderColor, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    style = Stroke(w, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null),
                )
            }
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = ArbDimens.s16, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val d = 28.sp.toScaledDp(max = 40.dp)
        when {
            correct && marked -> ArbIcon(vector(Icons.Default.CheckCircle), stringResource(R.string.icon_correct), tint = MaterialTheme.arb.correct, size = d)
            correct -> ArbIcon(vector(Icons.Outlined.Check), stringResource(R.string.icon_correct), tint = onContainer, size = d)
            marked -> ArbIcon(vector(Icons.Outlined.Close), stringResource(R.string.icon_wrong), tint = scheme.error, size = d)
            else -> Spacer(Modifier.size(d))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
            Text(text, style = ArbText.option, color = onContainer)
            Text(
                label, style = MaterialTheme.typography.labelMedium,
                color = if (!correct && !marked) scheme.onSurfaceVariant else onContainer,
                fontWeight = if (correct && !marked) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}
