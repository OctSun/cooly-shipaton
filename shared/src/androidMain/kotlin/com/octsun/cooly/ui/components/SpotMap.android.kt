package com.octsun.cooly.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.domain.model.GeoPoint
import com.octsun.cooly.domain.model.SpotType
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.ui.theme.LocalCoolyColors
import com.octsun.cooly.ui.theme.glyph
import com.octsun.cooly.ui.theme.pinColor
import com.octsun.cooly.ui.theme.NIGHT_MAP_STYLE_JSON

/** Adapts a [CoolSpot] to the clustering library's item contract. */
private class SpotClusterItem(val spot: CoolSpot, private val title: String) : ClusterItem {
    override fun getPosition() = LatLng(spot.point.lat, spot.point.lng)
    override fun getTitle() = title
    override fun getSnippet(): String? = null
    override fun getZIndex(): Float = 0f
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
actual fun SpotMap(
    userLocation: GeoPoint?,
    spots: List<CoolSpot>,
    strings: Strings,
    onSpotClick: (CoolSpot) -> Unit,
    modifier: Modifier,
) {
    val colors = LocalCoolyColors.current
    if (userLocation == null) {
        // Neutral background only — the app shell shows loading/error, so avoid a
        // "couldn't get location" message fighting the loading spinner (verified on device).
        Box(modifier.fillMaxSize().background(colors.mapLand))
        return
    }

    val context = LocalContext.current
    val hasLocationPermission = remember {
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(userLocation.lat, userLocation.lng),
            14f,
        )
    }
    // Re-center when the user's location changes (pan only — preserve the user's zoom).
    LaunchedEffect(userLocation.lat, userLocation.lng) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLng(LatLng(userLocation.lat, userLocation.lng)),
        )
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = hasLocationPermission,
            // Same night style JSON as iOS (GMSMapStyle) for cross-platform parity.
            mapStyleOptions = if (colors.isDark) MapStyleOptions(NIGHT_MAP_STYLE_JSON) else null,
        ),
        uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission, zoomControlsEnabled = false),
    ) {
        // Cluster dense POIs so a downtown's 200 pins collapse into readable count bubbles
        // instead of an unreadable pile (Design). Tapping a cluster zooms in; tapping a
        // pin opens its detail sheet.
        val items = remember(spots) {
            spots.map { SpotClusterItem(it, it.displayName(strings)) }
        }
        Clustering(
            items = items,
            onClusterItemClick = { item ->
                onSpotClick(item.spot)
                true
            },
            clusterContent = { cluster -> ClusterBubble(cluster.size) },
            clusterItemContent = { item -> SpotPin(item.spot.type) },
        )
    }
}

/** The shared pin identity as a composable: white ring + type color + emoji glyph. */
@Composable
private fun SpotPin(type: SpotType) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.White, CircleShape)
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(type.pinColor(), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(type.glyph(), fontSize = 16.sp)
        }
    }
}

/** A cluster is drawn as a branded count bubble that scales gently with size. */
@Composable
private fun ClusterBubble(count: Int) {
    val diameter = when {
        count >= 50 -> 56.dp
        count >= 10 -> 50.dp
        else -> 44.dp
    }
    Box(
        modifier = Modifier
            .size(diameter)
            .background(Color(0xFF0277BD), CircleShape)
            .border(2.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else "$count",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
