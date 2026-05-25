package com.zenbase.app.ui.collectiondetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.zenbase.app.domain.model.FieldDefinition
import com.zenbase.app.database.RecordEntity
import com.zenbase.app.ui.form.RecordFormSheet
import com.zenbase.app.ui.schema.AddFieldDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

/**
 * Zobrazení detailu kolekce implementující vysoce kompaktní grid pro seznam záznamů,
 * a plná interaktivity pro přidání dalších položek či manipulaci se schématy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: CollectionDetailViewModel = viewModel(factory = CollectionDetailViewModel.provideFactory(collectionId))
) {
    val currentCollection by viewModel.collection.collectAsStateWithLifecycle()
    val fields by viewModel.fields.collectAsStateWithLifecycle()
    val recordsState by viewModel.recordsUiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    var showFieldDialog by remember { mutableStateOf(false) }
    var selectedRecordForEdit by remember { mutableStateOf<RecordEntity?>(null) }
    var showRecordSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.importFromCsv(uri)
            Toast.makeText(context, "Import spuštěn", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentCollection?.name ?: "Načítání...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportToCsv() }) {
                        Icon(Icons.Filled.FileUpload, contentDescription = "Exportovat CSV")
                    }
                    IconButton(onClick = { importCsvLauncher.launch("text/csv") }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Importovat CSV")
                    }
                    IconButton(onClick = { showFieldDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Nastavení polí (Schéma)")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    selectedRecordForEdit = null
                    showRecordSheet = true 
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Přidat záznam")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Hledat...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true
                )
                
                var sortExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = sortOrder.option.name,
                        onValueChange = {},
                        label = { Text("Řazení") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false },
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    viewModel.updateSortOrder(SortState(option = option))
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (recordsState.records.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Žádné záznamy. Přidejte první tlačítkem +",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(recordsState.records, key = { it.id }) { record ->
                        RecordGridCard(
                            record = record,
                            fields = fields,
                            onClick = {
                                selectedRecordForEdit = record
                                showRecordSheet = true
                            }
                        )
                    }
                }
            }
        }

        if (showFieldDialog) {
            AddFieldDialog(
                onDismiss = { showFieldDialog = false },
                onAddField = { name, label, type ->
                    viewModel.addFieldMetadata(name, label, type)
                    showFieldDialog = false
                }
            )
        }

        if (showRecordSheet) {
            RecordFormSheet(
                fields = fields,
                initialRecord = selectedRecordForEdit,
                onDismiss = { showRecordSheet = false },
                onSave = { dataMap ->
                    if (selectedRecordForEdit == null) {
                        viewModel.appendNewRecord(dataMap)
                    } else {
                        viewModel.updateExistingRecord(selectedRecordForEdit!!.id, dataMap)
                    }
                    showRecordSheet = false
                }
            )
        }
    }
}

@Composable
fun RecordGridCard(
    record: RecordEntity,
    fields: List<FieldDefinition>,
    onClick: () -> Unit
) {
    val type = object : TypeToken<Map<String, Any?>>() {}.type
    val dataMap: Map<String, Any?> = Gson().fromJson(record.dataJson, type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // Dynamicky stavěný grid pro zobrazení polí
        Column(modifier = Modifier.padding(6.dp)) {
            val columns = 2
            val rows = (fields.size + columns - 1) / columns
            
            for (i in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (j in 0 until columns) {
                        val index = i * columns + j
                        if (index < fields.size) {
                            val field = fields[index]
                            val value = dataMap[field.fieldName]?.toString() ?: "–"
                            GridCell(label = field.fieldLabel, value = value)
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.GridCell(label: String, value: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
