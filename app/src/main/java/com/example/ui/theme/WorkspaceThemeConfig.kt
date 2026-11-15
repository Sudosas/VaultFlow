package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class ThemeFontStyle {
  DEFAULT,
  MONOSPACE,
  SERIF,
  ROUNDED,
}

@Serializable
enum class CardVisualStyle {
  GRADIENT,
  GLASS,
  MINIMAL,
}

@Serializable
enum class HomeBlockType {
  BALANCE,
  ACCOUNTS,
  QUICK_ACTIONS,
  TRANSACTIONS,
  CHART,
  GOALS,
  SUBSCRIPTIONS,
}

@Serializable
data class HomeBlockConfig(
  val type: HomeBlockType,
  val visible: Boolean = true,
)

@Serializable
data class WorkspaceThemeConfig(
  val presetName: String = "Obsidian",
  val isLightTheme: Boolean = false,
  val backgroundColorHex: String = "#0D0E12",
  val cardColorHex: String = "#16181F",
  val primaryTextColorHex: String = "#F9FAFB",
  val secondaryTextColorHex: String = "#9CA3AF",
  val accentColorHex: String = "#10B981",
  val incomeColorHex: String = "#10B981",
  val expenseColorHex: String = "#F43F5E",
  val cornerRadiusDp: Float = 18f,
  val cardBorderWidthDp: Float = 0.5f,
  val cardAlpha: Float = 0.96f,
  val cardStyle: CardVisualStyle = CardVisualStyle.GRADIENT,
  val fontStyle: ThemeFontStyle = ThemeFontStyle.DEFAULT,
  val homeBlocks: List<HomeBlockConfig> = defaultHomeBlocks(),
) {
  companion object {
    fun defaultHomeBlocks(): List<HomeBlockConfig> =
      listOf(
        HomeBlockConfig(HomeBlockType.BALANCE, visible = true),
        HomeBlockConfig(HomeBlockType.ACCOUNTS, visible = true),
        HomeBlockConfig(HomeBlockType.QUICK_ACTIONS, visible = true),
        HomeBlockConfig(HomeBlockType.TRANSACTIONS, visible = true),
        HomeBlockConfig(HomeBlockType.CHART, visible = false),
        HomeBlockConfig(HomeBlockType.GOALS, visible = false),
        HomeBlockConfig(HomeBlockType.SUBSCRIPTIONS, visible = false),
      )
  }
}

object ThemePresets {
  val obsidian =
    WorkspaceThemeConfig(
      presetName = "Obsidian",
      isLightTheme = false,
      backgroundColorHex = "#0D0E12",
      cardColorHex = "#16181F",
      primaryTextColorHex = "#F9FAFB",
      secondaryTextColorHex = "#9CA3AF",
      accentColorHex = "#10B981",
      incomeColorHex = "#10B981",
      expenseColorHex = "#F43F5E",
      cornerRadiusDp = 18f,
      cardBorderWidthDp = 0.5f,
      cardAlpha = 0.96f,
      cardStyle = CardVisualStyle.GRADIENT,
      fontStyle = ThemeFontStyle.DEFAULT,
    )

  val cyberpunkNeon =
    WorkspaceThemeConfig(
      presetName = "Cyberpunk",
      isLightTheme = false,
      backgroundColorHex = "#080612",
      cardColorHex = "#141029",
      primaryTextColorHex = "#F5F3FF",
      secondaryTextColorHex = "#A78BFA",
      accentColorHex = "#00F5D4",
      incomeColorHex = "#00F5D4",
      expenseColorHex = "#FF2A85",
      cornerRadiusDp = 12f,
      cardBorderWidthDp = 0.8f,
      cardAlpha = 0.84f,
      cardStyle = CardVisualStyle.GLASS,
      fontStyle = ThemeFontStyle.MONOSPACE,
    )

