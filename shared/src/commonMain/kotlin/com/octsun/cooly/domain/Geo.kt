package com.octsun.cooly.domain

import com.octsun.cooly.domain.model.GeoPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_M = 6_371_000.0

/** Great-circle distance between two points, in meters (haversine). */
fun distanceMeters(a: GeoPoint, b: GeoPoint): Int {
    val dLat = (b.lat - a.lat).toRadians()
    val dLng = (b.lng - a.lng).toRadians()
    val lat1 = a.lat.toRadians()
    val lat2 = b.lat.toRadians()

    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(sqrt(h), sqrt(1 - h))
    return (EARTH_RADIUS_M * c).roundToInt()
}

/** Rough walking time from a straight-line distance, assuming ~4.5 km/h + a 1.3 street factor. */
fun walkingMinutes(distanceMeters: Int): Int {
    val walkingSpeedMetersPerMin = 75.0 // ~4.5 km/h
    val streetFactor = 1.3               // detour vs. straight line
    val minutes = (distanceMeters * streetFactor) / walkingSpeedMetersPerMin
    return minutes.roundToInt().coerceAtLeast(1)
}

private fun Double.toRadians(): Double = this * PI / 180.0
