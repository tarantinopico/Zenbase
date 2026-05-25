package com.zenbase.app.ui.collectiondetail

import com.zenbase.app.domain.usecase.ExportCsvUseCase
import com.zenbase.app.domain.usecase.ImportCsvUseCase
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenbase.app.database.RecordEntity
import com.zenbase.app.domain.model.Collection
import com.zenbase.app.domain.model.FieldDefinition
import com.zenbase.app.domain.repository.CollectionRepository
import com.zenbase.app.domain.usecase.AddRecordUseCase
import com.zenbase.app.domain.usecase.DeleteRecordUseCase
import com.zenbase.app.domain.usecase.GetRecordsForCollectionUseCase
import com.zenbase.app.domain.usecase.AddFieldUseCase
import com.zenbase.app.domain.usecase.UpdateRecordUseCase
import com.zenbase.app.domain.model.FieldType
import com.google.gson.Gson
import com.zenbase.app.domain.repository.FieldDefinitionRepository
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenbase.app.ZenbaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.combine

/**
 * Možnosti řazení záznamů.
 */
enum class SortOption {
    CREATED_DESC, CREATED_ASC, FIELD_ASC, FIELD_DESC
}

data class SortState(
    val option: SortOption = SortOption.CREATED_DESC,
    val fieldName: String? = null
)

/**
 * Stav reprezentující načtená data záznamů včetně případné detekované chybové odrážky pocházející z exekucí uvnitř interakcí.
 */
