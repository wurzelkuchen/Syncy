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
    primary = OwnCloudBlueLight,
    onPrimary = DarkBackground,
    primaryContainer = OwnCloudBlueDark,
    secondary = StatusSuccessGreen,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = SlateBackgroundLight,
    onSurface = SlateBackgroundLight
)

private val LightColorScheme = lightColorScheme(
    primary = OwnCloudBlue,
    onPrimary = SlateSurfaceLight,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = OwnCloudBlueDark,
    secondary = StatusSuccessGreen,
    background = SlateBackgroundLight,
    surface = SlateSurfaceLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = SlateTextPrimary,
    onSurface = SlateTextPrimary
)

@Composable
fun OwnCloudSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
