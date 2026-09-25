package com.octsun.cooly.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * iOS banner placeholder. Wiring the Google Mobile Ads iOS SDK needs CocoaPods integration
 *; until then this shows a labeled slot so layout matches Android.
 */
@Composable
actual fun BannerAd(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(60.dp).background(Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center,
    ) {
        Text("Ad", color = Color(0xFF7A7A7A), fontSize = 12.sp)
    }
}
