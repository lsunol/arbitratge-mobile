package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lluissunol.arbitratge.R

@Composable
fun LoadingScreen(message: String? = null) {
    ScreenScaffold(title = null, topBar = { StatusInset() }) {
        Column(
            Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(64.dp)
            Spacer(Modifier.height(ArbDimens.s24))
            CircularProgressIndicator()
            if (message != null) Body(message, Modifier.padding(top = ArbDimens.s16))
        }
    }
}

/** Marge superior per a pantalles sense barra (respecta la barra d'estat i el retall). */
@Composable
fun StatusInset() {
    Spacer(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)).height(ArbDimens.s8))
}

/** Capçalera de marca: gespa profunda, línies de cal i el botó. Provisional (CFBB ha d'aprovar la identitat). */
@Composable
fun HeroCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = BrandColors.Pitch, contentColor = Color.White) {
        Column(
            Modifier.pitchLines().heightIn(min = 168.dp).padding(ArbDimens.s24),
            verticalArrangement = Arrangement.spacedBy(ArbDimens.s8),
        ) {
            BrandMark(52.dp, onPitch = true)
            Spacer(Modifier.height(ArbDimens.s8))
            Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.88f))
        }
    }
}

@Composable
private fun BenefitRow(icon: androidx.compose.ui.graphics.painter.Painter, text: String) {
    Row(Modifier.heightIn(min = ArbDimens.buttonMin), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        Body(text, Modifier.weight(1f))
    }
}

