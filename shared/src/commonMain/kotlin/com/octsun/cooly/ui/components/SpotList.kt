package com.octsun.cooly.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.i18n.Strings
import com.octsun.cooly.ui.theme.CoolyPalette
import com.octsun.cooly.ui.theme.glyph
import com.octsun.cooly.ui.theme.pinColor

@Composable
fun SpotList(
    spots: List<CoolSpot>,
    hasHiddenSpots: Boolean,
    strings: Strings,
    onSpotClick: (CoolSpot) -> Unit,
    onUnlockClick: () -> Unit,
    adInFlight: Boolean,
    modifier: Modifier = Modifier,
    emptyText: String = strings.noSpotsFound,
    topPadding: Dp = 0.dp,
) {
    if (spots.isEmpty()) {
        Box(modifier.fillMaxSize().padding(32.dp).padding(top = topPadding), contentAlignment = Alignment.Center) {
            Text(emptyText, color = CoolyPalette.Sub, textAlign = TextAlign.Center)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding),
    ) {
        items(spots, key = { it.id }) { spot ->
            // Filter-chip toggles animate rows in/out instead of snapping (design polish).
            Column(modifier = Modifier.animateItem()) {
                SpotRow(spot, strings, onClick = { onSpotClick(spot) })
                HorizontalDivider(color = CoolyPalette.Divider)
            }
        }
        if (hasHiddenSpots) {
            item {
                // Faint preview so the user senses there's more behind the ad gate.
                Text(
                    "• • •",
                    color = CoolyPalette.Sub,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                UnlockAllSpotsCard(
                    strings = strings,
                    onClick = onUnlockClick,
                    enabled = !adInFlight,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
fun SpotRow(
    spot: CoolSpot,
    strings: Strings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(color = spot.type.pinColor(), shape = CircleShape, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(spot.type.glyph(), fontSize = 18.sp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                spot.displayName(strings),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${strings.spotType(spot.type)} · ${strings.walkMinutes(spot.walkingMinutes)}",
                fontSize = 13.sp,
                color = CoolyPalette.Sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            formatDistance(spot.distanceMeters, strings),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = CoolyPalette.Brand,
        )
    }
}
