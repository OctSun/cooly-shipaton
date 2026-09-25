package com.octsun.cooly.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.octsun.cooly.domain.model.GeoPoint

/** Current-location access with runtime permission handling. */
interface LocationService {
    /** True if the OS location permission is currently granted. */
    val hasPermission: Boolean

    /** Request the location permission from the user; returns the granted result. */
    suspend fun ensurePermission(): Boolean

    /** Best available current location, or null if unavailable. */
    suspend fun currentLocation(): GeoPoint?
}

/**
 * Hands off turn-by-turn to an external maps app instead of building navigation
 * in-app: google.navigation / geo on Android, comgooglemaps / maps on iOS.
 */
interface DirectionsLauncher {
    fun openWalkingDirections(destination: GeoPoint, label: String)
}

/** Opens an external web URL (e.g. the privacy policy, spec 4-4) and the OS app-settings page. */
interface LinkOpener {
    fun openUrl(url: String)

    /** Opens this app's system settings so the user can re-enable a denied permission. */
    fun openAppSettings()
}

/** Tiny key-value store for preferences + favorites (SharedPreferences / NSUserDefaults). */
interface PreferencesStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

/** AdMob bridge. Banner is a composable; rewarded is a suspend call. */
interface AdsController {
    /** Preload a rewarded ad so it's ready when the user taps a trigger. */
    fun preloadRewarded()

    /** True once a rewarded ad is loaded and ready to show. */
    val isRewardedReady: Boolean

    /**
     * Show a rewarded ad. Returns true only if the user watched to completion
     * and earned the reward; false on dismissal/failure.
     */
    suspend fun showRewardedAd(): Boolean
}

/** Pokes any home-screen widgets to redraw after a fresh reading is persisted. */
interface WidgetUpdater {
    fun refresh()
}

/**
 * Schedules/cancels the periodic background danger-alert check (heat/air).
 * Enabling may trigger an OS notification-permission prompt on the platform side.
 */
interface AlertsController {
    fun setEnabled(enabled: Boolean)
}

/** Everything the shared UI needs from the host platform. */
class AppServices(
    val location: LocationService,
    val directions: DirectionsLauncher,
    val ads: AdsController,
    val links: LinkOpener,
    val preferences: PreferencesStore,
    val languageCode: String = currentLanguageCode(),
    /** Null on platforms without home-screen widgets (e.g. iOS for now). */
    val widgets: WidgetUpdater? = null,
    /** Null on platforms without background danger alerts wired yet. */
    val alerts: AlertsController? = null,
)

/** 2-letter language code of the device (e.g. "ko", "en"). */
expect fun currentLanguageCode(): String

/** Platform AdMob banner. No-op placeholder box on platforms without the SDK wired. */
@Composable
expect fun BannerAd(modifier: Modifier)
