package dev.lluissunol.arbitratge.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Esquema M3 a partir de la semilla CFBB #1d6f42 (04 §8.1); sin color dinámico. */
private val Light = lightColorScheme(
    primary = Color(0xFF1D6F42), onPrimary = Color.White,
    primaryContainer = Color(0xFFB9EFCB), onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF145530), onSecondary = Color.White,
    error = Color(0xFFB3261E), onError = Color.White,
    background = Color(0xFFF4F6F8), onBackground = Color(0xFF1F2937),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFE3E9E5), onSurfaceVariant = Color(0xFF3B4540),
    outline = Color(0xFF6B7770),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF7FD9A2), onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF0F5230), onPrimaryContainer = Color(0xFFB9EFCB),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    background = Color(0xFF121413), onBackground = Color(0xFFE6EAE7),
    surface = Color(0xFF1B1E1C), onSurface = Color(0xFFE6EAE7),
    surfaceVariant = Color(0xFF2A302C), onSurfaceVariant = Color(0xFFC3CBC5),
    outline = Color(0xFF8A958E),
)

/** Tipografía de 04 §8.1 (sp): cuerpo 18, opción 20, enunciado 22, título 24, nombre de credencial 28. */
private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 22.sp, lineHeight = 31.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    labelLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
)

@Composable
fun ArbitratgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}
