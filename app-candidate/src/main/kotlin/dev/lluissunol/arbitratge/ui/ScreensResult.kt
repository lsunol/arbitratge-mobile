package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.lluissunol.arbitratge.R
import dev.lluissunol.arbitratge.exam.QuestionResult

@Composable
fun TimeUpScreen(st: Stage.TimeUp, onSeeResult: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.timeup_title), bottom = {
        PrimaryButton(stringResource(R.string.btn_see_result), onSeeResult)
    }) {
        if (st.away) Body(stringResource(R.string.timeup_away))
        Body(stringResource(R.string.timeup_body, st.answered, st.total, st.total - st.answered))
    }
}

/** C7. Con error (no se pudo desar/leer el resultado): reintento y abandono (04 §2.10). */
@Composable
fun GradingScreen(st: Stage.Grading, onRetry: () -> Unit, onAbandon: () -> Unit) {
    if (!st.error) {
        ScreenScaffold(title = null) {
            Column(
                Modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Body(stringResource(R.string.correcting), Modifier.padding(top = 16.dp))
            }
        }
    } else {
        ScreenScaffold(title = stringResource(R.string.correcting), bottom = {
            PrimaryButton(stringResource(R.string.btn_retry), onRetry)
            SecondaryButton(stringResource(R.string.btn_abandon), onAbandon)
        }) {
            Text(
                stringResource(R.string.save_result_err),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
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
    ScreenScaffold(title = stringResource(R.string.result_title), bottom = {
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
        SecondaryButton(stringResource(R.string.btn_home), onHome)
    }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                if (passed) Icons.Default.Check else Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(if (passed) R.string.passed_title else R.string.failed_title).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        Body(stringResource(R.string.result_correct, r.correct, r.total))
        Body(stringResource(R.string.result_wrong, failures))
        Body(stringResource(R.string.result_min, r.threshold))
        if (!passed) Body(stringResource(R.string.result_missing, r.threshold - r.correct))
        val secs = r.elapsedMs / 1000
        Body(stringResource(R.string.result_time, secs / 60, secs % 60))
        if (passed && failures == 0) Body(stringResource(R.string.passed_zero))
        if (passed) {
            Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                when (st.credential) {
                    is CredentialPhase.Ready -> Body(stringResource(R.string.passed_next))
                    CredentialPhase.Pending -> Text(
                        stringResource(R.string.cred_pending),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> Body(stringResource(R.string.cred_issuing))
                }
            }
        } else {
            Body(stringResource(R.string.failed_next))
        }
    }
}

@Composable
private fun reviewLabel(failures: Int) =
    if (failures == 1) stringResource(R.string.btn_review_n_one) else stringResource(R.string.btn_review_n, failures)

// ---------------------------------------------------------------------------------- C10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    st: Stage.Review,
    onBack: () -> Unit,
    onFilter: (Boolean) -> Unit,
    onOpen: (Int?) -> Unit,
) {
    val all = st.result.items.withIndex().toList()
    // Fallos primero (03 §4.2): por defecto solo fallos; "Totes" los muestra en orden con los fallos delante.
    val list = if (st.onlyWrong) all.filter { !it.value.isCorrect } else all.sortedBy { it.value.isCorrect }
    val wrongCount = all.count { !it.value.isCorrect }
    val open = st.openIndex?.let { list.getOrNull(it) }
    if (open != null) {
        val pos = st.openIndex!!
        ScreenScaffold(
            title = stringResource(R.string.review_q_title, open.index + 1, st.result.total, qStatus(open.value)),
            onBack = onBack,
            bottom = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(stringResource(R.string.btn_prev), { onOpen(pos - 1) }, Modifier.weight(1f), enabled = pos > 0)
                    SecondaryButton(stringResource(R.string.btn_next_q), { onOpen(pos + 1) }, Modifier.weight(1f), enabled = pos < list.size - 1)
                }
            },
        ) { QuestionDetail(open.value) }
    } else {
        ScreenScaffold(title = stringResource(R.string.review_title), onBack = onBack) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = st.onlyWrong,
                    onClick = { onFilter(true) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.filter_wrong, wrongCount), style = MaterialTheme.typography.bodyLarge) }
                SegmentedButton(
                    selected = !st.onlyWrong,
                    onClick = { onFilter(false) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.filter_all, all.size), style = MaterialTheme.typography.bodyLarge) }
            }
            if (list.isEmpty()) Body(stringResource(R.string.review_empty))
            list.forEachIndexed { pos, item ->
                OutlinedCard(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onOpen(pos) }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.progress, item.index + 1, st.result.total) + " · " + qStatus(item.value),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(item.value.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun qStatus(q: QuestionResult): String = when {
    q.isCorrect -> stringResource(R.string.q_right)
    q.selectedIds.isEmpty() -> stringResource(R.string.q_unanswered)
    else -> stringResource(R.string.q_wrong)
}

/** Detalle (04 §2.9): lo marcado, SIEMPRE la correcta y el porqué solo si existe. Icono + texto, nunca solo color. */
@Composable
private fun QuestionDetail(q: QuestionResult) {
    Text(q.text, style = MaterialTheme.typography.titleMedium)
    Heading(stringResource(R.string.your_answer))
    if (q.selectedIds.isEmpty()) {
        Body(stringResource(R.string.unanswered))
    } else {
        q.options.filter { it.id in q.selectedIds }.forEach { o ->
            val right = o.id in q.correctIds
            VerdictRow(right, o.text, stringResource(if (right) R.string.opt_right_marked else R.string.opt_wrong_marked))
        }
    }
    Heading(stringResource(R.string.correct_answer))
    q.options.filter { it.id in q.correctIds }.forEach { o ->
        VerdictRow(true, o.text, stringResource(if (o.id in q.selectedIds) R.string.opt_right_marked else R.string.opt_right_missed))
    }
    // Sin `explanation`: ni bloque ni hueco.
    q.explanation?.takeIf { it.isNotBlank() }?.let {
        Heading(stringResource(R.string.why))
        Body(it)
    }
}

@Composable
private fun VerdictRow(right: Boolean, text: String, status: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Icon(
            if (right) Icons.Default.Check else Icons.Default.Close,
            contentDescription = stringResource(if (right) R.string.icon_correct else R.string.icon_wrong),
            tint = if (right) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Column {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
