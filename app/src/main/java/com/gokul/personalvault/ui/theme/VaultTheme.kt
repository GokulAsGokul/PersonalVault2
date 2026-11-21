package com.gokul.personalvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

// Premium Purple palette shared across Personal Vault screens.
val VaultBlue = Color(0xFF7652C8)
val VaultNavy = Color(0xFF29213D)
val VaultBackground = Color(0xFFF7F4FC)
val VaultSurface = Color(0xFFFFFFFF)
val VaultText = Color(0xFF241D32)
val VaultSecondary = Color(0xFF746B83)
val VaultGreen = Color(0xFF23856D)
val VaultRed = Color(0xFFD65365)

private val VaultLightColors = lightColorScheme(
    primary = Color(0xFF7652C8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DFFF),
    onPrimaryContainer = Color(0xFF321A63),
    secondary = Color(0xFF67558A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE6F8),
    onSecondaryContainer = Color(0xFF302344),
    tertiary = Color(0xFF9B5BAA),
    onTertiary = Color.White,
    background = Color(0xFFF7F4FC),
    onBackground = Color(0xFF241D32),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF241D32),
    surfaceVariant = Color(0xFFF0EBF7),
    onSurfaceVariant = Color(0xFF746B83),
    outline = Color(0xFFD8D0E4),
    outlineVariant = Color(0xFFE8E1F0),
    error = Color(0xFFD65365),
    onError = Color.White
)

private val VaultDarkColors = darkColorScheme(
    primary = Color(0xFFC7AEFF),
    onPrimary = Color(0xFF321A63),
    primaryContainer = Color(0xFF493477),
    onPrimaryContainer = Color(0xFFEBDDFF),
    secondary = Color(0xFFD0C1EA),
    onSecondary = Color(0xFF352849),
    secondaryContainer = Color(0xFF493B5F),
    onSecondaryContainer = Color(0xFFE9DDF8),
    tertiary = Color(0xFFE0A9E8),
    onTertiary = Color(0xFF4C2454),
    background = Color(0xFF14111B),
    onBackground = Color(0xFFF0EAF7),
    surface = Color(0xFF1D1926),
    onSurface = Color(0xFFF0EAF7),
    surfaceVariant = Color(0xFF302A3B),
    onSurfaceVariant = Color(0xFFC6BBD3),
    outline = Color(0xFF62586F),
    outlineVariant = Color(0xFF443B51),
    error = Color(0xFFFFB4B8),
    onError = Color(0xFF690019)
)

@Composable
fun VaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) VaultDarkColors else VaultLightColors,
        content = content
    )
}
