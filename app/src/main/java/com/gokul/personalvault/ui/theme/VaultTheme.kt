
package com.gokul.personalvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VaultBlue = Color(0xFF315CDB)
val VaultNavy = Color(0xFF14213D)
val VaultBackground = Color(0xFFF5F7FC)
val VaultSurface = Color(0xFFFFFFFF)
val VaultText = Color(0xFF172033)
val VaultSecondary = Color(0xFF64748B)
val VaultGreen = Color(0xFF16845B)
val VaultRed = Color(0xFFD94848)

private val VaultLightColors = lightColorScheme(
    primary = VaultBlue,
    onPrimary = Color.White,
    secondary = VaultNavy,
    onSecondary = Color.White,
    background = VaultBackground,
    onBackground = VaultText,
    surface = VaultSurface,
    onSurface = VaultText,
    surfaceVariant = Color(0xFFEAF0FA),
    onSurfaceVariant = VaultSecondary,
    error = VaultRed,
    onError = Color.White
)

@Composable
fun VaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VaultLightColors,
        content = content
    )
}
