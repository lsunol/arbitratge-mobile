package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.lluissunol.arbitratge.R

@Composable
fun LoadingScreen(message: String? = null) {
    ScreenScaffold(title = null) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            if (message != null) Body(message, Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
fun WelcomeScreen(onUnderstood: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.welcome_title), bottom = {
        PrimaryButton(stringResource(R.string.btn_understood), onUnderstood)
    }) {
        Body(stringResource(R.string.welcome_body))
    }
}

@Composable
fun HomeScreen(
    st: Stage.Home,
    onStart: () -> Unit,
    onShowCredential: () -> Unit,
    onReviewLast: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.app_name), bottom = {
        if (st.credential != null) {
            PrimaryButton(stringResource(R.string.btn_see_credential), onShowCredential)
            SecondaryButton(stringResource(R.string.btn_retake), onStart)
        } else {
            PrimaryButton(stringResource(R.string.btn_start), onStart)
        }
    }) {
        Heading(stringResource(R.string.home_title))
        Body(stringResource(R.string.home_summary, st.rules.questions, st.rules.minutes, st.rules.threshold))
        if (st.credential != null) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val last = st.last
                    Text(
                        if (last != null && last.passed) "✔ " + stringResource(R.string.last_passed, last.correct, last.total)
                        else "✔ " + stringResource(R.string.passed_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Body(stringResource(R.string.cred_title))
                }
            }
        } else if (st.last == null) {
            Body(stringResource(R.string.home_empty))
        }
        val last = st.last
        if (last != null) {
            if (st.credential == null) {
                Body(stringResource(R.string.last_failed, last.correct, last.total))
            }
            TextButton(onClick = onReviewLast, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.btn_review_last), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun NameScreen(st: Stage.Name, onChange: (String) -> Unit, onContinue: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.name_title), onBack = onBack, bottom = {
        PrimaryButton(stringResource(R.string.btn_continue_generic), onContinue)
    }) {
        Body(stringResource(R.string.name_help))
        OutlinedTextField(
            value = st.draft,
            onValueChange = onChange,
            label = { Text(stringResource(R.string.name_label)) },
            isError = st.problem != null,
            supportingText = {
                st.problem?.let {
                    Text(
                        stringResource(
                            when (it) {
                                NameProblem.EMPTY -> R.string.name_error
                                NameProblem.TOO_LONG -> R.string.name_error_long
                                NameProblem.INVALID_CHARS -> R.string.name_error_chars
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            },
            singleLine = false,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        )
    }
}

@Composable
fun RulesScreen(st: Stage.Rules, onChangeName: () -> Unit, onBegin: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.rules_title), onBack = onBack, bottom = {
        PrimaryButton(stringResource(R.string.btn_begin), onBegin, enabled = !st.starting)
    }) {
        Column(
            Modifier.semantics { collectionInfo = CollectionInfo(5, 1) },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RuleLine("⏱", stringResource(R.string.rules_time, st.summary.minutes))
            RuleLine("❓", stringResource(R.string.rules_count, st.summary.questions))
            RuleLine("✔", stringResource(R.string.rules_pass, st.summary.threshold))
            RuleLine("➜", stringResource(R.string.rules_noback))
            RuleLine("⚠", stringResource(R.string.rules_leave))
        }
        Gap()
        Body(stringResource(R.string.rules_name_label))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(st.nom, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onChangeName, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.btn_change), style = MaterialTheme.typography.labelLarge)
            }
        }
        if (st.saveError) {
            Text(
                stringResource(R.string.start_save_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
    }
}

@Composable
private fun RuleLine(icon: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(icon, style = MaterialTheme.typography.bodyLarge)
        Body(text)
    }
}

@Composable
fun ErrorScreen(message: String, buttonLabel: String?, onButton: (() -> Unit)?) {
    ScreenScaffold(title = stringResource(R.string.app_name), bottom = if (buttonLabel != null && onButton != null) {
        { PrimaryButton(buttonLabel, onButton) }
    } else null) {
        Text(message, style = MaterialTheme.typography.titleMedium)
    }
}
