package com.octsun.cooly.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.octsun.cooly.domain.model.SpotLayer
import com.octsun.cooly.i18n.Strings

private val MvpLayers = listOf(
    SpotLayer.COOL_INDOOR,
    SpotLayer.WATER,
    SpotLayer.SHADE,
)

private fun SpotLayer.glyph(): String = when (this) {
    SpotLayer.COOL_INDOOR -> "❄️"
    SpotLayer.WATER -> "🚰"
    SpotLayer.SHADE -> "🌳"
    SpotLayer.TOILET -> "🚻"
}

@Composable
fun LayerFilterRow(
    activeLayers: Set<SpotLayer>,
    onToggle: (SpotLayer) -> Unit,
    strings: Strings,
    showFavoritesOnly: Boolean,
    onToggleFavorites: () -> Unit,
    toiletUnlocked: Boolean,
    isDanger: Boolean = false,
    onFindToilets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // In DANGER the toilet layer is force-shown by the ViewModel — showing a lock at the
    // same time would contradict the map.
    val toiletsAvailable = toiletUnlocked || isDanger
    // Chips float over live map tiles: the default translucent container blends into the
    // land color and unselected chips become hard to spot — force opaque surfaces.
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surface,
    )
    val chipElevation = FilterChipDefaults.filterChipElevation(elevation = 2.dp)
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Emojis are decorative: screen readers announce only the localized label.
        FilterChip(
            selected = showFavoritesOnly,
            onClick = onToggleFavorites,
            label = { Text("★ ${strings.favorites}") },
            colors = chipColors,
            elevation = chipElevation,
            modifier = Modifier.semantics { contentDescription = strings.favorites },
        )
        MvpLayers.forEach { layer ->
            FilterChip(
                selected = layer in activeLayers,
                onClick = { onToggle(layer) },
                label = { Text("${layer.glyph()} ${strings.layer(layer)}") },
                colors = chipColors,
                elevation = chipElevation,
                modifier = Modifier.semantics { contentDescription = strings.layer(layer) },
            )
        }
        if (toiletsAvailable) {
            FilterChip(
                selected = SpotLayer.TOILET in activeLayers,
                onClick = { onToggle(SpotLayer.TOILET) },
                label = { Text("${SpotLayer.TOILET.glyph()} ${strings.layer(SpotLayer.TOILET)}") },
                colors = chipColors,
                elevation = chipElevation,
                modifier = Modifier.semantics { contentDescription = strings.layer(SpotLayer.TOILET) },
            )
        } else {
            // Reward-gated: a distinct AssistChip (not a filter toggle) so the grammar is clear.
            AssistChip(
                onClick = onFindToilets,
                label = { Text(strings.findToilets) },
                leadingIcon = { Text("🔒") },
                colors = AssistChipDefaults.assistChipColors(),
                modifier = Modifier.semantics { contentDescription = strings.findToilets },
            )
        }
    }
}
