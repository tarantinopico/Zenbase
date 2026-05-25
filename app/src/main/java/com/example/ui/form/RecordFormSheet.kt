package com.example.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.FieldDefinition
import com.example.domain.model.FieldType
import com.example.database.RecordEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Dynamický formulářový BottomSheet pro přidání nebo editaci konkrétního záznamu v kolekci.
 * Extrémně husté a kompaktní UI renderující dynamické input typy dle předložených polí.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordFormSheet(
    fields: List<FieldDefinition>,
    initialRecord: RecordEntity? = null,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any?>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Stav držící hodnoty formuláře dynamicky mapované pomocí klíče = map(name -> hodnota)
    var formState by remember { 
        mutableStateOf(mutableMapOf<String, Any?>())
    }

    LaunchedEffect(initialRecord) {
        if (initialRecord != null) {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val dataMap: Map<String, Any?> = Gson().fromJson(initialRecord.dataJson, type)
            formState = dataMap.toMutableMap()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Upravit záznam",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(fields, key = { it.id }) { field ->
                    DynamicInputField(
                        fieldDef = field,
                        value = formState[field.fieldName],
                        onValueChange = { newValue ->
                            val updated = formState.toMutableMap()
                            updated[field.fieldName] = newValue
                            formState = updated
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Zrušit")
                }
                Button(
                    onClick = { onSave(formState) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Uložit")
                }
            }
        }
    }
}

/**
 * Dynamický renderovač input pole založený na FieldType dané hodnoty.
 */
@Composable
fun DynamicInputField(
    fieldDef: FieldDefinition,
    value: Any?,
    onValueChange: (Any?) -> Unit
) {
    val commonModifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
    
    when (fieldDef.fieldType) {
        is FieldType.TextType -> {
            OutlinedTextField(
                value = value?.toString() ?: "",
                onValueChange = { onValueChange(it) },
                label = { Text(fieldDef.fieldLabel) },
                singleLine = true,
                modifier = commonModifier,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
        is FieldType.NumberType -> {
            OutlinedTextField(
                value = value?.toString() ?: "",
                onValueChange = { 
                    if (it.isEmpty() || it.toDoubleOrNull() != null) {
                       onValueChange(it.toDoubleOrNull())
                    }
                },
                label = { Text(fieldDef.fieldLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = commonModifier,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
        is FieldType.CheckboxType -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Checkbox(
                    checked = (value as? Boolean) ?: false,
                    onCheckedChange = { onValueChange(it) }
                )
                Text(text = fieldDef.fieldLabel, style = MaterialTheme.typography.bodyMedium)
            }
        }
        is FieldType.ComputedType -> {
            // Computed fields are read-only
            OutlinedTextField(
                value = value?.toString() ?: "–",
                onValueChange = {},
                label = { Text("${fieldDef.fieldLabel} (Automaticky vzorec)") },
                readOnly = true,
                singleLine = true,
                modifier = commonModifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
            )
        }
        else -> {
            OutlinedTextField(
                value = value?.toString() ?: "",
                onValueChange = { onValueChange(it) },
                label = { Text(fieldDef.fieldLabel) },
                singleLine = true,
                modifier = commonModifier
            )
        }
    }
}
