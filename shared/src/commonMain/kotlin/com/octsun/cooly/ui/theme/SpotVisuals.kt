package com.octsun.cooly.ui.theme

import androidx.compose.ui.graphics.Color
import com.octsun.cooly.domain.model.AqiLevel
import com.octsun.cooly.domain.model.RiskLevel
import com.octsun.cooly.domain.model.SpotType

/** Emoji glyph for a spot type — cheap, colorful, and needs no icon dependency. */
fun SpotType.glyph(): String = when (this) {
    SpotType.LIBRARY -> "📚"
    SpotType.MALL -> "🏬"
    SpotType.SUPERMARKET -> "🛒"
    SpotType.SUBWAY -> "🚇"
    SpotType.CAFE -> "☕"
    SpotType.COOLING_CENTER -> "❄️"
    SpotType.PARK -> "🌳"
    SpotType.WATER_FOUNTAIN -> "🚰"
    SpotType.TOILET -> "🚻"
    SpotType.OTHER -> "📍"
}

fun SpotType.pinColor(): Color = when (this) {
    SpotType.WATER_FOUNTAIN -> Color(0xFF00ACC1)
    SpotType.PARK -> Color(0xFF43A047)
    SpotType.TOILET -> Color(0xFF8E24AA)
    else -> Color(0xFF0091EA)
}

fun RiskLevel.color(): Color = when (this) {
    RiskLevel.SAFE -> RiskSafe
    RiskLevel.CAUTION -> RiskCaution
    RiskLevel.DANGER -> RiskDanger
}

fun AqiLevel.color(): Color = when (this) {
    AqiLevel.GOOD -> AqiGood
    AqiLevel.MODERATE -> AqiModerate
    AqiLevel.UNHEALTHY_SENSITIVE -> AqiUnhealthySensitive
    AqiLevel.UNHEALTHY -> AqiUnhealthy
    AqiLevel.VERY_UNHEALTHY -> AqiVeryUnhealthy
    AqiLevel.HAZARDOUS -> AqiHazardous
}
