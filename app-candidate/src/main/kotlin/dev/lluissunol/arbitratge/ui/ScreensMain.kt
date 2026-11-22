package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
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

/** Fila d'una llista informativa (I4): icona de contorn de 24 dp sense cercle, alineada amb la 1a línia; text en tinta. */
@Composable
private fun InfoRow(icon: Painter, text: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.secondary) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s16)) {
        ArbIcon(icon, null, Modifier.padding(top = 2.dp), tint = tint)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
fun WelcomeScreen(onUnderstood: () -> Unit) {
    ScreenScaffold(
        title = null, pinBottom = false,
        header = { BrandBand(stringResource(R.string.brand_name), stringResource(R.string.home_title)) },
        bottom = { PrimaryButton(stringResource(R.string.btn_understood), onUnderstood) },
    ) {
        Heading(stringResource(R.string.welcome_title))
        Body(stringResource(R.string.welcome_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
        InfoRow(vector(Icons.Outlined.Check), stringResource(R.string.welcome_b1))
        InfoRow(drawable(R.drawable.ic_menu_book), stringResource(R.string.welcome_b2))
        InfoRow(drawable(R.drawable.ic_id_card), stringResource(R.string.welcome_b3))
        InfoRow(drawable(R.drawable.ic_wifi_off), stringResource(R.string.welcome_b4))
    }
}

/** Les tres dades de l'examen amb etiquetes COMPLETES («preguntes», «minuts», «encerts per aprovar»), sense abreviar. */
@Composable
private fun RulesFactStrip(r: RulesSummary) {
    FactStrip(
        listOf(
            FactCell(r.questions.toString(), stringResource(R.string.home_fact_questions_full)),
            FactCell(r.minutes.toString(), stringResource(R.string.home_fact_minutes_full)),
            FactCell(r.threshold.toString(), stringResource(R.string.home_fact_pass_full)),
        ),
        spoken = stringResource(R.string.home_facts_a11y, r.questions, r.minutes, r.threshold),
    )
}

/** Miniatura del carnet (no és la credencial: només la recorda). Sense cap marca de validesa. */
@Composable
private fun CredentialMini(name: String) {
    Surface(
        Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = BrandColors.Paper,
        border = BorderStroke(1.dp, BrandColors.PaperLine),
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

/**
 * Inici. Banda I3 (34 % de la finestra amb lletra < 1,3× si encara no hi ha historial) + `FactStrip`. Amb historial, l'ordre és el de UX P2.1:
 * 1) estat de l'últim examen, 2) miniatura de la credencial si n'hi ha, 3) fila «Repassar l'últim examen».
 */
@Composable
fun HomeScreen(
    st: Stage.Home,
    onStart: () -> Unit,
    onShowCredential: () -> Unit,
    onReviewLast: () -> Unit,
    onRetryPending: () -> Unit = {},
) {
    ScreenScaffold(
        title = null, pinBottom = false,
        header = { BrandBand(stringResource(R.string.brand_name), stringResource(R.string.home_title), minHeightFraction = if (st.last == null && st.credential == null) 0.34f else 0f) },
        bottom = {
            if (st.credential != null) {
                PrimaryButton(stringResource(R.string.btn_see_credential), onShowCredential)
                SecondaryButton(stringResource(R.string.btn_retake), onStart)
            } else {
                PrimaryButton(stringResource(R.string.btn_start), onStart)
            }
        },
    ) {
        RulesFactStrip(st.rules)
        val last = st.last
        if (last != null) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = ArbDimens.touchMin),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12),
            ) {
                ArbIcon(vector(if (last.passed) Icons.Outlined.CheckCircle else Icons.Outlined.Info), null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    stringResource(if (last.passed) R.string.last_passed else R.string.last_failed, last.correct, last.total),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f),
                )
            }
        }
        st.credential?.let { CredentialMini(it.name) }
        if (last != null) {
            ActionRow(
                drawable(R.drawable.ic_menu_book), stringResource(R.string.btn_review_last), null, onReviewLast,
            )
        } else if (st.credential == null) {
            Body(stringResource(R.string.home_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        st.pending?.let { p ->
            InfoBanner(BannerKind.ERROR) {
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
        InfoBanner(BannerKind.NEUTRAL) {
            Text(stringResource(R.string.name_warn), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Tarja del nom (I4): vora d'1,5 dp, radi 12, el nom en `titleLarge` i «Canviar» a la mateixa fila (columna amb lletra gran). */
@Composable
private fun NameCard(nom: String, onChangeName: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val change: @Composable (Modifier) -> Unit = { m ->
        TertiaryButton(stringResource(R.string.btn_change), onChangeName, modifier = m, icon = vector(Icons.Outlined.Edit), fillWidth = false)
    }
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = scheme.surface, border = BorderStroke(1.5.dp, scheme.outline)) {
        if (isLargeText()) {
            Column(Modifier.padding(ArbDimens.s16), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
                Text(stringResource(R.string.rules_name_label), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Text(nom, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                change(Modifier.offset(x = (-16).dp))
            }
        } else {
            Row(Modifier.padding(start = ArbDimens.s16, top = ArbDimens.s12, bottom = ArbDimens.s12, end = ArbDimens.s8), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
                    Text(stringResource(R.string.rules_name_label), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    Text(nom, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                }
                change(Modifier)
            }
        }
    }
}

/**
 * Normes en tres nivells (UX P2.2): `FactStrip`, 4 regles amb icona de contorn sense cercle i l'avís ambre «Si surts de
 * l'app…» a part. `collectionInfo` = 6 elements (dades, 4 regles, avís).
 */
@Composable
fun RulesScreen(st: Stage.Rules, onChangeName: () -> Unit, onBegin: () -> Unit, onBack: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.rules_title), onBack = onBack, pinBottom = false, bottom = {
        PrimaryButton(stringResource(R.string.btn_begin), onBegin, enabled = !st.starting)
    }) {
        Column(
            Modifier.semantics { collectionInfo = CollectionInfo(6, 1) },
            verticalArrangement = Arrangement.spacedBy(ArbDimens.s16),
        ) {
            RulesFactStrip(st.summary)
            InfoRow(drawable(R.drawable.ic_swap_horiz), stringResource(R.string.rules_nav))
            InfoRow(drawable(R.drawable.ic_checklist), stringResource(R.string.rules_types))
            InfoRow(drawable(R.drawable.ic_remove), stringResource(R.string.rules_unanswered))
            InfoRow(vector(Icons.Outlined.Check), stringResource(R.string.rules_autosave))
            InfoBanner(BannerKind.WARNING) { Text(stringResource(R.string.rules_leave), style = MaterialTheme.typography.bodyLarge) }
        }
        NameCard(st.nom, onChangeName)
        if (isBatteryLow(androidx.compose.ui.platform.LocalContext.current)) {
            InfoBanner(
                BannerKind.WARNING, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, icon = drawable(R.drawable.ic_battery_alert),
            ) { Text(stringResource(R.string.battery_low), style = MaterialTheme.typography.bodyLarge) }
        }
        if (st.saveError) {
            InfoBanner(BannerKind.ERROR, Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) {
                Text(stringResource(R.string.start_save_error), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String, buttonLabel: String?, onButton: (() -> Unit)?) {
    ScreenScaffold(
        title = null,
        header = { BrandBand(stringResource(R.string.brand_name), stringResource(R.string.home_title)) },
        bottom = if (buttonLabel != null && onButton != null) {
            { PrimaryButton(buttonLabel, onButton) }
        } else null,
    ) {
        InfoBanner(BannerKind.ERROR) {
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
