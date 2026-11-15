package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val JetBrainsMonoFontFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))
val PlayfairSerifFontFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
val NunitoRoundedFontFamily = FontFamily(Font(R.font.nunito, FontWeight.Normal))

@Immutable
data class ResolvedWorkspaceTheme(
  val config: WorkspaceThemeConfig,
  val background: Color,
  val cardSurface: Color,
  val cardSurfaceAlpha: Color,
  val primaryText: Color,
  val secondaryText: Color,
  val accent: Color,
  val income: Color,
  val expense: Color,
  val cornerRadius: Dp,
  val borderWidth: Dp,
  val borderColor: Color,
  val cardStyle: CardVisualStyle,
  val fontFamily: FontFamily,
  val isLight: Boolean,
)

val LocalWorkspaceTheme = staticCompositionLocalOf {
  resolveWorkspaceTheme(ThemePresets.obsidian)
}

fun fontFamilyFor(style: ThemeFontStyle): FontFamily =
  when (style) {
    ThemeFontStyle.DEFAULT -> PlusJakartaFontFamily
    ThemeFontStyle.MONOSPACE -> JetBrainsMonoFontFamily
    ThemeFontStyle.SERIF -> PlayfairSerifFontFamily
    ThemeFontStyle.ROUNDED -> NunitoRoundedFontFamily
  }

fun resolveWorkspaceTheme(config: WorkspaceThemeConfig): ResolvedWorkspaceTheme {
  val bg = parseHexColor(config.backgroundColorHex, Color(0xFF0D0E12))
  val card = parseHexColor(config.cardColorHex, Color(0xFF16181F))
  val primaryText = parseHexColor(config.primaryTextColorHex, Color(0xFFF9FAFB))
  val secondaryText = parseHexColor(config.secondaryTextColorHex, Color(0xFF9CA3AF))
  val accent = parseHexColor(config.accentColorHex, Color(0xFF10B981))
  val income = parseHexColor(config.incomeColorHex, Color(0xFF10B981))
  val expense = parseHexColor(config.expenseColorHex, Color(0xFFF43F5E))
  val luminance = (0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue)
  val isLight = luminance > 0.5f
  val borderColor =
    if (isLight) primaryText.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
  val effectiveAlpha =
    when (config.cardStyle) {
      CardVisualStyle.GLASS -> config.cardAlpha.coerceAtMost(0.68f)
      CardVisualStyle.GRADIENT -> config.cardAlpha.coerceIn(0.88f, 0.98f)
      CardVisualStyle.MINIMAL -> 1.0f
    }

  return ResolvedWorkspaceTheme(
    config = config,
    background = bg,
    cardSurface = card,
    cardSurfaceAlpha = card.copy(alpha = effectiveAlpha),
    primaryText = primaryText,
    secondaryText = secondaryText,
    accent = accent,
    income = income,
    expense = expense,
    cornerRadius = config.cornerRadiusDp.coerceIn(0f, 32f).dp,
    borderWidth = config.cardBorderWidthDp.coerceIn(0f, 4f).dp,
    borderColor = borderColor,
    cardStyle = config.cardStyle,
    fontFamily = fontFamilyFor(config.fontStyle),
    isLight = isLight,
  )
}

private fun buildTypography(fontFamily: FontFamily): Typography =
  Typography(
    displaySmall =
      TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = "tnum",
      ),
    headlineMedium =
      TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum",
      ),
    titleLarge =
      TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontFeatureSettings = "tnum",
      ),
    titleMedium =
      TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = "tnum",
      ),
    bodyLarge =
      TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
      ),
    labelMedium =
      TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontFeatureSettings = "tnum",
      ),
  )

@Composable
fun VaultFlowTheme(
  themeConfig: WorkspaceThemeConfig = ThemePresets.obsidian,
  content: @Composable () -> Unit,
) {
  val resolved = resolveWorkspaceTheme(themeConfig)
  val onAccent =
    if ((0.299f * resolved.accent.red + 0.587f * resolved.accent.green + 0.114f * resolved.accent.blue) > 0.55f) {
      Color(0xFF0A0F14)
    } else {
      Color.White
    }

  val colorScheme =
    if (resolved.isLight) {
      lightColorScheme(
        primary = resolved.accent,
        onPrimary = onAccent,
        primaryContainer = resolved.accent.copy(alpha = 0.12f),
        onPrimaryContainer = resolved.primaryText,
        secondary = resolved.income,
        onSecondary = Color.White,
        tertiary = resolved.expense,
        background = resolved.background,
        onBackground = resolved.primaryText,
        surface = resolved.cardSurface,
        onSurface = resolved.primaryText,
        surfaceVariant = resolved.cardSurface,
        onSurfaceVariant = resolved.secondaryText,
        outline = resolved.borderColor,
        error = resolved.expense,
      )
    } else {
      darkColorScheme(
        primary = resolved.accent,
        onPrimary = onAccent,
        primaryContainer = resolved.accent.copy(alpha = 0.16f),
        onPrimaryContainer = resolved.primaryText,
        secondary = resolved.income,
        onSecondary = Color(0xFF041C11),
        tertiary = resolved.expense,
        background = resolved.background,
        onBackground = resolved.primaryText,
        surface = resolved.cardSurface,
        onSurface = resolved.primaryText,
        surfaceVariant = resolved.cardSurface,
        onSurfaceVariant = resolved.secondaryText,
        outline = resolved.borderColor,
        error = resolved.expense,
      )
    }

  val shapes =
    Shapes(
      small = RoundedCornerShape((resolved.cornerRadius * 0.55f).coerceAtLeast(6.dp)),
      medium = RoundedCornerShape(resolved.cornerRadius),
      large = RoundedCornerShape((resolved.cornerRadius * 1.25f).coerceAtMost(32.dp)),
    )

  CompositionLocalProvider(LocalWorkspaceTheme provides resolved) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = buildTypography(resolved.fontFamily),
      shapes = shapes,
      content = content,
    )
  }
}
