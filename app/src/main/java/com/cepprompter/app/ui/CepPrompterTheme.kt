package com.cepprompter.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val Light = lightColorScheme(
    primary = Color(0xFF007F5F),
    secondary = Color(0xFF00668A),
    background = Color(0xFFF4F7FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE3EAF0),
    onBackground = Color(0xFF07111F),
    onSurface = Color(0xFF07111F)
)

@Composable
fun CepPrompterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
