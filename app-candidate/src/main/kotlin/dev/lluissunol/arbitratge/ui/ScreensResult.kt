package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lluissunol.arbitratge.R
import dev.lluissunol.arbitratge.exam.QuestionResult
import dev.lluissunol.arbitratge.exam.QuestionType

@Composable
fun TimeUpScreen(st: Stage.TimeUp, onSeeResult: () -> Unit) {
    // Pantalla de transició (UX P3.2): icona, títol, què ha passat i UN sol botó («Veure el resultat»).
    ScreenScaffold(title = stringResource(R.string.timeup_title), bottom = {
        PrimaryButton(stringResource(R.string.btn_see_result), onSeeResult)
    }) {
        ArbIcon(drawable(R.drawable.ic_timer), null, tint = MaterialTheme.colorScheme.secondary, size = 40.sp.toScaledDp(max = 64.dp))
        if (st.away) InfoBanner(BannerKind.WARNING) { Text(stringResource(R.string.timeup_away), style = MaterialTheme.typography.bodyLarge) }
        Text(stringResource(R.string.timeup_answered, st.answered, st.total), style = MaterialTheme.typography.titleMedium)
        val missing = st.total - st.answered
        if (missing > 0) Body(pluralStringResource(R.plurals.timeup_missing, missing, missing), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
 * Marcador de botons (I6): un disc de 24 dp per pregunta, primer els encerts (ple) i després la resta (aro de 2 dp), amb una
 * línia vertical després del disc [threshold] (el mínim per aprovar). Decoratiu: el pare ja descriu «N encerts de M».
 * Els discs s'omplen d'un en un (60 ms cadascun, 600 ms com a màxim); quiets si l'usuari ha reduït les animacions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ButtonsMarker(correct: Int, total: Int, threshold: Int, fill: Color, ring: Color, line: Color) {
    val d = 24.sp.toScaledDp(max = 32.dp)
    val still = reduceMotion()
    var shown by remember(correct) { mutableIntStateOf(if (still) correct else 0) }
    LaunchedEffect(correct, still) {
        if (!still) {
            val step = if (correct > 0) minOf(60L, 600L / correct) else 0L
            for (i in 1..correct) { delay(step); shown = i }
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8),
        verticalArrangement = Arrangement.spacedBy(ArbDimens.s8),
        itemVerticalAlignment = Alignment.CenterVertically,
        maxItemsInEachRow = 10,
    ) {
        // Cada element és el disc (i, si toca, la línia del mínim enganxada): la línia no compta com a element de la fila.
        for (i in 0 until total) {
            Row(horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(d).then(
                        if (i < shown) Modifier.background(fill, CircleShape) else Modifier.border(2.dp, ring, CircleShape),
                    ),
                )
                if (i + 1 == threshold && threshold < total) Box(Modifier.width(2.dp).height(32.dp).background(line))
            }
        }
    }
}

/**
 * Bloc de veredicte (I6), a sang per darrere de la barra d'estat. Aprovat: gespa amb text blanc; suspès: `surfaceContainer`
 * amb text en tinta i SENSE vermell (04 §2.8). El veredicte és l'encapçalament de la pantalla (heading + liveRegion polite).
 */
@Composable
private fun VerdictBlock(passed: Boolean, correct: Int, total: Int, threshold: Int) {
    val scheme = MaterialTheme.colorScheme
    val bg = if (passed) BrandColors.Pitch else scheme.surfaceContainer
    val ink = if (passed) Color.White else scheme.onSurface
    val soft = if (passed) BrandColors.PitchSubtitle else scheme.onSurfaceVariant
    val desc = pluralStringResource(R.plurals.score_a11y, correct, correct, total)
    Column(
        Modifier.fillMaxWidth().background(bg)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = ArbDimens.gutter).padding(top = ArbDimens.s24, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(ArbDimens.s12),
    ) {
        Text(
            stringResource(if (passed) R.string.passed_title else R.string.failed_title),
            style = MaterialTheme.typography.headlineMedium, color = ink,
            modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite },
        )
        Column(Modifier.clearAndSetSemantics { contentDescription = desc }, verticalArrangement = Arrangement.spacedBy(ArbDimens.s12)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8)) {
                Text(correct.toString(), style = MaterialTheme.typography.displayLarge, color = ink)
                Text(
                    pluralStringResource(R.plurals.score_of, total, total),
                    style = MaterialTheme.typography.titleMedium, color = soft, modifier = Modifier.padding(bottom = ArbDimens.s12),
                )
            }
            ButtonsMarker(
                correct, total, threshold,
                fill = if (passed) Color.White else scheme.primary,
                ring = if (passed) Color.White.copy(alpha = 0.7f) else scheme.outline,
                line = ink,
            )
        }
        Text(stringResource(R.string.result_min, threshold), style = MaterialTheme.typography.bodyLarge, color = soft)
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
    ScreenScaffold(
        title = null, pinBottom = false,
        header = { VerdictBlock(passed, r.correct, r.total, r.threshold) },
        headerColor = if (passed) BrandColors.Pitch else scheme.surfaceContainer,
        headerLightIcons = passed,
        bottom = {
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
            TertiaryButton(stringResource(R.string.btn_home), onHome, minHeight = ArbDimens.buttonMin)
        },
    ) {
        // Fallades = només les contestades malament; «Sense resposta» només si n'hi ha (UX P1.1).
        val secs = r.elapsedMs / 1000
        val cells = buildList {
            add(FactCell(r.wrong.toString(), pluralStringResource(R.plurals.stat_failures, r.wrong)))
            if (r.unanswered > 0) add(FactCell(r.unanswered.toString(), stringResource(R.string.stat_unanswered)))
            add(FactCell(stringResource(R.string.result_time_clock, secs / 60, secs % 60), stringResource(R.string.stat_time)))
        }
        // TalkBack: «N incorrectes, M sense resposta. Temps: …»
        val counts = buildList {
            add(pluralStringResource(R.plurals.stat_failures_a11y, r.wrong, r.wrong))
            if (r.unanswered > 0) add("${r.unanswered} ${stringResource(R.string.stat_unanswered).lowercase()}")
        }.joinToString(", ")
        val spoken = "$counts. ${stringResource(R.string.stat_time)}: ${stringResource(R.string.result_time, secs / 60, secs % 60)}"
        FactStrip(cells, spoken)
        if (passed) {
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(ArbDimens.s12)) {
                when (val c = st.credential) {
                    is CredentialPhase.Ready -> {
                        Text(stringResource(R.string.passed_next), style = MaterialTheme.typography.bodyLarge)
                        CredentialMini(c.credential.name)
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
            Body(stringResource(R.string.failed_next), color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun reviewLabel(failures: Int) =
    pluralStringResource(R.plurals.btn_review_n, failures, failures)

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

/**
 * Indicador d'estat de les llistes (I4): icona de 28 dp SENSE cercle. ✕ `wrong`, ✓ `correct`, – `onSurfaceVariant`. És
 * decoratiu: l'estat el diu el text de la fila («Pregunta N de M · Fallada»).
 */
@Composable
private fun StateGlyph(s: QState) {
    val scheme = MaterialTheme.colorScheme
    val arb = MaterialTheme.arb
    val d = 28.sp.toScaledDp(max = 40.dp)
    when (s) {
        QState.RIGHT -> ArbIcon(vector(Icons.Outlined.Check), null, tint = arb.correct, size = d)
        QState.WRONG -> ArbIcon(vector(Icons.Outlined.Close), null, tint = arb.wrong, size = d)
        QState.EMPTY -> ArbIcon(drawable(R.drawable.ic_remove), null, tint = scheme.onSurfaceVariant, size = d)
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
    val shape = MaterialTheme.shapes.medium
    Row(
        Modifier.fillMaxWidth().heightIn(min = ArbDimens.rowMin).clip(shape).background(scheme.surface, shape)
            .border(1.dp, scheme.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(ArbDimens.s16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12),
    ) {
        StateGlyph(state)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
            Text("$title · $status", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Text(q.text, style = MaterialTheme.typography.bodyLarge)
        }
        ArbIcon(vector(Icons.AutoMirrored.Outlined.KeyboardArrowRight), null, tint = scheme.onSurfaceVariant)
    }
}

/**
 * Frase neutra que explica POR QUÉ falló (UX §3.3 + Q3): SOLO en respuesta múltiple (qué se marcó de más / se olvidó);
 * en respuesta única y Cert/Fals el chip de estado y las etiquetas de las opciones ya lo dicen, y no hay frase.
 */
@Composable
private fun reasonText(q: QuestionResult): String? {
    val sel = q.selectedIds.toSet()
    val cor = q.correctIds.toSet()
    val missed = (cor - sel).size
    val extra = (sel - cor).size
    return when {
        q.isCorrect -> stringResource(R.string.review_ok)
        sel.isEmpty() -> stringResource(R.string.review_empty_q)
        q.type != QuestionType.MULTIPLE_CHOICE -> null
        missed > 0 && extra > 0 -> stringResource(R.string.review_both, extra, missed)
        missed > 0 -> pluralStringResource(R.plurals.review_missed, missed, missed)
        else -> pluralStringResource(R.plurals.review_extra, extra, extra)
    }
}

/**
 * Detalle (UX §3.3; I7): TODAS las opciones en el orden mostrado, en DOS canales independientes: lo que marcaste (el
 * indicador de la izquierda: disco de tinta o aro) y el veredicto (contenedor, borde, icono relleno al final y etiqueta,
 * en verde/rojo; SIN ámbar). Sin amarillo ni azul.
 */
@Composable
private fun QuestionDetail(index: Int, total: Int, q: QuestionResult) {
    val scheme = MaterialTheme.colorScheme
    val arb = MaterialTheme.arb
    val state = qState(q)
    Text(stringResource(R.string.progress, index + 1, total), style = ArbText.progressLabel, color = scheme.onSurface)
    Row(horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8), verticalAlignment = Alignment.CenterVertically) {
        val (container, content) = when (state) {
            QState.RIGHT -> arb.correctContainer to arb.onCorrectContainer
            QState.WRONG -> arb.wrongContainer to arb.onWrongContainer
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
    reasonText(q)?.let { reason ->
        if (!q.isCorrect && q.selectedIds.isNotEmpty()) {
            InfoBanner(BannerKind.NEUTRAL) { Text(reason, style = MaterialTheme.typography.bodyLarge) }
        } else Body(reason, color = scheme.onSurfaceVariant)
    }
    q.options.forEach { o -> ResolvedOption(displayOptionText(q.type, o.text), marked = o.id in q.selectedIds, correct = o.id in q.correctIds) }
    // Sin `explanation`: ni bloque ni hueco. «Per què?»: cal con filete izquierdo de 4 dp `primary` (cita del reglamento).
    q.explanation?.takeIf { it.isNotBlank() }?.let {
        val shape = MaterialTheme.shapes.medium
        Row(Modifier.fillMaxWidth().clip(shape).background(scheme.surfaceContainer, shape).height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(scheme.primary))
            Column(Modifier.weight(1f).padding(ArbDimens.s16), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
                Text(stringResource(R.string.why), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                Text(it, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            }
        }
    }
}

@Composable
private fun ResolvedOption(text: String, marked: Boolean, correct: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val arb = MaterialTheme.arb
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
    val labelColor: Color
    val borderColor: Color
    val dashed: Boolean
    val borderWidth: Float
    when {
        correct && marked -> { container = arb.correctContainer; labelColor = arb.onCorrectContainer; borderColor = arb.correct; dashed = false; borderWidth = 2f }
        correct -> { container = scheme.surface; labelColor = arb.correct; borderColor = arb.correct; dashed = true; borderWidth = 2f }
        marked -> { container = arb.wrongContainer; labelColor = arb.onWrongContainer; borderColor = arb.wrong; dashed = false; borderWidth = 2f }
        else -> { container = scheme.surface; labelColor = scheme.onSurfaceVariant; borderColor = scheme.outlineVariant; dashed = false; borderWidth = 1f }
    }
    val textColor = if (container == scheme.surface) scheme.onSurface else if (correct) arb.onCorrectContainer else arb.onWrongContainer
    Row(
        Modifier.fillMaxWidth().heightIn(min = ArbDimens.optionMin).clip(shape).background(container, shape)
            .drawBehind {
                val w = borderWidth.dp.toPx()
                drawRoundRect(
                    borderColor, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    style = Stroke(w, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null),
                )
            }
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = ArbDimens.s16, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val d = 28.sp.toScaledDp(max = 40.dp)
        // Canal 1 (lo que marcaste): disco de tinta o aro, como en el examen.
        Box(
            Modifier.size(d).clip(CircleShape).then(
                if (marked) Modifier.background(arb.mark, CircleShape) else Modifier.border(2.5.dp, scheme.outline, CircleShape),
            ),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
            Text(text, style = ArbText.option.copy(fontWeight = if (marked) FontWeight.SemiBold else FontWeight.Normal), color = textColor)
            Text(
                label, style = MaterialTheme.typography.labelMedium, color = labelColor,
                fontWeight = if (correct && !marked) FontWeight.Bold else FontWeight.SemiBold,
            )
        }
        // Canal 2 (veredicto): icono relleno al final.
        when {
            correct -> ArbIcon(vector(Icons.Default.CheckCircle), stringResource(R.string.icon_correct), tint = arb.correct, size = d)
            marked -> ArbIcon(drawable(R.drawable.ic_cancel), stringResource(R.string.icon_wrong), tint = arb.wrong, size = d)
            else -> Unit
        }
    }
}