@Composable
fun WelcomeScreen(onUnderstood: () -> Unit) {
    ScreenScaffold(title = null, topBar = { StatusInset() }, bottom = {
        PrimaryButton(stringResource(R.string.btn_understood), onUnderstood)
    }) {
        HeroCard(stringResource(R.string.brand_name), stringResource(R.string.home_title))
        Gap(4)
        Heading(stringResource(R.string.welcome_title))
        Body(stringResource(R.string.welcome_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
        BenefitRow(vector(Icons.Default.Check), stringResource(R.string.welcome_b1))
        BenefitRow(drawable(R.drawable.ic_menu_book), stringResource(R.string.welcome_b2))
        BenefitRow(drawable(R.drawable.ic_id_card), stringResource(R.string.welcome_b3))
        BenefitRow(drawable(R.drawable.ic_wifi_off), stringResource(R.string.welcome_b4))
    }
}

@Composable
private fun FactTile(value: Int, label: String, icon: Int, spoken: String, modifier: Modifier = Modifier) {
    Surface(
        modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(ArbDimens.s16), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
            ArbIcon(drawable(icon), null, tint = MaterialTheme.colorScheme.primary)
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FactTiles(r: RulesSummary) {
    val q = stringResource(R.string.home_fact_questions)
    val m = stringResource(R.string.home_fact_minutes)
    val p = stringResource(R.string.home_fact_pass)
    val whole = stringResource(R.string.home_facts_a11y, r.questions, r.minutes, r.threshold)
    val tile: @Composable (Int, String, Int, Modifier) -> Unit = { v, l, i, mod -> FactTile(v, l, i, "$v $l", mod) }
    Column(Modifier.semantics { contentDescription = whole }) {
        if (isLargeText()) {
            Column(verticalArrangement = Arrangement.spacedBy(ArbDimens.itemGap)) {
                tile(r.questions, q, R.drawable.ic_quiz, Modifier.fillMaxWidth())
                tile(r.minutes, m, R.drawable.ic_timer, Modifier.fillMaxWidth())
                tile(r.threshold, p, R.drawable.ic_flag, Modifier.fillMaxWidth())
            }
        } else {
            // Tarjetas de la misma altura aunque una etiqueta ocupe dos líneas (IntrinsicSize).
            Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8)) {
                tile(r.questions, q, R.drawable.ic_quiz, Modifier.weight(1f).fillMaxHeight())
                tile(r.minutes, m, R.drawable.ic_timer, Modifier.weight(1f).fillMaxHeight())
                tile(r.threshold, p, R.drawable.ic_flag, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

/** Miniatura del carnet (no és la credencial: només la recorda). Sense cap marca de validesa. */
@Composable
private fun CredentialMini(name: String) {
    Surface(
        Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = BrandColors.Paper,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandColors.PaperLine),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(BrandColors.Pitch).pitchLines().padding(horizontal = ArbDimens.s16, vertical = ArbDimens.s12),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12),
            ) {
                BrandMark(28.dp, onPitch = true)
                Text(stringResource(R.string.cred_title), style = MaterialTheme.typography.titleSmall, color = Color.White)
            }
            Column(Modifier.padding(ArbDimens.s16), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
                Text(name, style = MaterialTheme.typography.titleSmall, color = BrandColors.PaperInk)
                Text(stringResource(R.string.home_has_cred), style = MaterialTheme.typography.bodyMedium, color = BrandColors.PaperMuted)
            }
        }
    }
}

@Composable
fun HomeScreen(
    st: Stage.Home,
    onStart: () -> Unit,
    onShowCredential: () -> Unit,
    onReviewLast: () -> Unit,
    onRetryPending: () -> Unit = {},
) {
    ScreenScaffold(title = null, topBar = { StatusInset() }, bottom = {
        if (st.credential != null) {
            PrimaryButton(stringResource(R.string.btn_see_credential), onShowCredential)
            SecondaryButton(stringResource(R.string.btn_retake), onStart)
        } else {
            PrimaryButton(stringResource(R.string.btn_start), onStart)
        }
    }) {
        HeroCard(stringResource(R.string.brand_name), stringResource(R.string.home_title))
        FactTiles(st.rules)
        st.credential?.let { CredentialMini(it.name) }
        // El resultat de l'últim intent va APART de la credencial (UX F11): dues veritats, dues targetes.
        val last = st.last
        if (last != null) {
            ArbCard {
                Text(
                    stringResource(if (last.passed) R.string.last_passed else R.string.last_failed, last.correct, last.total),
                    style = MaterialTheme.typography.bodyLarge,
                )
                TertiaryButton(stringResource(R.string.btn_review_last), onReviewLast, fillWidth = false)
            }
        } else if (st.credential == null) {
            Body(stringResource(R.string.home_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        st.pending?.let { p ->
            Callout(vector(Icons.Default.Warning), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer) {
                if (p.count > 0) Body(pluralStringResource(R.plurals.pending_cards, p.count, p.count))
                else Body(stringResource(R.string.pending_read_error))
                if (p.error && p.count > 0) {
                    Text(
                        stringResource(R.string.pending_error),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Gap(4)
                SecondaryButton(stringResource(R.string.btn_retry), onRetryPending)
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
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,
            ),
            textStyle = MaterialTheme.typography.titleMedium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onContinue() }),
            modifier = Modifier.fillMaxWidth().heightIn(min = ArbDimens.buttonMin),
        )
        Callout(vector(Icons.Default.Info), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) {
            Text(stringResource(R.string.name_warn), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RuleRow(icon: androidx.compose.ui.graphics.painter.Painter, text: String, warn: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.heightIn(min = ArbDimens.touchMin), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(
            icon,
            if (warn) scheme.tertiaryContainer else scheme.primaryContainer,
            if (warn) scheme.onTertiaryContainer else scheme.onPrimaryContainer,
        )
        Body(text, Modifier.weight(1f))
    }
}

@Composable
fun RulesScreen(st: Stage.Rules, onChangeName: () -> Unit, onBegin: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.rules_title), onBack = onBack, bottom = {
        PrimaryButton(stringResource(R.string.btn_begin), onBegin, enabled = !st.starting)
    }) {
        Column(
            Modifier.semantics { collectionInfo = CollectionInfo(8, 1) },
            verticalArrangement = Arrangement.spacedBy(ArbDimens.s12),
        ) {
            RuleRow(drawable(R.drawable.ic_timer), stringResource(R.string.rules_time, st.summary.minutes))
            RuleRow(drawable(R.drawable.ic_quiz), stringResource(R.string.rules_count, st.summary.questions))
            RuleRow(drawable(R.drawable.ic_flag), stringResource(R.string.rules_pass, st.summary.threshold))
            RuleRow(drawable(R.drawable.ic_swap_horiz), stringResource(R.string.rules_nav))
            RuleRow(drawable(R.drawable.ic_check_box), stringResource(R.string.rules_types))
            RuleRow(drawable(R.drawable.ic_remove), stringResource(R.string.rules_unanswered))
            RuleRow(vector(Icons.Default.Check), stringResource(R.string.rules_autosave))
            RuleRow(vector(Icons.AutoMirrored.Filled.ExitToApp), stringResource(R.string.rules_leave), warn = true)
        }
        ArbCard {
            Text(stringResource(R.string.rules_name_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(st.nom, style = MaterialTheme.typography.headlineSmall)
            TertiaryButton(stringResource(R.string.btn_change), onChangeName, icon = vector(Icons.Default.Edit), fillWidth = false)
        }
        if (isBatteryLow(androidx.compose.ui.platform.LocalContext.current)) {
            Callout(
                drawable(R.drawable.ic_battery_alert), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer,
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) { Text(stringResource(R.string.battery_low), style = MaterialTheme.typography.bodyLarge) }
        }
        if (st.saveError) {
            Callout(
                vector(Icons.Default.Warning), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer,
                Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            ) { Text(stringResource(R.string.start_save_error), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
fun ErrorScreen(message: String, buttonLabel: String?, onButton: (() -> Unit)?) {
    ScreenScaffold(title = null, topBar = { StatusInset() }, bottom = if (buttonLabel != null && onButton != null) {
        { PrimaryButton(buttonLabel, onButton) }
    } else null) {
        HeroCard(stringResource(R.string.brand_name), stringResource(R.string.home_title))
        Callout(vector(Icons.Default.Warning), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer) {
            Text(message, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** <20 % (03 §4.5; aviso en la pantalla previa al examen). `BatteryManager` sticky, sin permisos. */
internal fun isBatteryLow(context: android.content.Context): Boolean {
    val i = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)) ?: return false
    val level = i.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
    val scale = i.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
    return isLowBattery(level, scale)
}

/** Aviso por debajo del 20 %, esté o no cargando (03 §4.5). */
internal fun isLowBattery(level: Int, scale: Int): Boolean = level >= 0 && scale > 0 && level * 100 / scale < 20
