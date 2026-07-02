package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeather?,
    val hourly: HourlyWeather?,
    val daily: DailyWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val time: String,
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @Json(name = "wind_direction_10m") val windDirection10m: Double,
    @Json(name = "uv_index") val uvIndex: Double?
)

@JsonClass(generateAdapter = true)
data class HourlyWeather(
    val time: List<String>?,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>?
)

@JsonClass(generateAdapter = true)
data class DailyWeather(
    val time: List<String>?,
    val sunrise: List<String>?,
    val sunset: List<String>?,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>?
)

interface OpenMeteoService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m,uv_index",
        @Query("hourly") hourly: String = "precipitation_probability",
        @Query("daily") daily: String = "sunrise,sunset,uv_index_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 1
    ): OpenMeteoResponse
}

object WeatherClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: OpenMeteoService = retrofit.create(OpenMeteoService::class.java)
}
