package com.example.ui.collectiondetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.RecordEntity
import com.example.domain.model.Collection
import com.example.domain.model.FieldDefinition
import com.example.domain.repository.CollectionRepository
import com.example.domain.usecase.AddRecordUseCase
import com.example.domain.usecase.DeleteRecordUseCase
import com.example.domain.usecase.GetRecordsForCollectionUseCase
import com.example.domain.usecase.AddFieldUseCase
import com.example.domain.usecase.UpdateRecordUseCase
import com.example.domain.model.FieldType
import com.google.gson.Gson
import com.example.domain.repository.FieldDefinitionRepository
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ZenbaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val addFieldUseCase: AddFieldUseCase
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
                    addFieldUseCase = container.addFieldUseCase
                )
            }
        }
    }

    private val collectionId: Long = selectedCollectionId

    /**
     * Vystavená stabilní aktuální kolekce pocházející přes StateFlow mapu z agregátu centrálního celku repozitáře.
     */
    val collection: StateFlow<Collection?> = collectionRepository.allCollections
        .map { list -> list.find { it.id == collectionId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Všechny konfigurační definiční formy seřazené napřímo z příslušného repozitáře pro tuto odlišitelnou sbírku.
     */
    val fields: StateFlow<List<FieldDefinition>> = fieldDefinitionRepository.getFieldsForCollection(collectionId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _recordsUiState = MutableStateFlow(RecordsUiState())
    
    /**
     * Datová schránka plnohodnotně obsluhující vizualizaci prvků do listu (využito Compose komponentami s naslouchajícím flow).
     */
    val recordsUiState: StateFlow<RecordsUiState> = _recordsUiState.asStateFlow()

    init {
        if (collectionId != -1L) {
            viewModelScope.launch {
                getRecordsForCollectionUseCase(collectionId)
                    .catch { e -> _recordsUiState.update { it.copy(errorMessage = e.message) } }
                    .collect { recordList ->
                        _recordsUiState.update { it.copy(records = recordList, errorMessage = null) }
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
