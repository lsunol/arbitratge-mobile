package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lluissunol.arbitratge.R
import dev.lluissunol.arbitratge.credential.QrEncoder
import dev.lluissunol.arbitratge.data.StoredCredential
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun shortCode(cid: String) = credentialShortCode(cid)

private fun displayDate(iat: String): String = credentialDisplayDate(iat)

internal fun credentialShortCode(cid: String) = cid.substring(0, 4).uppercase() + "-" + cid.substring(4, 8).uppercase()

internal fun credentialDisplayDate(iat: String): String =
    runCatching { LocalDate.parse(iat).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(iat)

/** Fila etiqueta | valor del carnet (I8, una sola columna). Amb lletra ≥ 1,3× l'etiqueta va a sobre del valor. */
@Composable
private fun CardRow(label: String, value: String, code: Boolean = false) {
    val valueStyle = if (code) ArbText.code else MaterialTheme.typography.titleMedium
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$label: $value" }) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (isLargeText()) {
            Column(Modifier.padding(vertical = ArbDimens.s12), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = BrandColors.PaperMuted)
                Text(value, style = valueStyle, color = BrandColors.PaperInk)
            }
        } else {
            Row(
                Modifier.padding(vertical = ArbDimens.s12),
                horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12), verticalAlignment = Alignment.Top,
            ) {
                Text(
                    label, style = MaterialTheme.typography.bodyMedium, color = BrandColors.PaperMuted,
                    modifier = Modifier.padding(top = 2.dp).widthIn(max = 128.dp),
                )
                Text(value, style = valueStyle, color = BrandColors.PaperInk, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * C11 (04 §3.2; I8 + UX P2.3): la credencial com un document en paper blanc (també amb el tema fosc; el QR va sobre
 * blanc). Ordre: banda (filet groc) → nom → «APTE» → QR → detalls → avís. SENSE check verd, sense «VÀLIDA», sense
 * segells ni hologrames (T5 / 02 §4): la banda és de marca, no un indicador de validesa, i «Emesa en autoservei» viu
 * DINS del document, al peu. Només presentació: el contingut i el QR (ARB1) no canvien.
 */
@Composable
fun CredentialScreen(c: StoredCredential, onPresent: () -> Unit, onHome: () -> Unit, onBack: () -> Unit) {
    // El token guardado es lo que se codifica: nunca se reconstruye (02 §3.7.10 paso 6).
    val qr = remember(c.token) { QrEncoder.encode(c.token) }
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    ScreenScaffold(title = stringResource(R.string.cred_screen_title), onBack = onBack, pinBottom = false, bottom = {
        PrimaryButton(stringResource(R.string.btn_show_org), onPresent, icon = drawable(R.drawable.ic_qr_code))
        TertiaryButton(stringResource(R.string.btn_home), onHome, minHeight = ArbDimens.buttonMin)
    }) {
        Surface(
            Modifier.fillMaxWidth().softShadow(ArbShape.document),
            shape = ArbShape.document, // un document té les cantonades petites (shape lock)
            color = BrandColors.Paper,
        ) {
            Column {
                CredentialBand()
                Column(Modifier.padding(ArbDimens.gutter), verticalArrangement = Arrangement.spacedBy(ArbDimens.s16)) {
                    Text(c.name, style = MaterialTheme.typography.headlineSmall, color = BrandColors.PaperInk)
                    val apte = stringResource(R.string.cred_value_apte)
                    val apteSpoken = "${stringResource(R.string.cred_field_result)}: $apte"
                    Text(
                        apte, style = MaterialTheme.typography.titleMedium, color = BrandColors.Pitch,
                        modifier = Modifier.clip(CircleShape).background(BrandColors.PaperChip)
                            .padding(horizontal = ArbDimens.s16, vertical = ArbDimens.s8)
                            .semantics(mergeDescendants = true) { contentDescription = apteSpoken },
                    )
                    QrCode(qr, stringResource(R.string.qr_description, c.name), Modifier.fillMaxWidth())
                    Column {
                        CardRow(stringResource(R.string.cred_field_exam), stringResource(R.string.cred_value_exam, c.exv))
                        CardRow(stringResource(R.string.cred_field_date), displayDate(c.iat))
                        CardRow(stringResource(R.string.cred_field_code), shortCode(c.cid), code = true)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().background(BrandColors.PaperFoot).padding(ArbDimens.s16),
                    horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12),
                ) {
                    ArbIcon(vector(Icons.Outlined.Info), null, tint = BrandColors.PaperMuted)
                    Column(verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
                        Text(stringResource(R.string.cred_note), style = MaterialTheme.typography.bodyMedium, color = BrandColors.PaperMuted)
                        Text(stringResource(R.string.cred_disclaimer), style = MaterialTheme.typography.bodyMedium, color = BrandColors.PaperMuted)
                    }
                }
            }
        }
    }
}

/**
 * Mode presentació (04 §3.3; I8): pàgina blanca amb la mateixa banda i filet a sang a dalt, QR gran (90 %), brillantor al
 * màxim i pantalla encesa; «Tancar» a baix (zona del polze), secundari amb vora de gespa. Textos en `#3F4B44`.
 */
@Composable
fun PresentationScreen(c: StoredCredential, onClose: () -> Unit) {
    KeepScreenOn()
    MaxBrightness()
    val qr = remember(c.token) { QrEncoder.encode(c.token) }
    val ink = BrandColors.PaperInk
    val muted = BrandColors.PaperMuted
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize()) {
            CredentialBand(topInset = true, compact = true)
            Column(
                Modifier.weight(1f)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = ArbDimens.s16),
            ) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = ArbDimens.s12),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ArbDimens.s12),
                ) {
                    QrCode(qr, stringResource(R.string.qr_description, c.name), Modifier.fillMaxWidth(0.9f))
                    Text(c.name, style = MaterialTheme.typography.headlineSmall, color = ink, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.present_result, c.exv), style = MaterialTheme.typography.bodyLarge, color = ink)
                    Text(stringResource(R.string.cred_code, shortCode(c.cid)), style = ArbText.code, color = ink)
                    Text(stringResource(R.string.cred_orig), style = MaterialTheme.typography.titleSmall, color = ink)
                    Text(stringResource(R.string.org_present_help), style = MaterialTheme.typography.bodyMedium, color = muted, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.cred_how_to), style = MaterialTheme.typography.bodyMedium, color = muted, textAlign = TextAlign.Center)
                }
                OutlinedButton(
                    onClick = onClose,
                    shape = ArbShape.pill,
                    border = BorderStroke(2.dp, BrandColors.Pitch),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandColors.Pitch),
                    modifier = Modifier.fillMaxWidth().heightIn(min = ArbDimens.buttonMin),
                ) {
                    ArbIcon(vector(Icons.Outlined.Close), null, tint = BrandColors.Pitch)
                    Spacer(Modifier.padding(start = ArbDimens.s8))
                    Text(stringResource(R.string.btn_close), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.padding(bottom = ArbDimens.s8))
            }
        }
    }
}
