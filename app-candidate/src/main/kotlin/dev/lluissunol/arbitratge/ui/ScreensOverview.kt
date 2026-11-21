package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lluissunol.arbitratge.R

/**
 * C4b: resum de preguntes (pantalla completa). Llista, no quadrícula: amb 20 preguntes i la lletra al 200 % la primera
 * línia de l'enunciat ajuda a trobar la pregunta. El rellotge i els avisos segueixen actius (N11). Cap toc canvia de
 * pregunta si la selecció visible no està desada (el ViewModel ho garanteix).
 */
@Composable
fun OverviewScreen(
    st: Stage.Overview,
    a: QuestionActions,
    onBack: () -> Unit,
    onGoTo: (Int) -> Unit,
    onFirstUnanswered: () -> Unit,
    onFinish: () -> Unit,
) {
    KeepScreenOn()
    ScreenScaffold(
        title = null,
        topBar = {
            ArbTopBar(
                stringResource(R.string.overview_title), onBack,
                actions = { TimerPill(st.remainingMs, st.timeHidden, st.level, a.onToggleTime) },
            )
        },
        bottom = {
            if (st.firstUnanswered != null) SecondaryButton(stringResource(R.string.btn_first_empty), onFirstUnanswered)
            PrimaryButton(stringResource(R.string.btn_finish), onFinish, enabled = !st.finishing)
        },
    ) {
        st.banner?.let { b -> TimeBannerCard(b, a.onDismissBanner) }
        Text(
            stringResource(R.string.overview_answered, st.answeredCount, st.total),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (st.unanswered > 0) pluralStringResource(R.plurals.overview_missing, st.unanswered, st.unanswered)
            else stringResource(R.string.overview_all),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        st.items.forEach { item -> OverviewRow(item, here = item.index == st.current, onClick = { onGoTo(item.index) }) }
    }
    ExamDialogs(st.dialog, st.saveError, unanswered = st.unanswered, a = a)
}

@Composable
private fun OverviewRow(item: OverviewItem, here: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val arb = MaterialTheme.arb
    val shape = MaterialTheme.shapes.large
    val state = stringResource(if (item.answered) R.string.q_state_answered else R.string.q_state_empty)
    val spoken = if (here) state + ", " + stringResource(R.string.q_state_here) else state
    val desc = stringResource(R.string.overview_row_a11y, item.index + 1, spoken, item.text)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(shape).background(scheme.surface, shape)
            .border(if (here) BorderStroke(3.dp, scheme.onSurface) else BorderStroke(1.dp, scheme.outlineVariant), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .padding(horizontal = ArbDimens.s16, vertical = ArbDimens.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12),
    ) {
        // ● plena = amb resposta; ○ buida = sense. No és un ✔: el ✔ vol dir «correcta» (UX F4).
        val d = 28.sp.toScaledDp(max = 40.dp)
        if (item.answered) Box(Modifier.size(d).clip(CircleShape).background(arb.selection).clearAndSetSemantics { })
        else Box(Modifier.size(d).clip(CircleShape).border(2.5.dp, scheme.outline, CircleShape).clearAndSetSemantics { })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
            Text(
                stringResource(R.string.q_number, item.index + 1) + " · " + state,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (item.answered) FontWeight.Medium else FontWeight.Bold,
                color = if (item.answered) scheme.onSurfaceVariant else scheme.onSurface,
            )
            // «Ets aquí» en línia pròpia: no es parteix a mitges amb l'estat.
            if (here) Text(stringResource(R.string.q_state_here), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
            Text(item.text, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        ArbIcon(vector(Icons.AutoMirrored.Filled.KeyboardArrowRight), null, tint = scheme.onSurfaceVariant)
    }
}
