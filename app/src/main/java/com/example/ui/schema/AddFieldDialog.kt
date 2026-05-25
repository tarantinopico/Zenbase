package com.example.ui.schema

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.FieldType

/**
 * Kompaktní dialog představující formulář pro tvorbu nového pole schéma (definizace struktury záznamů).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFieldDialog(
    onDismiss: () -> Unit,
    onAddField: (name: String, label: String, type: FieldType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<FieldType>(FieldType.TextType(null)) }

    val fieldTypes = listOf(
        FieldType.TextType(null), FieldType.NumberType(null), FieldType.DateType, FieldType.CheckboxType
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Přidat pole", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ID pole (např. 'vek')") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Zobrazovaný popisek (např. 'Věk dodavatele')") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = selectedType::class.simpleName ?: "",
                        onValueChange = {},
                        label = { Text("Datový typ") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        fieldTypes.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption::class.simpleName ?: "") },
                                onClick = {
                                    selectedType = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (name.isNotBlank() && label.isNotBlank()) {
                        onAddField(name, label, selectedType)
                    }
                }
            ) {
                Text("Vytvořit", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
