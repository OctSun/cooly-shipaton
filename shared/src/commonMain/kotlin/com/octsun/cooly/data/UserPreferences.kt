package com.octsun.cooly.data

import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.domain.model.EnvStatus
import com.octsun.cooly.domain.model.SavedPlace
import com.octsun.cooly.domain.model.TemperatureUnit
import com.octsun.cooly.platform.PreferencesStore
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Typed persistence for user settings + favorites, on top of the platform key-value store. */
class UserPreferences(private val store: PreferencesStore) {

    private val json = Json { ignoreUnknownKeys = true }

    fun temperatureUnit(): TemperatureUnit =
        if (store.getString(KEY_UNIT) == "F") TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS

    fun setTemperatureUnit(unit: TemperatureUnit) {
        store.putString(KEY_UNIT, if (unit == TemperatureUnit.FAHRENHEIT) "F" else "C")
    }

    fun favorites(): List<CoolSpot> {
        val raw = store.getString(KEY_FAVORITES) ?: return emptyList()
        return runCatching { json.decodeFromString<List<CoolSpot>>(raw) }
            .onFailure { println("Cooly: failed to read favorites: ${it.message}") }
            .getOrDefault(emptyList())
    }

    fun setFavorites(favorites: List<CoolSpot>) {
        store.putString(KEY_FAVORITES, json.encodeToString(favorites))
    }

    /** Whether the user has saved a favorite at least once (tailors the empty state). */
    fun hasEverSavedFavorite(): Boolean = store.getBoolean(KEY_EVER_FAV, false)
    fun markFavoriteSaved() = store.putBoolean(KEY_EVER_FAV, true)

    /** Saved regions of interest (Cooly Plus). */
    fun savedPlaces(): List<SavedPlace> {
        val raw = store.getString(KEY_PLACES) ?: return emptyList()
        return runCatching { json.decodeFromString<List<SavedPlace>>(raw) }
            .onFailure { println("Cooly: failed to read saved places: ${it.message}") }
            .getOrDefault(emptyList())
    }

    fun setSavedPlaces(places: List<SavedPlace>) {
        store.putString(KEY_PLACES, json.encodeToString(places))
    }

    /** First-run onboarding for the free-vs-rewarded model. */
    fun onboardingComplete(): Boolean = store.getBoolean(KEY_ONBOARDING, false)
    fun setOnboardingComplete() = store.putBoolean(KEY_ONBOARDING, true)

    /** Background danger alerts (heat/air) on/off. Default OFF — enabling prompts for
     *  the OS notification permission, so it must be an explicit user choice. */
    fun alertsEnabled(): Boolean = store.getBoolean(KEY_ALERTS, false)
    fun setAlertsEnabled(enabled: Boolean) = store.putBoolean(KEY_ALERTS, enabled)

    /** Last location the app loaded data for — the alert worker re-checks weather here
     *  without needing any background location permission. */
    fun setLastPoint(point: com.octsun.cooly.domain.model.GeoPoint) {
        store.putString(KEY_W_LAT, point.lat.toString())
        store.putString(KEY_W_LNG, point.lng.toString())
    }

    /**
     * Snapshot the latest reading so the home-screen widget can render it without its own
     * network/location. Stored as flat keys (feels-like always °C; the widget converts).
     */
    fun setLastReading(env: EnvStatus, timestampMillis: Long) {
        store.putString(KEY_W_FEELS_C, env.feelsLikeTemp.toString())
        store.putString(KEY_W_AQI, env.aqi.toString())
        store.putString(KEY_W_RISK, env.riskLevel.name)
        store.putString(KEY_W_AQI_LEVEL, env.aqiLevel.name)
        store.putBoolean(KEY_W_HAS_TEMP, env.hasTemp)
        store.putBoolean(KEY_W_HAS_AQI, env.hasAqi)
        store.putString(KEY_W_TIME, timestampMillis.toString())
    }

    private companion object {
        const val KEY_UNIT = "temperature_unit"
        const val KEY_FAVORITES = "favorites_json"
        const val KEY_EVER_FAV = "ever_saved_favorite"
        const val KEY_ONBOARDING = "onboarding_complete"
        const val KEY_PLACES = "saved_places_json"
        const val KEY_W_FEELS_C = "widget_feelslike_c"
        const val KEY_W_AQI = "widget_aqi"
        const val KEY_W_RISK = "widget_risk"
        const val KEY_W_AQI_LEVEL = "widget_aqi_level"
        const val KEY_W_HAS_TEMP = "widget_has_temp"
        const val KEY_W_HAS_AQI = "widget_has_aqi"
        const val KEY_W_TIME = "widget_time"
        const val KEY_W_LAT = "last_lat"
        const val KEY_W_LNG = "last_lng"
        const val KEY_ALERTS = "alerts_enabled"
    }
}
