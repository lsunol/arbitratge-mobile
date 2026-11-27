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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
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

/**
 * Segell «APTE» (UI-08b): rectangle de doble vora, tinta verda, lleugerament girat i amb desgast, com estampat sobre el
 * paper. És un gràfic de mida fixa (no creix amb la lletra). SENSE check, sense «VÀLIDA» (T5 / 02 §4): descriu el resultat
 * de l'examen, no la validesa de la signatura. Dins del codi no es diu «Seal»: «segell» ja és la signatura ARB1.
 */
@Composable
fun ApteStamp(modifier: Modifier = Modifier, ink: Color = BrandColors.StampInk) {
    val spoken = "${stringResource(R.string.cred_field_result)}: ${stringResource(R.string.cred_value_apte)}"
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 1f)) {
        Box(
            modifier.size(128.dp, 64.dp)
                .graphicsLayer { rotationZ = -8f; alpha = 0.9f; compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    val o = 2.5.dp.toPx()
                    val i = 1.dp.toPx()
                    val inset = o + 3.5.dp.toPx() + i / 2
                    drawRoundRect(ink, Offset(o / 2, o / 2), Size(size.width - o, size.height - o), CornerRadius(10.dp.toPx()), style = Stroke(o))
                    drawRoundRect(ink, Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset), CornerRadius(6.dp.toPx()), style = Stroke(i))
                    drawContent()
                    val r = kotlin.random.Random(0xA27E)
                    repeat(90) {
                        drawCircle(
                            Color.Black, radius = (0.4f + r.nextFloat() * 0.8f).dp.toPx(),
                            center = Offset(r.nextFloat() * size.width, r.nextFloat() * size.height),
                            alpha = 0.35f + r.nextFloat() * 0.5f, blendMode = BlendMode.DstOut,
                        )
                    }
                }
                .testTag("apte_stamp")
                .clearAndSetSemantics { contentDescription = spoken },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.cred_value_apte), style = ArbText.stamp, color = ink, maxLines = 1)
                Text(stringResource(R.string.cred_stamp_micro), style = ArbText.stampMicro, color = ink, maxLines = 1)
            }
        }
    }
}

/** Data de l'examen: etiqueta sobre el valor (sempre apilats). */
@Composable
private fun DateField(date: String, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.cred_field_date)
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $date" }) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = BrandColors.PaperMuted)
        Text(date, style = MaterialTheme.typography.titleMedium, color = BrandColors.PaperInk)
    }
}

/**
 * C11 (UI-08): la credencial com un document en paper blanc (també amb el tema fosc), SENSE barra superior: banda de marca
 * → nom → [data | segell APTE] → QR. El segell és sobre la fila de la data, mai sobre el QR. Sense check verd ni «VÀLIDA».
 * Enrere: gest/botó del sistema (BackHandler d'ArbitratgeRoot) i «Tornar a l'inici». Només presentació: el QR (ARB1) no canvia.
 */
@Composable
fun CredentialScreen(c: StoredCredential, onPresent: () -> Unit, onHome: () -> Unit, onBack: () -> Unit) {
    // El token guardado es lo que se codifica: nunca se reconstruye (02 §3.7.10 paso 6).
    val qr = remember(c.token) { QrEncoder.encode(c.token) }
    ScreenScaffold(title = null, topBar = { StatusInset() }, pinBottom = false, bottom = {
        PrimaryButton(stringResource(R.string.btn_show_org), onPresent, icon = drawable(R.drawable.ic_qr_code))
        TertiaryButton(stringResource(R.string.btn_home), onHome, minHeight = ArbDimens.touchMin)
    }) {
        Surface(
            Modifier.fillMaxWidth().softShadow(ArbShape.document),
            shape = ArbShape.document, // un document té les cantonades petites (shape lock)
            color = BrandColors.Paper,
        ) {
            Column {
                CredentialBand(dense = true)
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(c.name, style = MaterialTheme.typography.headlineSmall, color = BrandColors.PaperInk)
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = BrandColors.PaperLine)
                    Spacer(Modifier.height(12.dp))
                    if (!isExtraLargeText()) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                            DateField(displayDate(c.iat), Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            ApteStamp(Modifier.offset(x = 6.dp, y = (-6).dp))
                        }
                    } else { // 200 %: el segell va a la seva pròpia fila
                        DateField(displayDate(c.iat), Modifier.fillMaxWidth())
                        ApteStamp(Modifier.padding(start = 8.dp, top = 12.dp, bottom = 10.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    QrCode(qr, stringResource(R.string.qr_description, c.name), Modifier.align(Alignment.CenterHorizontally).widthIn(max = 240.dp).fillMaxWidth())
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
                    Text(stringResource(R.string.cred_value_apte), style = MaterialTheme.typography.titleMedium, color = ink)
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
