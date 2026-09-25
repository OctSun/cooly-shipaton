package com.octsun.cooly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octsun.cooly.config.AppConfig
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.ui.theme.CoolyPalette

/**
 * Cooly Plus upsell. Deliberately NOT a paywall over safety features — the copy leads with
 * "every safety feature stays free" (Peace narrative). Replaced by the RevenueCat paywall
 * template once the SDK is wired; the [onPurchase] seam stays the same.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlusSheet(
    strings: Strings,
    priceLabel: String?,
    purchaseInFlight: Boolean,
    error: String?,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("❄️ ${strings.coolyPlus}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(strings.plusTagline, fontSize = 14.sp, color = CoolyPalette.Sub)

            Column(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlusFeature("🌍", strings.plusFeatureUnlimitedPlaces)
                PlusFeature("💙", strings.plusFeatureSupport)
            }

            Text(
                strings.placeLimitReached(AppConfig.FREE_PLACE_LIMIT),
                fontSize = 13.sp,
                color = CoolyPalette.Sub,
            )

            // Apple 3.1.2: the price must be visible before purchase.
            if (priceLabel != null) {
                Text(
                    strings.priceLine(priceLabel),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CoolyPalette.Ink,
                )
            }

            // Inline result feedback — a snackbar would render behind this modal sheet.
            if (error != null) {
                Text(error, fontSize = 13.sp, color = CoolyPalette.ErrorInk)
            }

            Button(
                onClick = onPurchase,
                enabled = !purchaseInFlight,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp),
            ) {
                if (purchaseInFlight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CoolyPalette.Sub,
                    )
                } else {
                    Text(
                        if (priceLabel != null) "${strings.upgrade} — $priceLabel" else strings.upgrade,
                        fontSize = 16.sp,
                    )
                }
            }
            // App Store guideline 3.1.2: a restore affordance must be reachable from the paywall.
            TextButton(onClick = onRestore, enabled = !purchaseInFlight, modifier = Modifier.fillMaxWidth()) {
                Text(strings.restorePurchases)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onPrivacyPolicy) {
                    Text(strings.privacyPolicy, fontSize = 13.sp, color = CoolyPalette.Sub)
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.notNow)
                }
            }
        }
    }
}

@Composable
private fun PlusFeature(glyph: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(glyph, fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
        Text(text, fontSize = 15.sp, color = CoolyPalette.Ink)
    }
}
