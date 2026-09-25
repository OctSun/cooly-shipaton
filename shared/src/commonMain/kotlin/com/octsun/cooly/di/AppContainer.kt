package com.octsun.cooly.di

import com.octsun.cooly.data.CoolyRepository
import com.octsun.cooly.data.Entitlements
import com.octsun.cooly.data.RevenueCatEntitlements
import com.octsun.cooly.data.StubEntitlements
import com.octsun.cooly.data.UserPreferences
import com.octsun.cooly.data.remote.GeocodingApi
import com.octsun.cooly.data.remote.OverpassApi
import com.octsun.cooly.data.remote.WeatherApi
import com.octsun.cooly.data.remote.createHttpClient
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.i18n.stringsFor
import com.octsun.cooly.platform.AppServices

/**
 * Wires the shared graph from the platform-provided [AppServices]. Owned by the ViewModel and
 * released in onCleared() via [close] so the HttpClient doesn't leak across config changes.
 */
class AppContainer(val services: AppServices) {
    private val http = createHttpClient()

    private val weatherApi = WeatherApi(http)

    val repository: CoolyRepository = CoolyRepository(
        weatherApi = weatherApi,
        overpassApi = OverpassApi(http),
    )

    val geocoding: GeocodingApi = GeocodingApi(http)

    /** Weather for saved places (bypasses the location-keyed cache in the repository). */
    val placeWeather: WeatherApi = weatherApi

    /** RevenueCat-backed when a platform API key is set; free-only stub otherwise.
     *  Configure runs on the launch path — never let a store-SDK init failure crash the
     *  app on start; fall back to the free stub instead. */
    val entitlements: Entitlements =
        if (RevenueCatEntitlements.isAvailable &&
            runCatching { RevenueCatEntitlements.configure() }.isSuccess
        ) {
            RevenueCatEntitlements()
        } else {
            StubEntitlements()
        }

    val strings: Strings = stringsFor(services.languageCode)

    val preferences: UserPreferences = UserPreferences(services.preferences)

    fun close() {
        http.close()
    }
}
