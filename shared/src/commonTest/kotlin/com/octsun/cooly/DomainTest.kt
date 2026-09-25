package com.octsun.cooly

import com.octsun.cooly.domain.aqiLevelOf
import com.octsun.cooly.domain.distanceMeters
import com.octsun.cooly.domain.model.AqiLevel
import com.octsun.cooly.domain.model.GeoPoint
import com.octsun.cooly.domain.model.RiskLevel
import com.octsun.cooly.domain.riskLevelOf
import com.octsun.cooly.domain.walkingMinutes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomainTest {

    @Test
    fun distance_between_known_points() {
        // ~1.11 km per 0.01° of latitude.
        val a = GeoPoint(37.5665, 126.9780)
        val b = GeoPoint(37.5765, 126.9780)
        val d = distanceMeters(a, b)
        assertTrue(d in 1050..1160, "expected ~1.11km, got $d")
    }

    @Test
    fun walking_time_is_at_least_one_minute() {
        assertEquals(1, walkingMinutes(5))
        assertTrue(walkingMinutes(1000) in 10..25)
    }

    @Test
    fun aqi_levels() {
        assertEquals(AqiLevel.GOOD, aqiLevelOf(20))
        assertEquals(AqiLevel.MODERATE, aqiLevelOf(80))
        assertEquals(AqiLevel.UNHEALTHY, aqiLevelOf(130))
        assertEquals(AqiLevel.VERY_UNHEALTHY, aqiLevelOf(200))
    }

    @Test
    fun risk_takes_the_worse_axis() {
        // Comfortable temp but terrible air -> caution.
        assertEquals(RiskLevel.CAUTION, riskLevelOf(25.0, AqiLevel.UNHEALTHY))
        // Extreme heat -> danger regardless of good air.
        assertEquals(RiskLevel.DANGER, riskLevelOf(40.0, AqiLevel.GOOD))
        // Both fine -> safe.
        assertEquals(RiskLevel.SAFE, riskLevelOf(24.0, AqiLevel.GOOD))
    }
}
