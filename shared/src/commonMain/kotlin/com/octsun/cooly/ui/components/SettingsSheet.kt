package com.octsun.cooly.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octsun.cooly.domain.model.TemperatureUnit
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.ui.theme.CoolyPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    strings: Strings,
    currentUnit: TemperatureUnit,
    languageLabel: String,
    showPlus: Boolean,
    isPlus: Boolean,
    /** Hide the alerts toggle on platforms without a scheduler wired (e.g. iOS for now). */
    showAlerts: Boolean,
    alertsEnabled: Boolean,
    onAlertsChange: (Boolean) -> Unit,
    onUpgrade: () -> Unit,
    onUnitChange: (TemperatureUnit) -> Unit,
    onPrivacyPolicy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(strings.settings, fontSize = 22.sp, fontWeight = FontWeight.Bold)

            // Always-visible Plus entry — the paywall used to be reachable only by hitting
            // the saved-place limit, so most users never saw that Cooly Plus exists.
            if (showPlus) {
                if (isPlus) {
                    PlusActiveCard(strings.plusActive)
                } else {
                    PlusUpsellCard(
                        title = strings.coolyPlus,
                        subtitle = strings.plusTagline,
                        cta = strings.upgrade,
                        onClick = onUpgrade,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Danger alerts: the Peace-story feature — warn before it gets dangerous.
            if (showAlerts) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(value = alertsEnabled, role = Role.Switch, onValueChange = onAlertsChange)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(strings.dangerAlerts, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(strings.dangerAlertsDesc, fontSize = 12.sp, color = CoolyPalette.Sub)
                    }
                    Switch(checked = alertsEnabled, onCheckedChange = null)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            SectionLabel(strings.temperatureUnit)
            UnitOption(strings.celsius, currentUnit == TemperatureUnit.CELSIUS) {
                onUnitChange(TemperatureUnit.CELSIUS)
            }
            UnitOption(strings.fahrenheit, currentUnit == TemperatureUnit.FAHRENHEIT) {
                onUnitChange(TemperatureUnit.FAHRENHEIT)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(strings.language, fontSize = 15.sp)
                Text(languageLabel, fontSize = 15.sp, color = CoolyPalette.Sub)
            }
            // Not a control — say so, or users tap it and assume it's broken.
            Text(strings.languageFollowsSystem, fontSize = 12.sp, color = CoolyPalette.Sub)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Data-source attribution — OSM's licence requires crediting contributors.
            SectionLabel(strings.dataSources)
            Text(strings.dataSourceMap, fontSize = 13.sp, color = CoolyPalette.Sub)
            Text(strings.dataSourceWeather, fontSize = 13.sp, color = CoolyPalette.Sub)
            Text(
                strings.privacyAssurance,
                fontSize = 13.sp,
                color = CoolyPalette.Sub,
                modifier = Modifier.padding(top = 6.dp),
            )

            Text(
                strings.privacyPolicy,
                fontSize = 15.sp,
                color = CoolyPalette.Brand,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPrivacyPolicy)
                    .semantics { role = Role.Button; contentDescription = strings.privacyPolicy }
                    .padding(vertical = 12.dp),
            )

            Text(
                strings.versionLine(com.octsun.cooly.config.AppConfig.APP_VERSION),
                fontSize = 12.sp,
                color = CoolyPalette.Sub,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PlusUpsellCard(title: String, subtitle: String, cta: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CoolyPalette.InfoTint,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .semantics { role = Role.Button; contentDescription = "$title. $cta" },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("❄️", fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("$title", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CoolyPalette.BrandDeep)
                Text(subtitle, fontSize = 12.sp, color = CoolyPalette.Sub)
            }
            Text(cta, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CoolyPalette.Brand)
        }
    }
}

@Composable
private fun PlusActiveCard(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CoolyPalette.InfoTint,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("💙", fontSize = 24.sp)
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CoolyPalette.BrandDeep)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = CoolyPalette.Sub,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun UnitOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, fontSize = 15.sp)
    }
}
