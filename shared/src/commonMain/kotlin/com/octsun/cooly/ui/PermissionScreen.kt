package com.octsun.cooly.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.ui.theme.CoolyPalette

@Composable
fun PermissionScreen(
    strings: Strings,
    denied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Scrollable so the grant button stays reachable at large font scale / small screens;
    // heightIn(min = viewport) keeps the content vertically centered when it fits.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .safeContentPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("❄️", fontSize = 56.sp)
            Text(
                strings.appName,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                strings.sloganLine,
                fontSize = 16.sp,
                color = CoolyPalette.Sub,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            // Concrete value preview before the permission ask.
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ValueBullet("🌡️", strings.valueProp1)
                ValueBullet("📍", strings.valueProp2)
                ValueBullet("🧭", strings.valueProp3)
            }

            Text(
                strings.locationPermissionTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (denied) strings.permissionDeniedHelp else strings.locationPermissionRationale,
                fontSize = 14.sp,
                color = CoolyPalette.Sub,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            // The privacy promise belongs right where the user decides.
            Text(
                strings.privacyAssurance,
                fontSize = 12.sp,
                color = CoolyPalette.Sub,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Button(onClick = onRequest) {
                Text(strings.grantPermission)
            }
            // After a denial the OS may not prompt again — offer a Settings deep-link.
            if (denied) {
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 12.dp)) {
                    Text(strings.openSettings)
                }
            }
        }
    }
}

@Composable
private fun ValueBullet(glyph: String, text: String) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Text(glyph, fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
        Text(text, fontSize = 15.sp, color = CoolyPalette.Ink)
    }
}
