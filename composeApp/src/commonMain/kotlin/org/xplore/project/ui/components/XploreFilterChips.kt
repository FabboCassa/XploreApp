package org.xplore.project.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.xplore.project.ui.home.FilterChipData

/**
 * Horizontally scrollable row of filter chips.
 *
 * ## UI Behavior
 * - Shows a list of [FilterChipData].
 * - Animates color/state changes when selected.
 * - Handles click events via callback (stateless).
 */
@Composable
fun XploreFilterChips(
    filters: List<FilterChipData>,
    onFilterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        filters.forEach { chip ->
            val containerColor by animateColorAsState(
                targetValue = if (chip.selected) {
                    colorScheme.primary
                } else {
                    colorScheme.surface.copy(alpha = 0.85f)
                },
                animationSpec = tween(durationMillis = 250),
                label = "chipColor_${chip.id}",
            )
            val labelColor by animateColorAsState(
                targetValue = if (chip.selected) {
                    colorScheme.onPrimary
                } else {
                    colorScheme.onSurface
                },
                animationSpec = tween(durationMillis = 250),
                label = "chipLabelColor_${chip.id}",
            )

            FilterChip(
                selected = chip.selected,
                onClick = { onFilterClick(chip.id) },
                label = {
                    Text(
                        text = stringResource(chip.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = labelColor,
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = containerColor,
                    selectedContainerColor = containerColor,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = colorScheme.outline.copy(alpha = 0.2f),
                    selectedBorderColor = colorScheme.primary,
                    enabled = true,
                    selected = chip.selected,
                ),
            )
        }
    }
}
