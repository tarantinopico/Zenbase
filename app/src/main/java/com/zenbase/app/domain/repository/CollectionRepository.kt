package com.zenbase.app.domain.repository

import com.zenbase.app.database.CollectionDao
import com.zenbase.app.database.CollectionEntity
import com.zenbase.app.domain.mapper.toDomain
import com.zenbase.app.domain.mapper.toEntity
import com.zenbase.app.domain.model.Collection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozitář zapouzdřující vrstvu logiky práce s databázovými Kolekcemi.
 * Zajišťuje potřebná namapování mezi API Roomu a zbytkem doménové vrstvy aplikace.
 */
@Singleton
class CollectionRepository @Inject constructor(
    private val collectionDao: CollectionDao
) {
    // Vytvoříme dedikovaný job scope, díky kterému můžeme udržovat sdílený StateFlow
    // i když momentálně nejsou žádní odběratelé na UI.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Představuje StateFlow obsahující seznam všech dostupných doménových modelů kolekcí,
     * který se naplňuje asynchronně přímo z Room databáze.
     */
    val allCollections: StateFlow<List<Collection>> = collectionDao.getAllCollections()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Vytvoří a zapíše novou CollectionEntity kolekci s aktuálním časem.
     * @param name Název zadávaný uživatelem pro formální založení skupiny záznamů.
     * @return Systémově interní ID generované po vložení nového záznamu.
     */
    suspend fun createCollection(name: String): Long {
        val now = System.currentTimeMillis()
        val entity = CollectionEntity(
            name = name,
            createdAt = now,
            updatedAt = now
        )
        return collectionDao.insertCollection(entity)
    }

    /**
     * Uloží specifikované modifikace do existující kolekce a zaznamená v jejích atributech přesný časový stempel.
     * @param collection Změněný doménový objekt kolekce.
     */
    suspend fun updateCollection(collection: Collection) {
        val entity = collection.copy(updatedAt = System.currentTimeMillis()).toEntity()
        collectionDao.updateCollection(entity)
    }

    /**
     * Využije definované DAO propažení mazání a nenávratně odstraní patřičnou kolekci.
     * Systém je nastaven na kaskádovou likvidaci veškerých polí a uložených záznamů uvnitř.
     * @param collection Konkrétní pojená kolekce k smazání.
     */
    suspend fun deleteCollection(collection: Collection) {
        collectionDao.deleteCollection(collection.toEntity())
    }
}
