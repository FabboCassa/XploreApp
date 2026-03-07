package org.xplore.project.ui.community.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.xplore.project.data.remote.dto.CompetitionRuleRequest
import xploreapp.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCompetitionDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, type: Int, startDate: String?, endDate: String?, rules: List<CompetitionRuleRequest>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(0) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var pointsPerPlace by remember { mutableStateOf("1") }

    val typeOptions = listOf(
        stringResource(Res.string.competition_type_most_places),
        stringResource(Res.string.competition_type_fastest),
        stringResource(Res.string.competition_type_scavenger)
    )

    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.competition_create_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.competition_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = typeOptions[type],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.competition_type_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        typeOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    type = index
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text(stringResource(Res.string.competition_start_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text(stringResource(Res.string.competition_end_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pointsPerPlace,
                    onValueChange = { pointsPerPlace = it },
                    label = { Text(stringResource(Res.string.competition_points_per_place)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pts = pointsPerPlace.toIntOrNull() ?: 1
                    val rules = listOf(
                        CompetitionRuleRequest(
                            actionType = 0, // VisitPlace
                            pointsAwarded = pts
                        )
                    )
                    onSave(
                        name,
                        type,
                        startDate.ifBlank { null },
                        endDate.ifBlank { null },
                        rules
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(Res.string.group_detail_btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.group_detail_btn_cancel))
            }
        }
    )
}
