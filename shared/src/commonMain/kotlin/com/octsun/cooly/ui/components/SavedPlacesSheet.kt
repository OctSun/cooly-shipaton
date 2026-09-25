package com.octsun.cooly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octsun.cooly.domain.model.EnvStatus
import com.octsun.cooly.domain.model.SavedPlace
import com.octsun.cooly.domain.model.TemperatureUnit
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.ui.theme.CoolyPalette
import com.octsun.cooly.ui.theme.color

/**
 * Watched places: search a city, save it, and see its heat/air at a glance.
 * The first Cooly Plus surface (free users get one place, spec: travellers/family).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesSheet(
    strings: Strings,
    temperatureUnit: TemperatureUnit,
    savedPlaces: List<SavedPlace>,
    placeEnv: Map<String, EnvStatus>,
    placeWeeklyMax: Map<String, Double> = emptyMap(),
    searchQuery: String,
    searchResults: List<SavedPlace>,
    searchLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onAdd: (SavedPlace) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                // Keep search results visible above the soft keyboard.
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(strings.savedPlaces, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(strings.savedPlacesDesc, fontSize = 13.sp, color = CoolyPalette.Sub)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text(strings.searchPlaceHint) },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Text("✕", fontSize = 16.sp, color = CoolyPalette.Sub)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (searchLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            // Search results appear above the saved list while a query is active.
            if (searchQuery.isNotBlank() && !searchLoading) {
                if (searchResults.isEmpty()) {
                    Text(strings.searchNoResults, fontSize = 13.sp, color = CoolyPalette.Sub)
                } else {
                    searchResults.take(5).forEach { result ->
                        SearchResultRow(
                            result = result,
                            alreadySaved = savedPlaces.any { it.id == result.id },
                            addLabel = strings.addPlace,
                            onAdd = { onAdd(result) },
                        )
                    }
                    HorizontalDivider(color = CoolyPalette.Divider)
                }
            }

            if (savedPlaces.isEmpty() && searchQuery.isBlank()) {
                Text(
                    strings.noSavedPlaces,
                    fontSize = 14.sp,
                    color = CoolyPalette.Sub,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(savedPlaces, key = { it.id }) { place ->
                        Column(modifier = Modifier.animateItem()) {
                        SavedPlaceRow(
                            place = place,
                            env = placeEnv[place.id],
                            weeklyMax = placeWeeklyMax[place.id],
                            strings = strings,
                            temperatureUnit = temperatureUnit,
                            onRemove = { onRemove(place.id) },
                        )
                        HorizontalDivider(color = CoolyPalette.Divider)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: SavedPlace,
    alreadySaved: Boolean,
    addLabel: String,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(result.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            result.region?.let {
                Text(it, fontSize = 12.sp, color = CoolyPalette.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        TextButton(onClick = onAdd, enabled = !alreadySaved) {
            Text(if (alreadySaved) "✓" else addLabel)
        }
    }
}

@Composable
private fun SavedPlaceRow(
    place: SavedPlace,
    env: EnvStatus?,
    weeklyMax: Double?,
    strings: Strings,
    temperatureUnit: TemperatureUnit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Risk dot: gray until this place's env loads.
        Surface(
            color = if (env?.hasTemp == true) env.riskLevel.color() else CoolyPalette.Sub,
            shape = CircleShape,
            modifier = Modifier.size(12.dp),
        ) {}
        Column(modifier = Modifier.weight(1f)) {
            Text(place.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, color = CoolyPalette.Ink)
            place.region?.let {
                Text(it, fontSize = 12.sp, color = CoolyPalette.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // 7-day outlook: the trip-planning line reviewers asked for.
            weeklyMax?.let {
                Text(
                    strings.weeklyMaxLine(formatTemperature(it, temperatureUnit)),
                    fontSize = 12.sp,
                    color = CoolyPalette.BrandDeep,
                    maxLines = 1,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (env?.hasTemp == true) formatTemperature(env.feelsLikeTemp, temperatureUnit) else "—",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CoolyPalette.Ink,
            )
            Text(
                if (env?.hasAqi == true) "${strings.aqi} ${env.aqi} · ${strings.aqiLevel(env.aqiLevel)}" else "—",
                fontSize = 12.sp,
                color = if (env?.hasAqi == true) env.aqiLevel.color() else CoolyPalette.Sub,
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.semantics { contentDescription = strings.removePlace },
        ) {
            Text("✕", fontSize = 16.sp, color = CoolyPalette.Sub)
        }
    }
}
