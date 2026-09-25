package com.octsun.cooly.ui.components

import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.domain.model.TemperatureUnit
import com.octsun.cooly.i18n.Strings
import kotlin.math.roundToInt

/** Format a Celsius value in the user's chosen unit. */
fun formatTemperature(celsius: Double, unit: TemperatureUnit): String = when (unit) {
    TemperatureUnit.CELSIUS -> "${celsius.roundToInt()}°C"
    TemperatureUnit.FAHRENHEIT -> "${(celsius * 9 / 5 + 32).roundToInt()}°F"
}

/** Human distance: meters under 1 km, else one-decimal km. */
fun formatDistance(meters: Int, strings: Strings): String =
    if (meters < 1000) strings.meters(meters)
    else strings.kilometers(meters / 1000.0)

/** Fallback to a localized type label when a POI has no name. */
fun CoolSpot.displayName(strings: Strings): String =
    name.ifBlank { strings.spotType(type) }
