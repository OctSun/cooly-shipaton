package com.octsun.cooly.platform

import platform.UIKit.UIView

/** One marker as consumed by the native map implementation (Swift side). */
data class NativeMapMarker(
    val id: String,
    val lat: Double,
    val lng: Double,
    val title: String,
    val snippet: String,
)

/**
 * Controller for a single native map view instance. Implemented in Swift
 * (GoogleMapProvider.swift) so the Google Maps SDK never crosses into Kotlin —
 * the shared framework builds with or without the SDK present.
 */
interface NativeMapController {
    val view: UIView
    fun setCamera(lat: Double, lng: Double, zoom: Double, animated: Boolean)
    fun setMarkers(markers: List<NativeMapMarker>)
    fun setMyLocationEnabled(enabled: Boolean)

    /** Apply a Google map style JSON (night mode), or null to reset to the default style. */
    fun setMapStyleJson(json: String?)

    fun dispose()
}

/** Creates [NativeMapController]s; one per on-screen map. */
interface NativeMapFactory {
    fun create(onMarkerTap: (String) -> Unit): NativeMapController
}

/**
 * Registration point: the Swift host sets [factory] at startup, before the Compose UI
 * is created. When null (SDK not bundled, or no API key), SpotMap falls back to CanvasMap.
 */
object NativeMapBridge {
    var factory: NativeMapFactory? = null
}
