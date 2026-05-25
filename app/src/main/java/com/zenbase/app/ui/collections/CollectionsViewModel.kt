package com.zenbase.app.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenbase.app.ZenbaseApp
import com.zenbase.app.domain.model.Collection
import com.zenbase.app.domain.usecase.CreateCollectionUseCase
import com.zenbase.app.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel spravující úvodní obrazovku aplikace se seznamem dynamických kolekcí.
 */
class CollectionsViewModel(
    private val collectionRepository: CollectionRepository,
    private val createCollectionUseCase: CreateCollectionUseCase
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ZenbaseApp
                val container = app.container
                CollectionsViewModel(
                    collectionRepository = container.collectionRepository,
                    createCollectionUseCase = container.createCollectionUseCase
                )
            }
        }
    }

    /**
     * Reaktivní seznam všech vytvořených kolekcí v lokální databázi.
     */
    val collections: StateFlow<List<Collection>> = collectionRepository.allCollections

    /**
     * Operace delegující volání tvorby nové databázové kolekce do UseCase modulu.
     */
    fun createNewCollection(name: String) {
        viewModelScope.launch {
            createCollectionUseCase(name)
        }
    }
}
