package com.example

import com.example.data.local.CurrencyRateEntity
import com.example.data.local.TransactionType
import com.example.data.parser.BankNotificationParser
import com.example.ui.theme.ThemeFontStyle
import com.example.ui.theme.ThemePresets
import com.example.ui.theme.WorkspaceThemeJson
import com.example.util.CurrencyConverter
import com.example.util.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VaultFlowUnitTest {

  @Test
  fun parseABankExpenseNotification() {
    val preset = BankNotificationParser.defaultUkrainianPresets().first { it.packageName == "ua.com.abank" }
    val result = BankNotificationParser.parse("Списання 345,50 грн у Silpo Kyiv. Баланс 14 200,00 грн", preset)
    assertNotNull(result)
    assertEquals(34550L, result!!.amountMinor)
    assertEquals("UAH", result.currency)
    assertEquals(TransactionType.EXPENSE, result.transactionType)
  }

  @Test
  fun parseAllianceIncomeNotification() {
    val preset = BankNotificationParser.defaultUkrainianPresets().first { it.packageName == "ua.alliancebank.android" }
    val result = BankNotificationParser.parse("Зарахування 1250.00 EUR Переказ на картку *4412", preset)
    assertNotNull(result)
    assertEquals(125000L, result!!.amountMinor)
    assertEquals("EUR", result.currency)
    assertEquals(TransactionType.INCOME, result.transactionType)
  }

  @Test
  fun themeJsonRoundTrip() {
    val original =
      ThemePresets.cyberpunkNeon.copy(
        cornerRadiusDp = 16f,
        cardAlpha = 0.72f,
        fontStyle = ThemeFontStyle.MONOSPACE,
      )
    val encoded = WorkspaceThemeJson.encode(original)
    val decoded = WorkspaceThemeJson.decodeOrNull(encoded)
    assertNotNull(decoded)
    assertEquals(original.accentColorHex, decoded!!.accentColorHex)
    assertEquals(16f, decoded.cornerRadiusDp)
    assertEquals(ThemeFontStyle.MONOSPACE, decoded.fontStyle)
    assertEquals(original.homeBlocks.size, decoded.homeBlocks.size)
  }

  @Test
  fun multiCurrencyConversionAndFormatting() {
    val rates = listOf(CurrencyRateEntity("840_980", 840, 980, 41.0, 42.0, 0.0, 0L))
    val converted = CurrencyConverter.convertMinor(100_00L, "USD", "UAH", rates)
    assertEquals(4150_00L, converted)
    assertEquals("•••• ₴", MoneyFormatter.formatMinor(4150_00L, "UAH", hide = true))
  }
}
