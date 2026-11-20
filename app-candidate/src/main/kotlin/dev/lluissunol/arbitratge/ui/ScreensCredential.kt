package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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

/** C11 (04 §3.2). Sin check verde ni "VÀLIDA": rótulo neutro "Credencial d'aptitud" (T5). */
@Composable
fun CredentialScreen(c: StoredCredential, onPresent: () -> Unit, onHome: () -> Unit, onBack: () -> Unit) {
    // El token guardado es lo que se codifica: nunca se reconstruye (02 §3.7.10 paso 6).
    val qr = remember(c.token) { QrEncoder.encode(c.token) }
    ScreenScaffold(title = stringResource(R.string.cred_screen_title), onBack = onBack, bottom = {
        PrimaryButton(stringResource(R.string.btn_show_org), onPresent)
        SecondaryButton(stringResource(R.string.btn_home), onHome)
    }) {
        Text(stringResource(R.string.cred_title).uppercase(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Body(stringResource(R.string.cred_subtitle))
        Text(c.name, style = MaterialTheme.typography.headlineMedium)
        Body(stringResource(R.string.cred_result))
        Body(stringResource(R.string.cred_exam, c.exv))
        Body(stringResource(R.string.cred_date, displayDate(c.iat)))
        Body(stringResource(R.string.cred_code, shortCode(c.cid)))
        QrCode(qr, stringResource(R.string.qr_description, c.name), Modifier.fillMaxWidth())
        Body(stringResource(R.string.cred_note))
        Text(stringResource(R.string.cred_disclaimer), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Modo presentación (04 §3.3): QR ≥ 80 % del ancho sobre blanco, brillo al máximo, pantalla encendida. */
@Composable
fun PresentationScreen(c: StoredCredential, onClose: () -> Unit) {
    KeepScreenOn()
    MaxBrightness()
    val qr = remember(c.token) { QrEncoder.encode(c.token) }
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.btn_close), style = MaterialTheme.typography.labelLarge, color = Color.Black)
            }
            QrCode(qr, stringResource(R.string.qr_description, c.name), Modifier.fillMaxWidth(0.9f))
            val black = Color.Black
            Text(c.name, style = MaterialTheme.typography.headlineMedium, color = black, textAlign = TextAlign.Center)
            Text("APTE · " + stringResource(R.string.cred_exam, c.exv).removePrefix("Examen: "), style = MaterialTheme.typography.bodyLarge, color = black)
            Text(stringResource(R.string.cred_code, shortCode(c.cid)), style = MaterialTheme.typography.titleMedium, color = black)
            Text(stringResource(R.string.cred_orig), style = MaterialTheme.typography.bodyLarge, color = black)
            Text(stringResource(R.string.org_present_help), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF333333), textAlign = TextAlign.Center)
            Text(stringResource(R.string.cred_how_to), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF333333), textAlign = TextAlign.Center)
        }
    }
}
