package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CloudSecondary,
    onPrimary = CloudOnSecondary,
    primaryContainer = CloudPrimaryContainer,
    onPrimaryContainer = CloudOnPrimaryContainer,
    secondary = CloudSecondary,
    onSecondary = CloudOnSecondary,
    secondaryContainer = CloudSecondaryContainer,
    onSecondaryContainer = CloudOnSecondaryContainer,
    tertiary = CloudTertiary,
    onTertiary = CloudOnTertiary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = CloudPrimary,
    onPrimary = CloudOnPrimary,
    primaryContainer = CloudOnPrimaryContainer,
    onPrimaryContainer = CloudPrimaryContainer,
    secondary = CloudSecondary,
    onSecondary = Color.White,
    secondaryContainer = CloudOnSecondaryContainer,
    onSecondaryContainer = CloudSecondaryContainer,
    tertiary = CloudTertiary,
    onTertiary = CloudOnTertiary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline
)

@Composable
fun OmniCloudTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded cloud security palette for high aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
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
