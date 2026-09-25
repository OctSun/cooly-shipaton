package com.octsun.cooly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.domain.model.GeoPoint
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.platform.NativeMapBridge
import com.octsun.cooly.platform.NativeMapMarker
import com.octsun.cooly.ui.theme.LocalCoolyColors
import com.octsun.cooly.ui.theme.NIGHT_MAP_STYLE_JSON

/**
 * iOS map surface. Renders the native Google Map when the Swift host registered a
 * [NativeMapBridge.factory] (SDK bundled + API key set); otherwise falls back to the
 * self-drawn [CanvasMap] so the app keeps working without the SDK.
 */
@Composable
actual fun SpotMap(
    userLocation: GeoPoint?,
    spots: List<CoolSpot>,
    strings: Strings,
    onSpotClick: (CoolSpot) -> Unit,
    modifier: Modifier,
) {
    val factory = NativeMapBridge.factory
    if (factory == null) {
        CanvasMap(
            userLocation = userLocation,
            spots = spots,
            strings = strings,
            onSpotClick = onSpotClick,
            modifier = modifier,
        )
        return
    }

    val colors = LocalCoolyColors.current
    if (userLocation == null) {
        // Neutral background only — the app shell shows the loading/error state.
        Box(modifier.fillMaxSize().background(colors.mapLand))
        return
    }

    // Keep tap handling correct across recompositions without recreating the map.
    val currentSpots by rememberUpdatedState(spots)
    val currentOnClick by rememberUpdatedState(onSpotClick)

    val controller = remember(factory) {
        factory.create { id ->
            currentSpots.firstOrNull { it.id == id }?.let { currentOnClick(it) }
        }.apply {
            setCamera(userLocation.lat, userLocation.lng, DEFAULT_ZOOM, animated = false)
            setMyLocationEnabled(true)
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }

    // Re-center when the user's location changes (parity with Android's animated camera).
    // The initial position was set at creation, so skip the first emission.
    val lastCentered = remember { arrayOf(userLocation.lat to userLocation.lng) }
    LaunchedEffect(userLocation.lat, userLocation.lng) {
        val target = userLocation.lat to userLocation.lng
        if (lastCentered[0] != target) {
            lastCentered[0] = target
            controller.setCamera(userLocation.lat, userLocation.lng, DEFAULT_ZOOM, animated = true)
        }
    }

    // Same night style JSON as Android's MapStyleOptions for cross-platform parity.
    LaunchedEffect(colors.isDark) {
        controller.setMapStyleJson(if (colors.isDark) NIGHT_MAP_STYLE_JSON else null)
    }

    LaunchedEffect(spots) {
        controller.setMarkers(
            spots.map { spot ->
                NativeMapMarker(
                    id = spot.id,
                    lat = spot.point.lat,
                    lng = spot.point.lng,
                    title = spot.displayName(strings),
                    snippet = "${strings.spotType(spot.type)} · ${strings.walkMinutes(spot.walkingMinutes)}",
                )
            },
        )
    }

    UIKitView(
        factory = { controller.view },
        modifier = modifier.fillMaxSize(),
    )
}

// Matches the Android GoogleMap initial zoom for visual parity.
private const val DEFAULT_ZOOM = 14.0
