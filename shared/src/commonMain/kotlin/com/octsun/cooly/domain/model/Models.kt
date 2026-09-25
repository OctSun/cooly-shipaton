package com.octsun.cooly.domain.model

import kotlinx.serialization.Serializable

/** A geographic coordinate. */
@Serializable
data class GeoPoint(
    val lat: Double,
    val lng: Double,
)

/** Category of a place the user can go to. Maps to the map pin + list icon. */
@Serializable
enum class SpotType {
    LIBRARY,
    MALL,
    SUPERMARKET,
    SUBWAY,
    CAFE,
    COOLING_CENTER,
    PARK,
    WATER_FOUNTAIN,
    TOILET,
    OTHER,
}

/** Which map layer a spot belongs to. Drives the filter toggles. */
enum class SpotLayer {
    COOL_INDOOR,   // libraries, malls, subway, cafes, cooling centers
    WATER,         // drinking water fountains
    SHADE,         // parks / green shade
    TOILET,        // public toilets (v1.1, gated behind rewarded ad)
}

val SpotType.layer: SpotLayer
    get() = when (this) {
        SpotType.WATER_FOUNTAIN -> SpotLayer.WATER
        SpotType.PARK -> SpotLayer.SHADE
        SpotType.TOILET -> SpotLayer.TOILET
        else -> SpotLayer.COOL_INDOOR
    }

/** Origin of the data, per spec 6-7. */
@Serializable
enum class SpotSource { GOOGLE_PLACES, OSM }

/** A single place shown on the map / list. */
@Serializable
data class CoolSpot(
    val id: String,
    val name: String,
    val type: SpotType,
    val point: GeoPoint,
    val distanceMeters: Int,        // computed from the user's current location
    val walkingMinutes: Int,        // estimated from distance
    val openingHours: String? = null,
    val address: String? = null,
    val source: SpotSource = SpotSource.OSM,
) {
    val layer: SpotLayer get() = type.layer
}

/** Display unit for temperature. */
enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

/**
 * A user-saved region of interest (city/neighbourhood) whose heat & air conditions are
 * watched from anywhere — the first Cooly Plus feature (travellers, families).
 */
@Serializable
data class SavedPlace(
    val id: String,             // stable id, e.g. "geo/<geonames-id>" or "pt/<lat>,<lng>"
    val name: String,
    val region: String? = null, // admin area / country for disambiguation
    val point: GeoPoint,
)

// Full US EPA AQI categories. UNHEALTHY_SENSITIVE = 101–150, UNHEALTHY = 151–200,
// VERY_UNHEALTHY = 201–300, HAZARDOUS = 301+.
enum class AqiLevel { GOOD, MODERATE, UNHEALTHY_SENSITIVE, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS }

enum class RiskLevel { SAFE, CAUTION, DANGER }

/** Current environmental status for the status bar + risk banner. */
data class EnvStatus(
    val feelsLikeTemp: Double,      // °C, apparent temperature
    val temperature: Double,        // °C, dry-bulb air temperature
    val humidity: Int,              // %
    val aqi: Int,                   // US AQI
    val aqiLevel: AqiLevel,
    val riskLevel: RiskLevel,
    val hasTemp: Boolean = true,    // false when the weather call failed but AQI still loaded
    val hasAqi: Boolean = true,     // false when the air-quality call failed
)
