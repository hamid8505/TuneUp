package com.tuneup.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFFE91E63)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFF48FB1)
)

@Composable
fun TuneUpTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
