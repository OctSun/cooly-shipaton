package com.octsun.cooly.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * No-op banner. The AdMob SDK is not bundled in this build, and CoolyApp only calls this when
 * AppConfig.ADS_ENABLED is true — kept as an empty composable so the expect/actual stays satisfied.
 */
@Composable
actual fun BannerAd(modifier: Modifier) {
    Box(modifier)
}
