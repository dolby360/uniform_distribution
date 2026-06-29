package com.uniformdist.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Ivory,
    primaryContainer = Bone,
    onPrimaryContainer = Ink,
    secondary = Taupe,
    onSecondary = WarmWhite,
    secondaryContainer = Linen,
    onSecondaryContainer = Color(0xFF3A352E),
    tertiary = Terracotta,
    onTertiary = WarmWhite,
    tertiaryContainer = TerracottaPale,
    onTertiaryContainer = ClayDeep,
    background = Ivory,
    onBackground = InkText,
    surface = WarmWhite,
    onSurface = InkText,
    surfaceVariant = Color(0xFFEDE7DC),
    onSurfaceVariant = MutedText,
    error = ErrorRed,
    onError = WarmWhite,
    errorContainer = ErrorRedLight,
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFFD8D0C2),
    outlineVariant = Color(0xFFE6DFD2)
)

private val DarkColorScheme = darkColorScheme(
    primary = BoneBright,
    onPrimary = Ink,
    primaryContainer = InkContainer,
    onPrimaryContainer = BoneBright,
    secondary = SandMuted,
    onSecondary = Color(0xFF2B2722),
    secondaryContainer = SandContainer,
    onSecondaryContainer = SandMuted,
    tertiary = TerracottaSoft,
    onTertiary = Color(0xFF44190A),
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = TerracottaPale,
    background = CharcoalWarm,
    onBackground = Color(0xFFEAE5DC),
    surface = CharcoalSurface,
    onSurface = Color(0xFFEAE5DC),
    surfaceVariant = Color(0xFF2B2722),
    onSurfaceVariant = DarkMutedText,
    error = ErrorRedDark,
    onError = Color(0xFF3C0A0A),
    errorContainer = ErrorRedDarkContainer,
    onErrorContainer = ErrorRedDark,
    outline = Color(0xFF57514A),
    outlineVariant = Color(0xFF3A3630)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
        shapes = AppShapes,
        content = content
    )
}
