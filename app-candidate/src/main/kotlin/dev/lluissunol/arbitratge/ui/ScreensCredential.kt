package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lluissunol.arbitratge.R
import dev.lluissunol.arbitratge.credential.QrEncoder
import dev.lluissunol.arbitratge.data.StoredCredential
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun shortCode(cid: String) = cid.substring(0, 4).uppercase() + "-" + cid.substring(4, 8).uppercase()

private fun displayDate(iat: String): String =
    runCatching { LocalDate.parse(iat).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(iat)

@Composable
private fun CardField(label: String, value: String, modifier: Modifier = Modifier, code: Boolean = false) {
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = BrandColors.PaperMuted)
        Text(
            value,
            style = if (code) ArbText.code else MaterialTheme.typography.titleSmall,
            color = BrandColors.PaperInk,
        )
    }
}

/**
 * C11 (04 §3.2): la credencial com un carnet/document reconeixible. Sempre en paper clar (també amb el tema fosc; el QR
 * va sobre blanc). SENSE check verd, sense «VÀLIDA», sense segells ni hologrames (T5 / 02 §4): la banda de gespa és
 * de marca, no un indicador de validesa, i «Emesa en autoservei» viu DINS del document, al peu.
 */
@Composable
fun CredentialScreen(c: StoredCredential, onPresent: () -> Unit, onHome: () -> Unit, onBack: () -> Unit) {
    // El token guardado es lo que se codifica: nunca se reconstruye (02 §3.7.10 paso 6).
    val qr = remember(c.token) { QrEncoder.encode(c.token) }
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    ScreenScaffold(title = stringResource(R.string.cred_screen_title), onBack = onBack, bottom = {
        PrimaryButton(stringResource(R.string.btn_show_org), onPresent, icon = drawable(R.drawable.ic_id_card))
        TertiaryButton(stringResource(R.string.btn_home), onHome)
    }) {
        Surface(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = BrandColors.Paper,
            border = BorderStroke(1.dp, if (dark) Color(0xFF3A453E) else BrandColors.PaperLine),
            shadowElevation = if (dark) 0.dp else 3.dp,
        ) {
            Column {
                // Banda de gespa (marca) amb les línies de cal
                Row(
                    Modifier.fillMaxWidth().background(BrandColors.Pitch).pitchLines().heightIn(min = 72.dp)
                        .padding(horizontal = ArbDimens.s16, vertical = ArbDimens.s12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12),
                ) {
                    BrandMark(44.dp, onPitch = true)
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.cred_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                            color = Color.White, modifier = Modifier.semantics { heading() },
                        )
                        Text(stringResource(R.string.cred_subtitle), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.88f))
                    }
                }
                Column(Modifier.padding(ArbDimens.gutter), verticalArrangement = Arrangement.spacedBy(ArbDimens.s16)) {
                    Text(c.name, style = MaterialTheme.typography.headlineMedium, color = BrandColors.PaperInk)
                    val result = stringResource(R.string.cred_field_result) to stringResource(R.string.cred_value_apte)
                    val exam = stringResource(R.string.cred_field_exam) to stringResource(R.string.cred_value_exam, c.exv)
                    val date = stringResource(R.string.cred_field_date) to displayDate(c.iat)
                    val code = stringResource(R.string.cred_field_code) to shortCode(c.cid)
                    if (isLargeText()) {
                        Column(verticalArrangement = Arrangement.spacedBy(ArbDimens.s12)) {
                            CardField(result.first, result.second)
                            CardField(exam.first, exam.second)
                            CardField(date.first, date.second)
                            CardField(code.first, code.second, code = true)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s16)) {
                            CardField(result.first, result.second, Modifier.weight(1f))
                            CardField(exam.first, exam.second, Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ArbDimens.s16)) {
                            CardField(date.first, date.second, Modifier.weight(1f))
                            CardField(code.first, code.second, Modifier.weight(1f), code = true)
                        }
                    }
                    QrCode(qr, stringResource(R.string.qr_description, c.name), Modifier.fillMaxWidth())
                }
                Row(
                    Modifier.fillMaxWidth().background(BrandColors.PaperFoot).padding(ArbDimens.s16),
                    horizontalArrangement = Arrangement.spacedBy(ArbDimens.s12),
                ) {
                    ArbIcon(vector(Icons.Default.Info), null, tint = BrandColors.PaperMuted)
                    Column(verticalArrangement = Arrangement.spacedBy(ArbDimens.s4)) {
                        Text(stringResource(R.string.cred_note), style = MaterialTheme.typography.bodyMedium, color = BrandColors.PaperMuted)
                        Text(stringResource(R.string.cred_disclaimer), style = MaterialTheme.typography.bodyMedium, color = BrandColors.PaperMuted)
                    }
                }
            }
        }
    }
}

/** Mode presentació (04 §3.3): QR gran sobre blanc, brillantor al màxim, pantalla encesa; «Tancar» a baix (zona del polze). */
@Composable
fun PresentationScreen(c: StoredCredential, onClose: () -> Unit) {
    KeepScreenOn()
    MaxBrightness()
    val qr = remember(c.token) { QrEncoder.encode(c.token) }
    val black = Color.Black
    val muted = Color(0xFF3D3D3D)
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = ArbDimens.s16)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = ArbDimens.s8),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ArbDimens.s12),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ArbDimens.s8)) {
                    BrandMark(32.dp)
                    Text(stringResource(R.string.cred_title), style = MaterialTheme.typography.titleSmall, color = black, modifier = Modifier.semantics { heading() })
                }
                QrCode(qr, stringResource(R.string.qr_description, c.name), Modifier.fillMaxWidth(0.9f))
                Text(c.name, style = MaterialTheme.typography.headlineMedium, color = black, textAlign = TextAlign.Center)
                Text(stringResource(R.string.present_result, c.exv), style = MaterialTheme.typography.bodyLarge, color = black)
                Text(stringResource(R.string.cred_code, shortCode(c.cid)), style = ArbText.code, color = black)
                Text(stringResource(R.string.cred_orig), style = MaterialTheme.typography.titleSmall, color = black)
                Text(stringResource(R.string.org_present_help), style = MaterialTheme.typography.bodyMedium, color = muted, textAlign = TextAlign.Center)
                Text(stringResource(R.string.cred_how_to), style = MaterialTheme.typography.bodyMedium, color = muted, textAlign = TextAlign.Center)
            }
            OutlinedButton(
                onClick = onClose,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.5.dp, black),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = black),
                modifier = Modifier.fillMaxWidth().heightIn(min = ArbDimens.buttonMin).padding(vertical = 0.dp),
            ) {
                ArbIcon(vector(Icons.Default.Close), null, tint = black)
                androidx.compose.foundation.layout.Spacer(Modifier.padding(start = ArbDimens.s8))
                Text(stringResource(R.string.btn_close), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
        }
    }
}
