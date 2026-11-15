package com.example.data.parser

import com.example.data.local.ParsingRuleDirection
import com.example.data.local.ParsingRuleEntity
import com.example.data.local.TransactionType
import kotlin.math.roundToLong

data class ParsedNotificationResult(
  val amountMinor: Long,
  val currency: String,
  val transactionType: TransactionType,
  val merchant: String,
  val matchedRule: ParsingRuleEntity,
)

object BankNotificationParser {

  fun parse(text: String, rule: ParsingRuleEntity): ParsedNotificationResult? {
    if (text.isBlank() || rule.regexPattern.isBlank()) return null

    val regex = runCatching { Regex(rule.regexPattern, setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }
      .getOrNull() ?: return null

    val match = regex.find(text) ?: return null

    val rawAmount =
      if (rule.amountGroupIndex in 1..match.groupValues.lastIndex) {
        match.groupValues[rule.amountGroupIndex]
      } else {
        return null
      }

    val amountMinor = parseAmountToMinor(rawAmount) ?: return null
    if (amountMinor <= 0L) return null

    val rawCurrency =
      if (rule.currencyGroupIndex in 1..match.groupValues.lastIndex) {
        match.groupValues[rule.currencyGroupIndex]
      } else {
        ""
      }
    val currency = normalizeCurrency(rawCurrency.ifBlank { text })

    val merchant =
      if (rule.merchantGroupIndex in 1..match.groupValues.lastIndex) {
        match.groupValues[rule.merchantGroupIndex].trim().take(60)
      } else {
        ""
      }.ifBlank { rule.name }

    val direction =
      when (runCatching { ParsingRuleDirection.valueOf(rule.direction) }.getOrNull()) {
        ParsingRuleDirection.EXPENSE -> TransactionType.EXPENSE
        ParsingRuleDirection.INCOME -> TransactionType.INCOME
        else -> inferDirection(text, rule.keywordExpense, rule.keywordIncome)
      }

    return ParsedNotificationResult(
      amountMinor = amountMinor,
      currency = currency,
      transactionType = direction,
      merchant = merchant,
      matchedRule = rule,
    )
  }

  fun parseAmountToMinor(raw: String): Long? {
    var cleaned = raw.trim().replace("\u00A0", "").replace(" ", "")
    // Handle Ukrainian/European decimal comma vs thousands separator
    val lastComma = cleaned.lastIndexOf(',')
    val lastDot = cleaned.lastIndexOf('.')
    cleaned =
      when {
        lastComma > lastDot -> cleaned.replace(".", "").replace(',', '.')
        lastDot > lastComma -> cleaned.replace(",", "")
        else -> cleaned
      }
    val value = cleaned.toDoubleOrNull() ?: return null
    return (kotlin.math.abs(value) * 100.0).roundToLong()
  }

  fun normalizeCurrency(raw: String): String {
    val upper = raw.uppercase()
    return when {
      upper.contains("USD") || upper.contains("$") || upper.contains("ДОЛ") -> "USD"
      upper.contains("EUR") || upper.contains("€") || upper.contains("ЄВР") -> "EUR"
      upper.contains("PLN") || upper.contains("ZŁ") || upper.contains("ЗЛ") -> "PLN"
      upper.contains("GBP") || upper.contains("£") -> "GBP"
      else -> "UAH"
    }
  }

  private fun inferDirection(
    text: String,
    expenseKeywords: String,
    incomeKeywords: String,
  ): TransactionType {
    val lower = text.lowercase()
    val incomeMatches =
      incomeKeywords.split('|').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.any {
        lower.contains(it)
      }
    if (incomeMatches) return TransactionType.INCOME

    val expenseMatches =
      expenseKeywords.split('|').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.any {
        lower.contains(it)
      }
    if (expenseMatches) return TransactionType.EXPENSE

    return TransactionType.EXPENSE
  }

  fun defaultUkrainianPresets(
    aBankAccountId: Long? = null,
    allianceAccountId: Long? = null,
  ): List<ParsingRuleEntity> =
    listOf(
      ParsingRuleEntity(
        name = "А-Банк (Списання / Поповнення)",
        packageName = "ua.com.abank",
        targetAccountId = aBankAccountId,
        direction = ParsingRuleDirection.AUTO.name,
        regexPattern =
          """(?:Списання|Оплата|Покупка|Зарахування|Поповнення)\s+([0-9\s]+[.,][0-9]{2}|[0-9\s]+)\s*(UAH|USD|EUR|грн|\$|€).*?(?:в|у|:|-)?\s*([A-Za-zА-Яа-яІіЇїЄєҐґ0-9 .,'"-]{2,40})?""",
        amountGroupIndex = 1,
        currencyGroupIndex = 2,
        merchantGroupIndex = 3,
        sampleText = "Списання 345,50 грн у Silpo Kyiv. Баланс 14 200,00 грн",
        isEnabled = true,
      ),
      ParsingRuleEntity(
        name = "Альянс Банк (Push)",
        packageName = "ua.alliancebank.android",
        targetAccountId = allianceAccountId,
        direction = ParsingRuleDirection.AUTO.name,
        regexPattern =
          """(?:Оплата|Списання|Зарахування|Переказ)\s*(?:на суму)?\s*([0-9\s]+[.,][0-9]{2}|[0-9\s]+)\s*(UAH|EUR|USD|грн|€|\$)(?:\s+([A-Za-zА-Яа-яІіЇїЄєҐґ0-9 .,'"-]{2,40}))?""",
        amountGroupIndex = 1,
        currencyGroupIndex = 2,
        merchantGroupIndex = 3,
        sampleText = "Зарахування 1250.00 EUR Переказ на картку *4412",
        isEnabled = true,
      ),
      ParsingRuleEntity(
        name = "Приват24 (Push)",
        packageName = "ua.privatbank.ap24",
        targetAccountId = null,
        direction = ParsingRuleDirection.AUTO.name,
        regexPattern =
          """([0-9\s]+[.,][0-9]{2})\s*(UAH|USD|EUR|грн).*?(?:Оплата|Покупка|Зарахування|Переказ)\s*(.*)""",
        amountGroupIndex = 1,
        currencyGroupIndex = 2,
        merchantGroupIndex = 3,
        sampleText = "480.00 UAH Оплата ATB Market",
        isEnabled = true,
      ),
      ParsingRuleEntity(
        name = "ПУМБ Online (Push)",
        packageName = "ua.pumb.mobile",
        targetAccountId = null,
        direction = ParsingRuleDirection.AUTO.name,
        regexPattern =
          """(?:Покупка|Списання|Зарахування)\s+([0-9\s]+[.,][0-9]{2})\s*(UAH|USD|EUR|грн)\s*(.*)""",
        amountGroupIndex = 1,
        currencyGroupIndex = 2,
        merchantGroupIndex = 3,
        sampleText = "Покупка 195.00 UAH WOG Cafe",
        isEnabled = true,
      ),
    )
}
