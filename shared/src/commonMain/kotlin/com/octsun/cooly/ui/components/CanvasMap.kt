package com.octsun.cooly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.domain.model.GeoPoint
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.ui.theme.CoolyColors
import com.octsun.cooly.ui.theme.LocalCoolyColors
import com.octsun.cooly.ui.theme.glyph
import com.octsun.cooly.ui.theme.pinColor
import androidx.compose.runtime.CompositionLocalProvider
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.PI

/**
 * Lightweight self-drawn map: plots the user at the center and spots by their real
 * bearing/distance. This is the cross-platform placeholder for the future Google Maps /
 * MapLibre view. Tap a pin to open its detail sheet.
 */
@Composable
fun CanvasMap(
    userLocation: GeoPoint?,
    spots: List<CoolSpot>,
    strings: Strings,
    onSpotClick: (CoolSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCoolyColors.current
    if (userLocation == null) {
        // Neutral background only — the app shell shows the loading/error state, so we avoid
        // a contradictory "couldn't get location" over the loading spinner.
        Box(modifier.fillMaxSize().background(colors.mapLand))
        return
    }

    val textMeasurer = rememberTextMeasurer()
    // Pins pop in when the spot set changes (parity with markers appearing on Google Maps).
    val appear = androidx.compose.runtime.remember { Animatable(1f) }
    LaunchedEffect(spots) {
        appear.snapTo(0f)
        appear.animateTo(1f, tween(durationMillis = 350))
    }
    // Meters spanned from center to edge; the farthest visible spot sets the zoom.
    val maxMeters = ((spots.maxOfOrNull { it.distanceMeters.toDouble() } ?: 500.0) * 1.15)
        .coerceIn(300.0, 200_000.0)

    Box(modifier.fillMaxSize().background(colors.mapLand)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(spots, maxMeters) {
                    detectTapGestures { tap ->
                        val hit = nearestPin(tap, spots, userLocation, maxMeters, size.width, size.height)
                        if (hit != null) onSpotClick(hit)
                    }
                },
        ) {
            drawGrid(colors.mapGrid)
            val cx = size.width / 2f
            val cy = size.height / 2f

            val scale = appear.value
            spots.forEach { spot ->
                val pos = project(spot.point, userLocation, maxMeters, size.width, size.height)
                drawPin(pos, spot.type.pinColor(), scale)
                if (scale > 0.6f) {
                    drawGlyph(textMeasurer, spot.type.glyph(), pos)
                    // Distance label below the pin so iOS users can compare without tapping.
                    drawLabel(textMeasurer, formatDistance(spot.distanceMeters, strings), pos, colors)
                }
            }
            // User marker on top.
            drawCircle(Color.White, radius = 13f, center = Offset(cx, cy))
            drawCircle(Color(0xFF0091EA), radius = 9f, center = Offset(cx, cy))
        }

        Surface(
            color = colors.mapLabelBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        ) {
            Text(
                "🧭 ${strings.mapView}",
                fontSize = 11.sp,
                color = colors.sub,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Equirectangular projection centered on the user; north is up. */
private fun project(
    p: GeoPoint,
    center: GeoPoint,
    maxMeters: Double,
    width: Float,
    height: Float,
): Offset {
    val cx = width / 2f
    val cy = height / 2f
    // Guard against malformed coordinates producing NaN/Infinity.
    val lat = p.lat.coerceIn(-90.0, 90.0)
    val lng = p.lng.coerceIn(-180.0, 180.0)
    if (lat.isNaN() || lng.isNaN() || center.lat.isNaN() || center.lng.isNaN()) return Offset(cx, cy)

    val metersPerDegLat = 110_540.0
    val metersPerDegLng = 111_320.0 * cos(center.lat.coerceIn(-90.0, 90.0) * PI / 180.0)
    val east = (lng - center.lng) * metersPerDegLng
    val north = (lat - center.lat) * metersPerDegLat

    val radiusPx = min(width, height) * 0.42f
    val scale = radiusPx / maxMeters
    val x = cx + (east * scale).toFloat()
    val y = cy - (north * scale).toFloat()
    if (!x.isFinite() || !y.isFinite()) return Offset(cx, cy)
    return Offset(x, y)
}

private fun nearestPin(
    tap: Offset,
    spots: List<CoolSpot>,
    center: GeoPoint,
    maxMeters: Double,
    width: Int,
    height: Int,
): CoolSpot? {
    var best: CoolSpot? = null
    var bestDist = 60f // px hit radius
    spots.forEach { spot ->
        val pos = project(spot.point, center, maxMeters, width.toFloat(), height.toFloat())
        val dx = pos.x - tap.x
        val dy = pos.y - tap.y
        val d = kotlin.math.sqrt(dx * dx + dy * dy)
        if (d < bestDist) {
            bestDist = d
            best = spot
        }
    }
    return best
}

private fun DrawScope.drawGrid(lineColor: Color) {
    val step = 60f
    val line = lineColor
    var x = 0f
    while (x < size.width) {
        drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
}

private fun DrawScope.drawPin(pos: Offset, color: Color, scale: Float = 1f) {
    drawCircle(Color.White, radius = 16f * scale, center = pos)
    drawCircle(color, radius = 13f * scale, center = pos)
}

private fun DrawScope.drawGlyph(measurer: TextMeasurer, glyph: String, pos: Offset) {
    val layout = measurer.measure(glyph, TextStyle(fontSize = 12.sp))
    drawText(
        layout,
        topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f),
    )
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, pos: Offset, colors: CoolyColors) {
    val layout = measurer.measure(text, TextStyle(fontSize = 10.sp, color = colors.ink))
    val w = layout.size.width
    val h = layout.size.height
    val topLeft = Offset(pos.x - w / 2f, pos.y + 18f)
    drawRoundRect(
        color = colors.mapLabelBg,
        topLeft = Offset(topLeft.x - 4f, topLeft.y - 2f),
        size = androidx.compose.ui.geometry.Size(w + 8f, h + 4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
    )
    drawText(layout, topLeft = topLeft)
}
