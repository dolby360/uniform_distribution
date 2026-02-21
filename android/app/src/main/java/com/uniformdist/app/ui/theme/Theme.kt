package com.uniformdist.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SlateBlue,
    onPrimary = White,
    primaryContainer = SlateBlueLight,
    onPrimaryContainer = Color(0xFF0D3B6F),
    secondary = CoolGray,
    onSecondary = White,
    secondaryContainer = CoolGrayLight,
    onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = MutedTeal,
    onTertiary = White,
    tertiaryContainer = MutedTealLight,
    onTertiaryContainer = Color(0xFF0E3D4D),
    background = OffWhite,
    onBackground = CharcoalText,
    surface = White,
    onSurface = CharcoalText,
    surfaceVariant = CoolGrayLight,
    onSurfaceVariant = MutedText,
    error = ErrorRed,
    onError = White,
    errorContainer = ErrorRedLight,
    onErrorContainer = Color(0xFF5C1A1A),
    outline = Color(0xFFDADCE0),
    outlineVariant = Color(0xFFE8EAED)
)

private val DarkColorScheme = darkColorScheme(
    primary = SlateBlue80,
    onPrimary = Color(0xFF002D6E),
    primaryContainer = SlateBlueContainer,
    onPrimaryContainer = SlateBlue80,
    secondary = LightGray,
    onSecondary = Color(0xFF1F1F1F),
    secondaryContainer = LightGrayContainer,
    onSecondaryContainer = LightGray,
    tertiary = SoftTeal,
    onTertiary = Color(0xFF003544),
    tertiaryContainer = SoftTealContainer,
    onTertiaryContainer = SoftTeal,
    background = DarkBackground,
    onBackground = Color(0xFFE3E3E3),
    surface = DarkSurface,
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = DarkMutedText,
    error = ErrorRedDark,
    onError = Color(0xFF3C0A0A),
    errorContainer = ErrorRedDarkContainer,
    onErrorContainer = ErrorRedDark,
    outline = Color(0xFF5F6368),
    outlineVariant = Color(0xFF3C4043)
)

@Composable
fun UniformDistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
