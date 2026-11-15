package com.example.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class MonobankClientInfo(
  val clientId: String = "",
  val name: String = "",
  val webHookUrl: String = "",
  val permissions: String = "",
  val accounts: List<MonobankAccount> = emptyList(),
)

@Serializable
data class MonobankAccount(
  val id: String,
  val sendId: String = "",
  val balance: Long = 0L,
  val creditLimit: Long = 0L,
  val type: String = "black",
  val currencyCode: Int = 980,
  val cashbackType: String = "",
  val maskedPan: List<String> = emptyList(),
  val iban: String = "",
)

@Serializable
data class MonobankStatementItem(
  val id: String,
  val time: Long,
  val description: String = "",
  val mcc: Int = 0,
  val originalMcc: Int = 0,
  val hold: Boolean = false,
  val amount: Long = 0L,
  val operationAmount: Long = 0L,
  val currencyCode: Int = 980,
  val commissionRate: Long = 0L,
  val cashbackAmount: Long = 0L,
  val balance: Long = 0L,
  val comment: String? = null,
  val receiptId: String? = null,
)

@Serializable
data class MonobankCurrencyRate(
  val currencyCodeA: Int,
  val currencyCodeB: Int,
  val date: Long = 0L,
  val rateBuy: Double = 0.0,
  val rateSell: Double = 0.0,
  val rateCross: Double = 0.0,
)

object IsoCurrencyMapper {
  private val codeToIso =
    mapOf(
      980 to "UAH",
      840 to "USD",
      978 to "EUR",
      985 to "PLN",
      826 to "GBP",
      756 to "CHF",
      203 to "CZK",
    )

  private val isoToCode = codeToIso.entries.associate { (k, v) -> v to k }

  fun toIso(numericCode: Int): String = codeToIso[numericCode] ?: "UAH"

  fun toNumeric(isoCode: String): Int = isoToCode[isoCode.uppercase()] ?: 980
}
