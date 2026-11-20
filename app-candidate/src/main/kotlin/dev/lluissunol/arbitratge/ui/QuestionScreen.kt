package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.lluissunol.arbitratge.R
import dev.lluissunol.arbitratge.exam.QuestionType

/** "mm:ss" del restante (04 §2.4). */
fun formatClock(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) + 999) / 1000 // redondeo hacia arriba: 00:00 solo al agotarse
    return "%02d:%02d".format(total / 60, total % 60)
}

@Composable
fun QuestionScreen(
    st: Stage.Question,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
    onToggleTime: () -> Unit,
    onDismissBanner: () -> Unit,
    onDialogConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onRetrySave: () -> Unit,
    onAbandon: () -> Unit,
) {
    KeepScreenOn()
    ScreenScaffold(
        title = null,
        topBar = { QuestionTopBar(st, onToggleTime) },
        bottom = {
            if (st.question.type == QuestionType.MULTIPLE_CHOICE) {
                val n = st.selected.size
                Text(
                    when (n) {
                        0 -> stringResource(R.string.multi_count_zero)
                        1 -> stringResource(R.string.multi_count_one)
                        else -> stringResource(R.string.multi_count_many, n)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            PrimaryButton(
                stringResource(if (st.isLast) R.string.btn_finish else R.string.btn_next),
                onNext,
                enabled = !st.saving,
            )
        },
    ) {
        st.banner?.let { b -> TimeBannerCard(b, onDismissBanner) }
        Text(
            st.question.text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Body(
            stringResource(if (st.question.type == QuestionType.MULTIPLE_CHOICE) R.string.multi_hint else R.string.single_hint),
        )
        val multiple = st.question.type == QuestionType.MULTIPLE_CHOICE
        st.question.options.forEach { opt ->
            OptionCard(
                text = opt.text,
                selected = opt.id in st.selected,
                multiple = multiple,
                enabled = !st.saving && st.saveError == null,
                onClick = { onSelect(opt.id) },
            )
        }
    }

    when (st.dialog) {
        QuestionDialog.EXIT -> AlertDialog(
            onDismissRequest = onDialogDismiss,
            title = { Text(stringResource(R.string.exit_title)) },
            text = { Text(stringResource(R.string.exit_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = onDialogDismiss, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.btn_continue), style = MaterialTheme.typography.labelLarge) } },
            dismissButton = { TextButton(onClick = onDialogConfirm, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.btn_exit), style = MaterialTheme.typography.labelLarge) } },
        )
        QuestionDialog.NO_ANSWER -> AlertDialog(
            onDismissRequest = onDialogDismiss,
            title = { Text(stringResource(R.string.noanswer_title)) },
            text = { Text(stringResource(R.string.noanswer_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = onDialogDismiss, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.btn_mark), style = MaterialTheme.typography.labelLarge) } },
            dismissButton = { TextButton(onClick = onDialogConfirm, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.btn_continue_anyway), style = MaterialTheme.typography.labelLarge) } },
        )
        QuestionDialog.FINISH -> AlertDialog(
            onDismissRequest = onDialogDismiss,
            title = { Text(stringResource(R.string.finish_title)) },
            text = { Text(stringResource(R.string.finish_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = onDialogDismiss, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.btn_review_q), style = MaterialTheme.typography.labelLarge) } },
            dismissButton = { TextButton(onClick = onDialogConfirm, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.btn_finish_ok), style = MaterialTheme.typography.labelLarge) } },
        )
        null -> Unit
    }

    // Fallo de guardado (03 §4.3, B3): diálogo modal; la respuesta sigue marcada detrás.
    st.saveError?.let { err ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.save_title)) },
            text = {
                Column(
                    Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(if (err == SaveError.RESULT) R.string.save_result_err else R.string.save_warn),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(stringResource(R.string.save_fail_exit), style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = { TextButton(onClick = onRetrySave, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.btn_retry), style = MaterialTheme.typography.labelLarge) } },
            dismissButton = { TextButton(onClick = onAbandon, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.btn_leave_exam), style = MaterialTheme.typography.labelLarge) } },
        )
    }
}

@Composable
private fun QuestionTopBar(st: Stage.Question, onToggleTime: () -> Unit) {
    val amber = Color(0xFFFFE08A)
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (st.banner ?: bannerLevel(st.remainingMs)) {
        TimeBanner.ONE_MIN -> scheme.error to scheme.onError
        TimeBanner.FIVE_MIN -> amber to Color(0xFF3B2F00)
        null -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
    val urgent = bannerLevel(st.remainingMs) != null
    Surface(color = scheme.background) {
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Anuncio de la pregunta nueva (foco inicial para TalkBack, 04 §8.2).
                Text(
                    stringResource(R.string.progress, st.index + 1, st.total),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
                )
                val minutes = (st.remainingMs / 60_000L).toInt()
                val seconds = ((st.remainingMs / 1000L) % 60L).toInt()
                val a11y = stringResource(R.string.time_left_a11y, minutes, seconds)
                Surface(
                    color = bg,
                    contentColor = fg,
                    shape = MaterialTheme.shapes.medium,
                    onClick = onToggleTime,
                    modifier = Modifier.heightIn(min = 48.dp).semantics {
                        // No se anuncia cada segundo: sin liveRegion; el usuario consulta tocando.
                        contentDescription = if (st.timeHidden) "" else a11y
                    },
                ) {
                    Text(
                        if (st.timeHidden) stringResource(R.string.time_show)
                        else (if (urgent) "⚠ " else "⏱ ") + formatClock(st.remainingMs),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (st.index + 1).toFloat() / st.total },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clearAndSetSemantics { },
            )
        }
    }
}

private fun bannerLevel(remainingMs: Long): TimeBanner? = when {
    remainingMs <= 60_000L -> TimeBanner.ONE_MIN
    remainingMs <= 300_000L -> TimeBanner.FIVE_MIN
    else -> null
}

@Composable
private fun TimeBannerCard(b: TimeBanner, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (b == TimeBanner.ONE_MIN) Color(0xFFFFD6D2) else Color(0xFFFFE08A),
            contentColor = Color(0xFF2B1B00),
        ),
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "⚠ " + stringResource(if (b == TimeBanner.ONE_MIN) R.string.warn_1 else R.string.warn_5),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            )
            val dismissText = stringResource(R.string.btn_dismiss)
            TextButton(onClick = onDismiss, Modifier.heightIn(min = 48.dp)) {
                Text("✕", style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { contentDescription = dismissText })
            }
        }
    }
}

/** Tarjeta de respuesta ≥ 56 dp, toda clicable, con rol y estado; borde de 2 dp además del color (04 §8.2). */
@Composable
private fun OptionCard(text: String, selected: Boolean, multiple: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outline)
    val stateText = stringResource(if (selected) R.string.option_selected else R.string.option_not_selected)
    val base = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    val mod = if (multiple) {
        base.toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        base.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    }
    OutlinedCard(
        border = border,
        colors = CardDefaults.outlinedCardColors(containerColor = if (selected) scheme.primaryContainer else scheme.surface),
        modifier = mod.semantics { stateDescription = stateText },
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (multiple) Checkbox(checked = selected, onCheckedChange = null) else RadioButton(selected = selected, onClick = null)
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
        }
    }
}
