package com.manglamfertilizer.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
  primary = Emerald400,
  onPrimary = Color(0xFF060E0A),
  primaryContainer = Emerald900,
  onPrimaryContainer = Emerald100,
  secondary = GoldAmber,
  onSecondary = Color(0xFF060E0A),
  secondaryContainer = GoldAmberDark,
  onSecondaryContainer = Emerald50,
  background = Color(0xFF060E0A),
  onBackground = Color(0xFFF8FAFC),
  surface = Color(0xFF0C1610),
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0xFF122218),
  onSurfaceVariant = Color(0xFF94A3B8),
  outline = Color(0xFF1B3826),
  error = SoftRed,
  onError = Color(0xFFF8FAFC)
)

private val LightColorScheme = lightColorScheme(
  primary = Emerald700,
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Emerald100,
  onPrimaryContainer = Emerald900,
  secondary = GoldAmberDark,
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Emerald50,
  onSecondaryContainer = Emerald900,
  background = Color(0xFFF4F7F5),
  onBackground = Color(0xFF0F172A),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF0F172A),
  surfaceVariant = Color(0xFFE5EDE7),
  onSurfaceVariant = Color(0xFF475569),
  outline = Color(0xFFCBD5E1),
  error = SoftRed,
  onError = Color(0xFFFFFFFF)
)

@Composable
fun ManglamFertilizerTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val customPalette = if (darkTheme) DarkPalette else LightPalette
  val view = LocalView.current

  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.background.toArgb()
      window.navigationBarColor = colorScheme.background.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
    }
  }

  CompositionLocalProvider(LocalManglamColors provides customPalette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}
