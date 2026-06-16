package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ElegantDarkColorScheme = darkColorScheme(
    primary = ElegantDarkPrimary,
    secondary = ElegantDarkSecondary,
    tertiary = ElegantDarkTertiary,
    background = ElegantDarkBackground,
    surface = ElegantDarkSurface,
    onPrimary = ElegantDarkOnBackground,
    onSecondary = ElegantDarkOnBackground,
    onTertiary = ElegantDarkOnBackground,
    onBackground = ElegantDarkOnBackground,
    onSurface = ElegantDarkOnSurface,
    surfaceVariant = ElegantDarkSurface,
    onSurfaceVariant = ElegantDarkOnSurface
)

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for elegant style
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Disable to force elegant dark
  content: @Composable () -> Unit,
) {
  val colorScheme = ElegantDarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
