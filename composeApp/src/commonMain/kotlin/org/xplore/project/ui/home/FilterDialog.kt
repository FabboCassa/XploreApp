package org.xplore.project.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.filter_dialog_btn_apply
import xploreapp.composeapp.generated.resources.filter_dialog_title
import xploreapp.composeapp.generated.resources.ic_close

private val ACCENT_BLUE = Color(0xFF4A90D9)

/**
 * Dialog that displays all available filters in a flowing grid.
 *
 * The user can toggle multiple filters on/off. Changes are only applied
 * when the "Applica filtri" button is pressed.
 *
 * Layout mirrors [SettingsDialog] for visual consistency.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    filters: List<FilterChipData>,
    onApply: (List<FilterChipData>) -> Unit,
    onDismiss: () -> Unit,
) {
    // Local copy so changes aren't applied until the user confirms
    var localFilters by remember { mutableStateOf(filters) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.65f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                // ── Header ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.filter_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.filter_dialog_title),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Scrollable filter grid ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        localFilters.forEachIndexed { index, chip ->
                            val containerColor by animateColorAsState(
                                targetValue = if (chip.selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = tween(durationMillis = 200),
                                label = "filterDialogChip_${chip.id}",
                            )
                            val labelColor by animateColorAsState(
                                targetValue = if (chip.selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                animationSpec = tween(durationMillis = 200),
                                label = "filterDialogLabel_${chip.id}",
                            )

                            FilterChip(
                                selected = chip.selected,
                                onClick = {
                                    localFilters = localFilters.toMutableList().apply {
                                        this[index] = chip.copy(selected = !chip.selected)
                                    }
                                },
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
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    enabled = true,
                                    selected = chip.selected,
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Apply button (fixed at bottom) ──
                Button(
                    onClick = { onApply(localFilters) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ACCENT_BLUE,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = stringResource(Res.string.filter_dialog_btn_apply),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
