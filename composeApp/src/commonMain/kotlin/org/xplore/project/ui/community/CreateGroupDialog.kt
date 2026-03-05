package org.xplore.project.ui.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.community_create_group
import xploreapp.composeapp.generated.resources.community_group_name
import xploreapp.composeapp.generated.resources.community_group_description
import xploreapp.composeapp.generated.resources.community_btn_create
import xploreapp.composeapp.generated.resources.community_btn_cancel
import xploreapp.composeapp.generated.resources.community_access_public
import xploreapp.composeapp.generated.resources.community_access_password
import xploreapp.composeapp.generated.resources.community_access_invite
import xploreapp.composeapp.generated.resources.community_password_label
import xploreapp.composeapp.generated.resources.community_access_label

/**
 * Dialog for creating a new community group.
 * Access type uses a dropdown rectangle with expand/collapse indicator.
 */
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String?, accessType: Int, password: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accessType by remember { mutableIntStateOf(0) }
    var password by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val accessOptions = listOf(
        0 to stringResource(Res.string.community_access_public),
        1 to stringResource(Res.string.community_access_password),
        2 to stringResource(Res.string.community_access_invite),
    )

    val selectedLabel = accessOptions.first { it.first == accessType }.second

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.community_create_group),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.community_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.community_group_description)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Access Type Dropdown ──
                AccessTypeDropdown(
                    selectedLabel = selectedLabel,
                    expanded = dropdownExpanded,
                    options = accessOptions,
                    onToggle = { dropdownExpanded = !dropdownExpanded },
                    onSelect = { value ->
                        accessType = value
                        dropdownExpanded = false
                    },
                )

                // Password field (only for Password type)
                if (accessType == 1) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(Res.string.community_password_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        name,
                        description.ifBlank { null },
                        accessType,
                        if (accessType == 1) password.ifBlank { null } else null,
                    )
                },
                enabled = name.isNotBlank() && (accessType != 1 || password.isNotBlank()),
            ) {
                Text(stringResource(Res.string.community_btn_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.community_btn_cancel))
            }
        },
    )
}

/**
 * Custom dropdown selector: a rectangle showing the selected value
 * with a triangle toggle. Expands to show the option list below.
 */
@Composable
private fun AccessTypeDropdown(
    selectedLabel: String,
    expanded: Boolean,
    options: List<Pair<Int, String>>,
    onToggle: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    Column {
        // Selected value rectangle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.community_access_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Icon(
                    imageVector = if (expanded)
                        Icons.Filled.KeyboardArrowUp
                    else
                        Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Expandable option list
        AnimatedVisibility(visible = expanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column {
                    options.forEach { (value, label) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(value) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
