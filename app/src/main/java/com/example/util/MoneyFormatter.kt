package com.example.util

import com.example.data.local.CurrencyRateEntity
import com.example.data.remote.IsoCurrencyMapper
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object MoneyFormatter {
  private val symbols =
    DecimalFormatSymbols(Locale.US).apply {
      groupingSeparator = ' '
      decimalSeparator = '.'
    }

  private val decimalFormat = DecimalFormat("#,##0.00", symbols)

  fun symbolFor(currency: String): String =
    when (currency.uppercase()) {
      "UAH" -> "₴"
      "USD" -> "$"
      "EUR" -> "€"
      "PLN" -> "zł"
      "GBP" -> "£"
      "CHF" -> "CHF"
      "CZK" -> "Kč"
      else -> currency
    }

  fun formatMinor(
    amountMinor: Long,
    currency: String,
    hide: Boolean = false,
    showPlus: Boolean = false,
  ): String {
    val sym = symbolFor(currency)
    if (hide) return "•••• $sym"
    val major = abs(amountMinor) / 100.0
    val formatted = decimalFormat.format(major)
    val prefix =
      when {
        amountMinor < 0 -> "-"
        showPlus && amountMinor > 0 -> "+"
        else -> ""
      }
    return "$prefix$formatted $sym"
  }

  fun formatRelativeDate(epochMillis: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMillis))
  }

  fun formatShortDate(epochMillis: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(epochMillis))
  }

  fun mapMccToCategory(mcc: Int, amountMinor: Long): String {
    if (amountMinor > 0) return "SALARY"
    return when (mcc) {
      in 5411..5499 -> "GROCERIES"
      in 5811..5814 -> "DINING"
      in 4111..4131, 4789, 5541, 5542 -> "TRANSPORT"
      4814, 4899, 4900 -> "UTILITIES"
      5732, 5734, 5815, 5817, 5818, 7372 -> "TECH"
      5912, 8011, 8062, 8099 -> "HEALTH"
      7832, 7922, 7999 -> "ENTERTAINMENT"
      else -> "OTHER"
    }
  }
}

object CurrencyConverter {
  // Fallback rates relative to 1 unit in UAH when offline before first sync
  private val fallbackToUah =
    mapOf(
      "UAH" to 1.0,
      "USD" to 41.35,
      "EUR" to 44.80,
      "PLN" to 10.45,
      "GBP" to 52.60,
      "CHF" to 47.10,
      "CZK" to 1.78,
    )

  fun rateToUah(currency: String, rates: List<CurrencyRateEntity>): Double {
    val iso = currency.uppercase()
    if (iso == "UAH") return 1.0
    val codeA = IsoCurrencyMapper.toNumeric(iso)
    val direct = rates.firstOrNull { it.currencyCodeA == codeA && it.currencyCodeB == 980 }
    if (direct != null) {
      val avg =
        when {
          direct.rateBuy > 0 && direct.rateSell > 0 -> (direct.rateBuy + direct.rateSell) / 2.0
          direct.rateCross > 0 -> direct.rateCross
          direct.rateBuy > 0 -> direct.rateBuy
          else -> 0.0
        }
      if (avg > 0.0) return avg
    }
    return fallbackToUah[iso] ?: 1.0
  }

  fun convertMinor(
    amountMinor: Long,
    fromCurrency: String,
    toCurrency: String,
    rates: List<CurrencyRateEntity>,
  ): Long {
    if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amountMinor
    val fromInUah = rateToUah(fromCurrency, rates)
    val toInUah = rateToUah(toCurrency, rates)
    if (toInUah <= 0.0) return amountMinor
    return (amountMinor * (fromInUah / toInUah)).roundToLong()
  }
}
