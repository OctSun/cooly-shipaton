package com.octsun.cooly.data

import com.octsun.cooly.data.remote.WeatherApi
import com.octsun.cooly.data.remote.createHttpClient
import com.octsun.cooly.domain.model.EnvStatus
import com.octsun.cooly.domain.model.GeoPoint
import kotlinx.coroutines.CancellationException

/**
 * One-shot weather/AQI check for the background danger-alert workers (Android WorkManager,
 * iOS BGAppRefreshTask). Self-contained: opens and closes its own HttpClient so the worker
 * needs no AppContainer/ViewModel plumbing. Returns null on any failure — a background
 * check must never crash or retry aggressively.
 */
class AlertChecker {
    suspend fun check(lat: Double, lng: Double): EnvStatus? = try {
        val http = createHttpClient()
        try {
            WeatherApi(http).fetch(GeoPoint(lat, lng))
        } finally {
            http.close()
        }
    } catch (e: CancellationException) {
        throw e // preserve cooperative cancellation (worker stop)
    } catch (_: Exception) {
        null
    }
}
