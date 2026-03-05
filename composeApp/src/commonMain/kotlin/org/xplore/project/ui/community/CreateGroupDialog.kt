package org.xplore.project.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.community_access_invite
import xploreapp.composeapp.generated.resources.community_access_label
import xploreapp.composeapp.generated.resources.community_access_password
import xploreapp.composeapp.generated.resources.community_access_public
import xploreapp.composeapp.generated.resources.community_btn_cancel
import xploreapp.composeapp.generated.resources.community_btn_create
import xploreapp.composeapp.generated.resources.community_create_group
import xploreapp.composeapp.generated.resources.community_group_description
import xploreapp.composeapp.generated.resources.community_group_name
import xploreapp.composeapp.generated.resources.community_password_label

/**
 * Dialog for creating a new community group.
 * The access-type selector uses [ExposedDropdownMenuBox] so the dropdown
 * renders as a floating Popup — the dialog size never changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

                // ── Access Type Dropdown (floating popup, dialog size unaffected) ──
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.community_access_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        accessOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                onClick = {
                                    accessType = value
                                    dropdownExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

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
