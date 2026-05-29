package com.example.instagramdetector.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GrayscaleColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF222222),
    onPrimaryContainer = Color.White,
    secondary = Color.LightGray,
    onSecondary = Color.Black,
    surface = Color.Black,
    onSurface = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color.LightGray,
    outline = Color.Gray
)

@Composable
fun DetectorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GrayscaleColorScheme,
        content = content
    )
}
