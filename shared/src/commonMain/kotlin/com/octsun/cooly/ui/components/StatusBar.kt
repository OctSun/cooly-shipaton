package com.octsun.cooly.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octsun.cooly.domain.model.AqiLevel
import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.domain.model.EnvStatus
import com.octsun.cooly.domain.model.RiskLevel
import com.octsun.cooly.domain.model.TemperatureUnit
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.ui.theme.CoolyPalette
import com.octsun.cooly.ui.theme.RiskDanger
import com.octsun.cooly.ui.theme.color

@Composable
fun StatusBar(
    env: EnvStatus?,
    strings: Strings,
    temperatureUnit: TemperatureUnit,
    nearestRefuge: CoolSpot?,
    isLoading: Boolean,
    onNearestClick: (CoolSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val placeholder = if (isLoading) "…" else "—"
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(color = CoolyPalette.Card, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Metric(
                    label = strings.feelsLike,
                    value = if (env?.hasTemp == true) {
                        formatTemperature(env.feelsLikeTemp, temperatureUnit)
                    } else placeholder,
                    // Neutral gray until real data arrives, so the dot doesn't imply "safe".
                    accent = if (env?.hasTemp == true) env.riskLevel.color() else CoolyPalette.Sub,
                    modifier = Modifier.weight(1f),
                )
                Metric(
                    label = strings.airQuality,
                    value = if (env?.hasAqi == true) "${strings.aqi} ${env.aqi}" else placeholder,
                    sub = if (env?.hasAqi == true) strings.aqiLevel(env.aqiLevel) else null,
                    accent = if (env?.hasAqi == true) env.aqiLevel.color() else CoolyPalette.Sub,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        when {
            env != null && env.riskLevel != RiskLevel.SAFE ->
                AdvisoryBanner(env, strings, temperatureUnit, nearestRefuge, onNearestClick)
            // On comfortable days the core answer ("where's the nearest cool spot") stays visible.
            nearestRefuge != null -> NearestStrip(nearestRefuge, strings, onNearestClick)
        }
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    sub: String? = null,
) {
    // Crossfade the risk/AQI color when conditions change (design polish).
    val animatedAccent by animateColorAsState(accent, animationSpec = tween(600))
    // Danger dot pulses gently to reinforce urgency (design polish).
    val pulse = if (accent == RiskDanger) {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        ).value
    } else 1f
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        // Real color indicator.
        Surface(
            color = animatedAccent,
            shape = CircleShape,
            modifier = Modifier.size(12.dp).graphicsLayer { scaleX = pulse; scaleY = pulse },
        ) {}
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(label, fontSize = 12.sp, color = CoolyPalette.Sub, maxLines = 1)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = CoolyPalette.Ink)
                if (sub != null) {
                    Text(
                        "  $sub",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = animatedAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvisoryBanner(
    env: EnvStatus,
    strings: Strings,
    temperatureUnit: TemperatureUnit,
    nearestRefuge: CoolSpot?,
    onNearestClick: (CoolSpot) -> Unit,
) {
    val message = when {
        env.aqiLevel == AqiLevel.VERY_UNHEALTHY || env.aqiLevel == AqiLevel.HAZARDOUS -> strings.airAdvisory
        // Weather call failed: never fabricate a temperature in a safety banner.
        !env.hasTemp -> strings.airCautionAdvisory
        env.riskLevel == RiskLevel.DANGER ->
            strings.dangerHeatAdvisory(formatTemperature(env.feelsLikeTemp, temperatureUnit))
        else -> strings.heatAdvisory(formatTemperature(env.feelsLikeTemp, temperatureUnit))
    }
    val bannerColor by animateColorAsState(env.riskLevel.color(), animationSpec = tween(600))
    // White fails contrast on the amber CAUTION banner; use a dark ink there.
    val ink = if (env.riskLevel == RiskLevel.CAUTION) Color(0xFF3E2C00) else Color.White
    Surface(color = bannerColor, modifier = Modifier.fillMaxWidth()) {
        // Breathing room: the advisory + refuge rows read cramped at vertical 10.
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = message,
                color = ink,
                // Readable in direct sunlight — this is the app's most critical text.
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (nearestRefuge != null) {
                val name = nearestRefuge.name.ifBlank { strings.spotType(nearestRefuge.type) }
                val refugeLabel = strings.nearestRefuge(name, nearestRefuge.walkingMinutes)
                // Tappable exactly like the SAFE-day NearestStrip — urgency is the worst
                // moment to remove an affordance the app taught elsewhere.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable { onNearestClick(nearestRefuge) }
                        .semantics { role = Role.Button; contentDescription = refugeLabel },
                ) {
                    Text(
                        text = "➡ $refugeLabel",
                        color = ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text("›", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NearestStrip(spot: CoolSpot, strings: Strings, onClick: (CoolSpot) -> Unit) {
    val name = spot.name.ifBlank { strings.spotType(spot.type) }
    val label = "${strings.nearestLabel}: $name · ${strings.walkMinutes(spot.walkingMinutes)}"
    // Reads as a button: min touch target, ripple, chevron.
    Surface(
        color = CoolyPalette.InfoTint,
        onClick = { onClick(spot) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { role = Role.Button; contentDescription = label },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📍", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
            Text(
                label,
                color = CoolyPalette.BrandDeep,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("›", color = CoolyPalette.BrandDeep, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
