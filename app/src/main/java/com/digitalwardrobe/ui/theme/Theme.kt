package com.digitalwardrobe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimary,
    primaryContainer = Primary,
    onPrimaryContainer = OnPrimary,
    secondary = SecondaryLight,
    onSecondary = OnSecondary,
    secondaryContainer = Secondary,
    onSecondaryContainer = OnSecondary,
    tertiary = TertiaryLight,
    onTertiary = OnTertiary,
    tertiaryContainer = Tertiary,
    onTertiaryContainer = OnTertiary,
    background = BackgroundDark,
    onBackground = SurfaceLight,
    surface = SurfaceDark,
    onSurface = SurfaceLight,
    error = ErrorDark,
    onError = OnError,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = OnPrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = OnSecondary,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryLight,
    onTertiaryContainer = OnTertiary,
    background = BackgroundLight,
    onBackground = BackgroundDark,
    surface = SurfaceLight,
    onSurface = BackgroundDark,
    error = Error,
    onError = OnError,
    outline = OutlineLight
)

@Composable
fun DigitalWardrobeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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

