package com.octsun.cooly.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octsun.cooly.domain.model.CoolSpot
import com.octsun.cooly.i18n.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailSheet(
    spot: CoolSpot,
    strings: Strings,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onDirections: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    spot.displayName(strings),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // Spring bounce when the star toggles (design polish).
                val starScale by animateFloatAsState(
                    targetValue = if (isFavorite) 1f else 0.99f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                )
                androidx.compose.material3.IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.semantics {
                        contentDescription =
                            if (isFavorite) strings.removeFromFavorites else strings.addToFavorites
                    },
                ) {
                    Text(
                        if (isFavorite) "★" else "☆",
                        fontSize = 24.sp,
                        color = Color(0xFFF9A825),
                        modifier = Modifier.graphicsLayer {
                            val s = if (isFavorite) starScale * 1.2f - 0.2f else 1f
                            scaleX = s; scaleY = s
                        },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(strings.spotType(spot.type))
                Chip(strings.distanceAwayLabel(formatDistance(spot.distanceMeters, strings)))
                Chip(strings.walkMinutes(spot.walkingMinutes))
            }
            spot.openingHours?.let {
                // Localize OSM day tokens (Mo-Fr → 월-금) + honesty note: community data
                // can be stale, and a closed door in a heat wave is a safety failure.
                DetailLine("${strings.openingHours}: ${strings.localizeOpeningHours(it)}")
                Text(
                    strings.openingHoursDisclaimer,
                    fontSize = 11.sp,
                    color = com.octsun.cooly.ui.theme.CoolyPalette.Sub,
                )
            }
            spot.address?.let {
                DetailLine(it)
            }
            Button(
                onClick = onDirections,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("🧭 ${strings.directions}", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    androidx.compose.material3.Surface(
        color = com.octsun.cooly.ui.theme.CoolyPalette.InfoTint,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = com.octsun.cooly.ui.theme.CoolyPalette.BrandDeep,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun DetailLine(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        color = com.octsun.cooly.ui.theme.CoolyPalette.Sub,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}
