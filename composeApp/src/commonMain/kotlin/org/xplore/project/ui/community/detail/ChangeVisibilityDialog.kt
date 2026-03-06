package org.xplore.project.ui.community.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import xploreapp.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeVisibilityDialog(
    initialAccessType: Int,
    onDismiss: () -> Unit,
    onSave: (accessType: Int, password: String?) -> Unit,
) {
    var accessType by remember { mutableIntStateOf(initialAccessType) }
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
        title = { Text(stringResource(Res.string.group_detail_change_visibility), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.group_detail_access_type)) },
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
                                text = { Text(label) },
                                onClick = {
                                    accessType = value
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (accessType == 1) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(Res.string.group_detail_new_password)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(accessType, if (accessType == 1) password.ifBlank { null } else null) },
                enabled = accessType != 1 || password.isNotBlank(),
            ) {
                Text(stringResource(Res.string.group_detail_btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.group_detail_btn_cancel))
            }
        },
    )
}
