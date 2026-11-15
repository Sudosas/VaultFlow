package com.example.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface MonobankApiService {
  @GET("personal/client-info")
  suspend fun getClientInfo(@Header("X-Token") token: String): MonobankClientInfo

  @GET("personal/statement/{account}/{from}/{to}")
  suspend fun getStatement(
    @Header("X-Token") token: String,
    @Path("account") accountId: String,
    @Path("from") fromEpochSeconds: Long,
    @Path("to") toEpochSeconds: Long,
  ): List<MonobankStatementItem>

  @GET("bank/currency")
  suspend fun getCurrencyRates(): List<MonobankCurrencyRate>

  companion object {
    private const val BASE_URL = "https://api.monobank.ua/"

    fun create(): MonobankApiService {
      val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
      }
      val logging =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
      val client =
        OkHttpClient.Builder()
          .addInterceptor(logging)
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(20, TimeUnit.SECONDS)
          .build()

      return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(MonobankApiService::class.java)
    }
  }
}