data class RecordsUiState(
    val records: List<RecordEntity> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Životem provázaný ViewModel spravující logiku zobrazení detailu jedné vymezené kolekce pokrývající kompletní
 * sestavu jejích polí a databézový odvětví vyhodnocené record flow dynamické entity pro zobrazení reálných obsahu polí.
 */
class CollectionDetailViewModel(
    val selectedCollectionId: Long,
    private val collectionRepository: CollectionRepository,
    private val fieldDefinitionRepository: FieldDefinitionRepository,
    private val getRecordsForCollectionUseCase: GetRecordsForCollectionUseCase,
    private val addRecordUseCase: AddRecordUseCase,
    private val updateRecordUseCase: UpdateRecordUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val addFieldUseCase: AddFieldUseCase,
    private val exportCsvUseCase: ExportCsvUseCase,
    private val importCsvUseCase: ImportCsvUseCase
) : ViewModel() {

    companion object {
        fun provideFactory(collectionId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ZenbaseApp
                val container = app.container
                CollectionDetailViewModel(
                    selectedCollectionId = collectionId,
                    collectionRepository = container.collectionRepository,
                    fieldDefinitionRepository = container.fieldDefinitionRepository,
                    getRecordsForCollectionUseCase = container.getRecordsForCollectionUseCase,
                    addRecordUseCase = container.addRecordUseCase,
                    updateRecordUseCase = container.updateRecordUseCase,
                    deleteRecordUseCase = container.deleteRecordUseCase,
                    addFieldUseCase = container.addFieldUseCase,
                    exportCsvUseCase = container.exportCsvUseCase,
                    importCsvUseCase = container.importCsvUseCase
                )
            }
        }
    }

    private val collectionId: Long = selectedCollectionId

    val collection: StateFlow<Collection?> = collectionRepository.allCollections
        .map { list -> list.find { it.id == collectionId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val fields: StateFlow<List<FieldDefinition>> = fieldDefinitionRepository.getFieldsForCollection(collectionId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortState())

    private val _recordsUiState = MutableStateFlow(RecordsUiState())
    val recordsUiState: StateFlow<RecordsUiState> = _recordsUiState.asStateFlow()

    init {
        if (collectionId != -1L) {
            viewModelScope.launch {
                combine(
                    getRecordsForCollectionUseCase(collectionId).catch { e -> _recordsUiState.update { it.copy(errorMessage = e.message) } },
                    searchQuery,
                    sortOrder
                ) { rawRecords, query, sort ->
                    val filtered = if (query.isBlank()) {
                        rawRecords
                    } else {
                        rawRecords.filter { record ->
                            val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any?>>() {}.type
                            val map: Map<String, Any?> = Gson().fromJson(record.dataJson, mapType)
                            map.values.any { value ->
                                value?.toString()?.contains(query, ignoreCase = true) == true
                            }
                        }
                    }

                    val sorted = when (sort.option) {
                        SortOption.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
                        SortOption.CREATED_ASC -> filtered.sortedBy { it.createdAt }
                        SortOption.FIELD_ASC -> {
                            val fName = sort.fieldName
                            if (fName != null) {
                                filtered.sortedBy { record ->
                                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any?>>() {}.type
                                    val map: Map<String, Any?> = Gson().fromJson(record.dataJson, mapType)
                                    map[fName]?.toString() ?: ""
                                }
                            } else filtered
                        }
                        SortOption.FIELD_DESC -> {
                            val fName = sort.fieldName
                            if (fName != null) {
                                filtered.sortedByDescending { record ->
                                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any?>>() {}.type
                                    val map: Map<String, Any?> = Gson().fromJson(record.dataJson, mapType)
                                    map[fName]?.toString() ?: ""
                                }
                            } else filtered
                        }
                    }
                    sorted
                }.collect { finalList ->
                    _recordsUiState.update { it.copy(records = finalList, errorMessage = null) }
                }
            }
        }
    }

    fun addFieldMetadata(name: String, label: String, type: FieldType) {
        viewModelScope.launch {
            try {
                val newField = FieldDefinition(
                    id = 0,
                    collectionId = collectionId,
                    fieldName = name,
                    fieldLabel = label,
                    fieldType = type,
                    isRequired = false,
                    orderIndex = 0
                )
                addFieldUseCase(collectionId, newField)
            } catch (e: Exception) {
                _recordsUiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun appendNewRecord(dataMap: Map<String, Any?>) {
        viewModelScope.launch {
            try {
                val json = Gson().toJson(dataMap)
                addRecordUseCase(collectionId, json)
            } catch (e: Exception) {
                _recordsUiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun updateExistingRecord(recordId: Long, dataMap: Map<String, Any?>) {
        viewModelScope.launch {
            try {
                val json = Gson().toJson(dataMap)
                val existingRecord = recordsUiState.value.records.find { it.id == recordId }
                if (existingRecord != null) {
                    updateRecordUseCase(existingRecord, json)
                }
            } catch (e: Exception) {
                _recordsUiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateSortOrder(sortState: SortState) {
        sortOrder.value = sortState
    }

    fun exportToCsv() {
        viewModelScope.launch {
            try {
                exportCsvUseCase(collectionId)
            } catch (e: Exception) {
                _recordsUiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun importFromCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val count = importCsvUseCase(collectionId, uri)
                // Notifikace nebo refresh je řešena flow a toastem
            } catch (e: Exception) {
                _recordsUiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /**
     * Přenese nový balík JSON argumentů skrze zapozdřelý validovaný use case do nitra transakcí SQLite.
     * @param dataJson Textový objem složený v adekvátní Map podstaty.
     */
    fun addRecord(dataJson: String) {
        viewModelScope.launch {
            try {
                addRecordUseCase(collectionId, dataJson)
            } catch (e: Exception) {
                _recordsUiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /**
     * Odstřihnutí Recordu z celostní databáze a vizualizace bez okliky.
     * @param record Doménově databázy provázaná kontejnerova klasifikace aktuálních dat ze sbírky.
     */
    fun deleteRecord(record: RecordEntity) {
        viewModelScope.launch {
            try {
                deleteRecordUseCase(record)
            } catch (e: Exception) {
                _recordsUiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    /**
     * Provede vyčištění oznamovače o nastalé výjimce pro opětovné zprůchodnění standardního vykreslovacího procesu appky vizuálních hlášek.
     */
    fun clearError() {
        _recordsUiState.update { it.copy(errorMessage = null) }
    }
}
