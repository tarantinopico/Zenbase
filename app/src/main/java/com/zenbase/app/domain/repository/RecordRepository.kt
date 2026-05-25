package com.zenbase.app.domain.repository

import com.zenbase.app.database.RecordDao
import com.zenbase.app.database.RecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centrální repozitář pro manipulaci s dynamickými řádky metadat (záznamy), 
 * zastupující faktická surová data (RecordEntity).
 */
@Singleton
class RecordRepository @Inject constructor(
    private val recordDao: RecordDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Odstíní datovou logiku pro dotazování se na listování entit pod určitou kolekcí.
     * Obaluje návrat do stabilního StateFlow.
     * @param collectionId Identifikátor mateřské kolekce, ze které se selektuje.
     */
    fun getRecordsForCollection(collectionId: Long): StateFlow<List<RecordEntity>> {
        return recordDao.getRecordsForCollection(collectionId)
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    /**
     * Získá přímou instanci přesného záznamu. Určeno pro přímé vyžadování a detailní editační dialog.
     * @param id Přesný PK index.
     * @return Validní RecordEntity ze samotné interní paměti SQLite (potažmo Room databáze) nebo null.
     */
    suspend fun getRecordById(id: Long): RecordEntity? {
        return recordDao.getRecordById(id)
    }

    /**
     * Zavede nový dynamický blok do databáze za provedení nastavení platných datových razítek.
     * @param collectionId Směr určení - do které tabulky.
     * @param dataJson Složený syntetický formát json soustřeďující EAV vlastnosti v jeden element row sloupce.
     * @return Přidělený klíč long pro následnou filtraci a synchronizaci do Flow.
     */
    suspend fun addRecord(collectionId: Long, dataJson: String): Long {
        val now = System.currentTimeMillis()
        val entity = RecordEntity(
            collectionId = collectionId,
            dataJson = dataJson,
            createdAt = now,
            updatedAt = now
        )
        return recordDao.insertRecord(entity)
    }

    /**
     * Modifikuje doposud existující prvek přímou interakcí.
     * Metoda zajistí interní reset timestamps k určení verze iterace.
     * @param record Současná RecordEntitia propisující požadované záměny.
     */
    suspend fun updateRecord(record: RecordEntity) {
        val updatedRecord = record.copy(updatedAt = System.currentTimeMillis())
        recordDao.updateRecord(updatedRecord)
    }

    /**
     * Odstranění z DB navždy a neodvratitelně.
     * @param record Předává instanci ke smazání.
     */
    suspend fun deleteRecord(record: RecordEntity) {
        recordDao.deleteRecord(record)
    }
}
