package com.octsun.cooly.data.remote

import com.octsun.cooly.domain.aqiLevelOf
import com.octsun.cooly.domain.model.AqiLevel
import com.octsun.cooly.domain.model.EnvStatus
import com.octsun.cooly.domain.model.GeoPoint
import com.octsun.cooly.domain.riskLevelOf
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * Open-Meteo forecast + air-quality. Both free, no API key, global coverage.
 * Each call degrades independently so one failing endpoint doesn't blank the other.
 */
class WeatherApi(private val client: HttpClient) {

    suspend fun fetch(point: GeoPoint): EnvStatus = coroutineScope {
        val weatherDeferred = async { runCatching { fetchWeather(point) }.getOrNull() }
        val airDeferred = async { runCatching { fetchAir(point) }.getOrNull() }

        val current = weatherDeferred.await()?.current
        val air = airDeferred.await()?.current

        val hasTemp = current != null
        val hasAqi = air?.usAqi != null
        if (!hasTemp && !hasAqi) {
            // Both failed — signal the caller so it can show a network error.
            throw IllegalStateException("weather + air-quality both failed")
        }

        val feelsLike = current?.apparentTemperature ?: 0.0
        val aqi = air?.usAqi?.roundToInt() ?: 0
        val aqiLevel = if (hasAqi) aqiLevelOf(aqi) else AqiLevel.GOOD

        EnvStatus(
            feelsLikeTemp = feelsLike,
            temperature = current?.temperature ?: 0.0,
            humidity = current?.humidity?.roundToInt() ?: 0,
            aqi = aqi,
            aqiLevel = aqiLevel,
            riskLevel = riskLevelOf(if (hasTemp) feelsLike else -100.0, aqiLevel),
            hasTemp = hasTemp,
            hasAqi = hasAqi,
        )
    }

    /** Max apparent temperature over the next 7 days (°C) — trip-planning for watched places. */
    suspend fun fetchWeeklyMaxFeelsLike(point: GeoPoint): Double? = runCatching {
        val resp: DailyResponse = client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", point.lat)
            parameter("longitude", point.lng)
            parameter("daily", "apparent_temperature_max")
            parameter("forecast_days", 7)
            parameter("timezone", "auto")
        }.body()
        resp.daily?.apparentTemperatureMax?.filterNotNull()?.maxOrNull()
    }.getOrNull()

    private suspend fun fetchWeather(point: GeoPoint): WeatherResponse =
        client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", point.lat)
            parameter("longitude", point.lng)
            parameter(
                "current",
                "temperature_2m,relative_humidity_2m,apparent_temperature",
            )
        }.body()

    private suspend fun fetchAir(point: GeoPoint): AirResponse =
        client.get("https://air-quality-api.open-meteo.com/v1/air-quality") {
            parameter("latitude", point.lat)
            parameter("longitude", point.lng)
            parameter("current", "us_aqi")
        }.body()
}

@Serializable
private data class WeatherResponse(val current: CurrentWeather? = null)

@Serializable
private data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
)

@Serializable
private data class AirResponse(val current: CurrentAir? = null)

@Serializable
private data class DailyResponse(val daily: DailyBlock? = null)

@Serializable
private data class DailyBlock(
    @SerialName("apparent_temperature_max") val apparentTemperatureMax: List<Double?>? = null,
)

@Serializable
private data class CurrentAir(
    @SerialName("us_aqi") val usAqi: Double? = null,
)
