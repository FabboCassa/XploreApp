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
        0 to "Public", // Defaults if string translation missing
        1 to "Password",
        2 to "Invite Only",
    )

    val selectedLabel = accessOptions.first { it.first == accessType }.second

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Visibility", style = MaterialTheme.typography.titleLarge) },
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
                        label = { Text("Access Type") },
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
                        label = { Text("New Password") },
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
