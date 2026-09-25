package com.octsun.cooly.platform

import android.app.Activity

/**
 * No-op ads controller. The AdMob SDK is intentionally not bundled in this build (no ads,
 * clean "No ads" Play declaration). Re-add play-services-ads and restore the AdMob
 * implementation to monetize later (see git history / AppConfig.ADS_ENABLED).
 */
@Suppress("UNUSED_PARAMETER")
class AndroidAdsController(activity: Activity) : AdsController {
    override val isRewardedReady: Boolean = false
    override fun preloadRewarded() {}
    override suspend fun showRewardedAd(): Boolean = false
}
