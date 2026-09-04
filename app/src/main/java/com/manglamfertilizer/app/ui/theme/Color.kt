package com.manglamfertilizer.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Primary Emerald / Manglam Green Palette (Agriculture, Vitality & Trust)
val Emerald900 = Color(0xFF064E3B)
val Emerald800 = Color(0xFF065F46)
val Emerald700 = Color(0xFF047857)
val Emerald600 = Color(0xFF059669)
val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Emerald300 = Color(0xFF6EE7B7)
val Emerald200 = Color(0xFFA7F3D0)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald50 = Color(0xFFECFDF5)

// Gold / Amber Accent (Harvest & Prosperity)
val GoldAmber = Color(0xFFF59E0B)
val GoldAmberLight = Color(0xFFFBBF24)
val GoldAmberDark = Color(0xFFB45309)

// Status & Alert Colors
val SoftRed = Color(0xFFEF4444)
val SoftRedBg = Color(0x25EF4444)
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberBg = Color(0x25F59E0B)
val InfoSky = Color(0xFF38BDF8)
val InfoSkyBg = Color(0x2538BDF8)
val PurpleAccent = Color(0xFFA855F7)
val PurpleAccentBg = Color(0x25A855F7)
val OrangeAccent = Color(0xFFFB923C)
val OrangeAccentBg = Color(0x25FB923C)

// Typography Colors
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF64748B)

data class ManglamColorPalette(
  val bg: Color,
  val surface: Color,
  val surfaceElevated: Color,
  val card: Color,
  val border: Color,
  val borderSubtle: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textMuted: Color,
  val isDark: Boolean
)

val DarkPalette = ManglamColorPalette(
  bg = Color(0xFF060E0A),
  surface = Color(0xFF0C1610),
  surfaceElevated = Color(0xFF122218),
  card = Color(0xFF0F1E15),
  border = Color(0xFF1B3826),
  borderSubtle = Color(0xFF152A1D),
  textPrimary = Color(0xFFF8FAFC),
  textSecondary = Color(0xFF94A3B8),
  textMuted = Color(0xFF64748B),
  isDark = true
)

val LightPalette = ManglamColorPalette(
  bg = Color(0xFFF4F7F5),
  surface = Color(0xFFFFFFFF),
  surfaceElevated = Color(0xFFE5EDE7),
  card = Color(0xFFFFFFFF),
  border = Color(0xFFCBD5E1),
  borderSubtle = Color(0xFFE2E8F0),
  textPrimary = Color(0xFF0F172A),
  textSecondary = Color(0xFF475569),
  textMuted = Color(0xFF64748B),
  isDark = false
)

val LocalManglamColors = staticCompositionLocalOf { DarkPalette }

// Theme-Aware Dynamic Color Properties
val DarkBg: Color
  @Composable
  @ReadOnlyComposable
  get() = LocalManglamColors.current.bg

val DarkSurface: Color
  @Composable
  @ReadOnlyComposable
  get() = LocalManglamColors.current.surface

val DarkSurfaceElevated: Color
  @Composable
  @ReadOnlyComposable
  get() = LocalManglamColors.current.surfaceElevated

val DarkCard: Color
  @Composable
  @ReadOnlyComposable
  get() = LocalManglamColors.current.card

val DarkBorder: Color
  @Composable
  @ReadOnlyComposable
  get() = LocalManglamColors.current.border

val DarkBorderSubtle: Color
  @Composable
  @ReadOnlyComposable
  get() = LocalManglamColors.current.borderSubtle

val TextPrimaryDark: Color
  @Composable
  @ReadOnlyComposable
  get() = LocalManglamColors.current.textPrimary

val TextSecondaryDark: Color
  @Composable
  @ReadOnlyComposable
  get() = LocalManglamColors.current.textSecondary

val TextMutedDark: Color
  @Composable
  @ReadOnlyComposable
  get() = LocalManglamColors.current.textMuted