  val appleMinimal =
    WorkspaceThemeConfig(
      presetName = "Apple Minimal",
      isLightTheme = true,
      backgroundColorHex = "#F5F5F7",
      cardColorHex = "#FFFFFF",
      primaryTextColorHex = "#111827",
      secondaryTextColorHex = "#6B7280",
      accentColorHex = "#007AFF",
      incomeColorHex = "#16A34A",
      expenseColorHex = "#EF4444",
      cornerRadiusDp = 20f,
      cardBorderWidthDp = 0.5f,
      cardAlpha = 1.0f,
      cardStyle = CardVisualStyle.MINIMAL,
      fontStyle = ThemeFontStyle.DEFAULT,
    )

  val nord =
    WorkspaceThemeConfig(
      presetName = "Nord",
      isLightTheme = false,
      backgroundColorHex = "#1B202B",
      cardColorHex = "#242B38",
      primaryTextColorHex = "#ECEFF4",
      secondaryTextColorHex = "#9AA5B8",
      accentColorHex = "#88C0D0",
      incomeColorHex = "#A3BE8C",
      expenseColorHex = "#BF616A",
      cornerRadiusDp = 18f,
      cardBorderWidthDp = 0.5f,
      cardAlpha = 0.94f,
      cardStyle = CardVisualStyle.GRADIENT,
      fontStyle = ThemeFontStyle.DEFAULT,
    )

  val sunset =
    WorkspaceThemeConfig(
      presetName = "Sunset",
      isLightTheme = false,
      backgroundColorHex = "#140D16",
      cardColorHex = "#221526",
      primaryTextColorHex = "#FFF7ED",
      secondaryTextColorHex = "#C4A7BA",
      accentColorHex = "#F97316",
      incomeColorHex = "#34D399",
      expenseColorHex = "#FB7185",
      cornerRadiusDp = 24f,
      cardBorderWidthDp = 0.6f,
      cardAlpha = 0.82f,
      cardStyle = CardVisualStyle.GLASS,
      fontStyle = ThemeFontStyle.ROUNDED,
    )

  // Aliases kept for backward compatibility
  val minimalDark: WorkspaceThemeConfig get() = obsidian
  val paperLight: WorkspaceThemeConfig get() = appleMinimal
  val emeraldGlass: WorkspaceThemeConfig get() = nord

  val all: List<WorkspaceThemeConfig> =
    listOf(obsidian, cyberpunkNeon, appleMinimal, nord, sunset)
}

object WorkspaceThemeJson {
  val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun encode(config: WorkspaceThemeConfig): String = json.encodeToString(config)

  fun decodeOrNull(raw: String): WorkspaceThemeConfig? =
    runCatching {
        val parsed = json.decodeFromString<WorkspaceThemeConfig>(raw)
        val existingTypes = parsed.homeBlocks.map { it.type }.toSet()
        val missingBlocks =
          HomeBlockType.entries.filter { it !in existingTypes }.map { HomeBlockConfig(it, false) }
        parsed.copy(
          cornerRadiusDp = parsed.cornerRadiusDp.coerceIn(0f, 32f),
          cardBorderWidthDp = parsed.cardBorderWidthDp.coerceIn(0f, 4f),
          cardAlpha = parsed.cardAlpha.coerceIn(0.2f, 1f),
          homeBlocks = parsed.homeBlocks + missingBlocks,
        )
      }
      .getOrNull()

  fun decodeOrDefault(raw: String): WorkspaceThemeConfig =
    decodeOrNull(raw) ?: ThemePresets.obsidian
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF10B981)): Color {
  val cleaned = hex.trim().removePrefix("#")
  return try {
    when (cleaned.length) {
      6 -> Color(("FF$cleaned").toLong(16))
      8 -> Color(cleaned.toLong(16))
      else -> fallback
    }
  } catch (_: Exception) {
    fallback
  }
}

fun Color.toHexString(): String {
  val argb = toArgb()
  return String.format("#%06X", 0xFFFFFF and argb)
}
