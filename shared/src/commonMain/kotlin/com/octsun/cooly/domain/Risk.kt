package com.octsun.cooly.domain

import com.octsun.cooly.domain.model.AqiLevel
import com.octsun.cooly.domain.model.RiskLevel

/**
 * Classify a US AQI value into a level.
 * Breakpoints follow the US EPA AQI categories, collapsed to four buckets.
 */
fun aqiLevelOf(aqi: Int): AqiLevel = when {
    aqi <= 50 -> AqiLevel.GOOD
    aqi <= 100 -> AqiLevel.MODERATE
    aqi <= 150 -> AqiLevel.UNHEALTHY_SENSITIVE
    aqi <= 200 -> AqiLevel.UNHEALTHY
    aqi <= 300 -> AqiLevel.VERY_UNHEALTHY
    else -> AqiLevel.HAZARDOUS
}

/**
 * Overall risk from heat (apparent temperature) and air quality.
 * The worse of the two axes wins.
 */
fun riskLevelOf(feelsLikeTemp: Double, aqiLevel: AqiLevel): RiskLevel {
    val heatRisk = when {
        feelsLikeTemp >= 38 -> RiskLevel.DANGER
        feelsLikeTemp >= 31 -> RiskLevel.CAUTION
        else -> RiskLevel.SAFE
    }
    val airRisk = when (aqiLevel) {
        // EPA "Very Unhealthy" (201–300) and "Hazardous" (301+) are the genuine danger tiers.
        AqiLevel.VERY_UNHEALTHY, AqiLevel.HAZARDOUS -> RiskLevel.DANGER
        // "Unhealthy for Sensitive Groups" (101–150) and "Unhealthy" (151–200) warrant caution.
        AqiLevel.UNHEALTHY_SENSITIVE, AqiLevel.UNHEALTHY -> RiskLevel.CAUTION
        else -> RiskLevel.SAFE
    }
    // RiskLevel is an enum, so it's Comparable by ordinal; the worse level wins.
    return maxOf(heatRisk, airRisk)
}
