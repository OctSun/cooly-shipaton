package com.octsun.cooly.platform

/**
 * iOS ads stub. The Google Mobile Ads iOS SDK needs CocoaPods; until integrated, rewarded
 * ads are unavailable so the unlock simply doesn't trigger. Banner is a placeholder.
 */
class IosAdsController : AdsController {
    override val isRewardedReady: Boolean = false
    override fun preloadRewarded() {}
    override suspend fun showRewardedAd(): Boolean = false
}
