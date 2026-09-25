package com.octsun.cooly.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.domain.model.GeoPoint
import com.octsun.cooly.i18n.Strings

/**
 * The map surface. Android renders a real Google Map; iOS falls back to the
 * self-drawn [CanvasMap] until a native map is wired.
 */
@Composable
expect fun SpotMap(
    userLocation: GeoPoint?,
    spots: List<CoolSpot>,
    strings: Strings,
    onSpotClick: (CoolSpot) -> Unit,
    modifier: Modifier,
)
