package com.cepprompter.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF26D7A0),
    onPrimary = Color(0xFF001E16),
    secondary = Color(0xFF79D9FF),
    background = Color(0xFF07111F),
    surface = Color(0xFF101D2C),
    surfaceVariant = Color(0xFF1A293A),
    onBackground = Color(0xFFF4F7FA),
    onSurface = Color(0xFFF4F7FA)
)

@Composable
fun CepPrompterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, content = content)
}
