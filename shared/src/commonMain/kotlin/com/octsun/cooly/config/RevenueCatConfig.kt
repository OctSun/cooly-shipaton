package com.octsun.cooly.config

/**
 * RevenueCat project keys. Public SDK keys (safe to ship in the binary, per RevenueCat docs).
 * A blank key makes that platform fall back to [com.octsun.cooly.data.StubEntitlements]
 * (no Plus gating, purchase CTA shows "coming soon"). The Android key is LIVE — the real
 * SDK initializes and the Plus paywall is reachable; the RevenueCat dashboard + Play
 * billing products must be configured before release.
 */
object RevenueCatConfig {
    /** RevenueCat public API key for the Play Store app (starts with "goog_"). */
    const val ANDROID_API_KEY = "goog_AWqYmPAzhGGhRjetmqVAXwtmAVB"

    /** RevenueCat public API key for the App Store app (starts with "appl_"). */
    const val IOS_API_KEY = "appl_YvZdyCvfRtgDlObrBXHpSImIiqc"

    /** Entitlement identifier configured in the RevenueCat dashboard. */
    const val ENTITLEMENT_PLUS = "plus"
}
