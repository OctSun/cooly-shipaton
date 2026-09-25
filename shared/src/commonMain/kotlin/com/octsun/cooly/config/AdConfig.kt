package com.octsun.cooly.config

/**
 * AdMob unit ids. These are Google's official TEST ids — swap for real ids before release.
 * Keep test ids in dev builds to avoid policy strikes.
 */
object AdConfig {
    // Android test ids
    const val ANDROID_BANNER = "ca-app-pub-3940256099942544/9214589741"
    const val ANDROID_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val ANDROID_APP_ID = "ca-app-pub-3940256099942544~3347511713"

    // iOS test ids
    const val IOS_BANNER = "ca-app-pub-3940256099942544/2934735716"
    const val IOS_REWARDED = "ca-app-pub-3940256099942544/1712485313"
}

/** Feature/config constants for the free vs. rewarded gating. */
object AppConfig {
    /**
     * Master switch for ads. When false: no banner, no rewarded gating (all spots free),
     * AdMob SDK is not initialized, and the manifest AdMob entries are removed.
     * Set true (and restore the manifest entries) to re-enable monetization.
     */
    const val ADS_ENABLED = false

    /** How many nearest spots are visible before the rewarded unlock. */
    const val FREE_SPOT_LIMIT = 4

    /** App version shown in Settings (keep in sync with androidApp versionName). */
    const val APP_VERSION = "1.2.0"

    /**
     * Search radius for POIs around the user, in meters. Kept to a genuinely walkable
     * range for a heat-stressed user; also keeps the Overpass result cap meaningful —
     * a huge radius + element cap can silently drop the NEAREST spots.
     */
    const val POI_SEARCH_RADIUS_M = 1200

    /** How long a cached POI result stays fresh, in milliseconds. */
    const val POI_CACHE_TTL_MS = 30 * 60 * 1000L // 30 min

    /** Privacy policy URL (store requirement, spec 4-4). */
    const val PRIVACY_POLICY_URL = "https://hellokotlin.tistory.com/15"

    /** Saved places a free user can watch; Cooly Plus removes the cap. */
    const val FREE_PLACE_LIMIT = 1
}
