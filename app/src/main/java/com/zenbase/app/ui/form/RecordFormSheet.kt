package com.zenbase.app.ui.form

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.zenbase.app.domain.model.FieldDefinition
import com.zenbase.app.domain.model.FieldType
import com.zenbase.app.database.RecordEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.layout.size

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
        } else {
            val map = mutableMapOf<String, Any?>()
            fields.forEach { field ->
                if (field.defaultValue != null) {
                    map[field.fieldName] = field.defaultValue
                }
            }
            formState = map
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
    val context = LocalContext.current
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
        is FieldType.UrlType -> {
            var isError by remember { mutableStateOf(false) }
            val textValue = value?.toString() ?: ""
            OutlinedTextField(
                value = textValue,
                onValueChange = { 
                    isError = it.isNotEmpty() && !android.util.Patterns.WEB_URL.matcher(it).matches()
                    onValueChange(it) 
                },
                label = { Text(fieldDef.fieldLabel) },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = commonModifier,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
        is FieldType.ImageType -> {
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    onValueChange(uri.toString())
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(fieldDef.fieldLabel, style = MaterialTheme.typography.bodyMedium)
                if (value != null && value.toString().isNotEmpty()) {
                    AsyncImage(
                        model = value.toString(),
                        contentDescription = "Selected image",
                        modifier = Modifier.size(60.dp).clickable { 
                            launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                        }
                    )
                } else {
                    IconButton(onClick = { launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Filled.Image, contentDescription = "Pick image")
                    }
                }
            }
        }
        is FieldType.RatingType -> {
            val rating = (value as? Double)?.toInt() ?: 0
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(fieldDef.fieldLabel, style = MaterialTheme.typography.bodyMedium)
                Row {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Star $i",
                            tint = if (i <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp).clickable { onValueChange(i.toDouble()) }
                        )
                    }
                }
            }
        }
        is FieldType.BarcodeType -> {
            OutlinedTextField(
                value = value?.toString() ?: "",
                onValueChange = { onValueChange(it) },
                label = { Text(fieldDef.fieldLabel) },
                singleLine = true,
                modifier = commonModifier,
                trailingIcon = {
                    IconButton(onClick = { Toast.makeText(context, "Skenování čárového kódu není implementováno", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Scan barcode")
                    }
                }
            )
        }
        is FieldType.LocationType -> {
            val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
            val map = (value as? Map<*, *>)
            val lat = map?.get("lat")?.toString()?.toDoubleOrNull() ?: 0.0
            val lng = map?.get("lng")?.toString()?.toDoubleOrNull() ?: 0.0
            
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    try {
                        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                            if (loc != null) {
                                val newMap = mapOf("lat" to loc.latitude, "lng" to loc.longitude)
                                onValueChange(newMap)
                            }
                        }
                    } catch (e: SecurityException) { }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(fieldDef.fieldLabel, style = MaterialTheme.typography.bodySmall)
                    Text("Lat: $lat, Lng: $lng", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                                if (loc != null) {
                                    val newMap = mapOf("lat" to loc.latitude, "lng" to loc.longitude)
                                    onValueChange(newMap)
                                }
                            }
                        } catch (e: SecurityException) { }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) {
                    Icon(Icons.Filled.LocationOn, contentDescription = "Get location")
                }
            }
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
