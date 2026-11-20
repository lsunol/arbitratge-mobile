package dev.lluissunol.arbitratge.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.lluissunol.arbitratge.R
import dev.lluissunol.arbitratge.credential.QrMatrix

/**
 * Estructura común (04 §2): barra superior, contenido con scroll (sin alturas fijas: texto al 200 %) y
 * botón primario anclado abajo respetando insets (edge-to-edge, 03 §10.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String?,
    onBack: (() -> Unit)? = null,
    topBar: (@Composable () -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (topBar != null) {
                topBar()
            } else if (title != null) {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (bottom != null) {
                Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 2.dp) {
                    Column(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) { bottom() }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.semantics { heading() })
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = modifier)
}

@Composable
fun Gap(h: Int = 8) = Spacer(Modifier.height(h.dp))

/** QR con fondo blanco puro, módulos negros y zona de silencio de 4 módulos (03 §6.5; 04 §3.1). */
@Composable
fun QrCode(matrix: QrMatrix, description: String, modifier: Modifier = Modifier) {
    val quiet = 4
    Canvas(
        modifier.aspectRatio(1f).background(Color.White).semantics { contentDescription = description },
    ) {
        val total = matrix.size + 2 * quiet
        val cell = size.minDimension / total
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix[x, y]) {
                    drawRect(Color.Black, Offset((x + quiet) * cell, (y + quiet) * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}

/** `FLAG_KEEP_SCREEN_ON` mientras hay examen o presentación (03 §4.5). */
@Composable
fun KeepScreenOn() {
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(window) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/** Brillo al máximo mientras se muestra el QR; se restaura al salir (04 §3.3). Sin permisos, sin FLAG_SECURE. */
@Composable
fun MaxBrightness() {
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(window) {
        val previous = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window?.let { w ->
            val a = w.attributes
            a.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            w.attributes = a
        }
        onDispose {
            window?.let { w ->
                val a = w.attributes
                a.screenBrightness = previous
                w.attributes = a
            }
        }
    }
}
