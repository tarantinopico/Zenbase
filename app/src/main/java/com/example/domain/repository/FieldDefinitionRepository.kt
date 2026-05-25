package com.example.domain.repository

import com.example.database.FieldDefinitionDao
import com.example.database.FieldDefinitionEntity
import com.example.domain.mapper.toDomain
import com.example.domain.mapper.toEntity
import com.example.domain.model.FieldDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozitář zabezpečující logiku ukládání metadat definic (FieldDefinitionEntity),
 * které popisují podobu jednoho pole/sloupce uvnitř existující kolekce.
 */
@Singleton
class FieldDefinitionRepository @Inject constructor(
    private val fieldDefinitionDao: FieldDefinitionDao
) {
    // Definujeme scope taktéž zařazený do repozitáře pro StateFlow emitující kontinuálně stav polí.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Odebírá informace ohledně definic polí k přesnému Id sbírky.
     * @param collectionId ID kolekce, pro jejíž záznamy načítáme atributová pole.
     * @return StateFlow obsahující ucelený seznam připojených polních bloků v odpovídajícím doménovém převodu.
     */
    fun getFieldsForCollection(collectionId: Long): StateFlow<List<FieldDefinition>> {
        return getFieldsForCollectionFlow(collectionId)
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    /**
     * Vrátí čistý Flow využitelný pro interní dotazování.
     */
    fun getFieldsForCollectionFlow(collectionId: Long): Flow<List<FieldDefinition>> {
        return fieldDefinitionDao.getFieldsForCollection(collectionId)
            .map { list -> list.map { it.toDomain() } }
    }

    /**
     * Doplní nové pole k existující struktuře (schématu).
     * @param collectionId Zacílená kolekce, pod níž chceme definici zanořit.
     * @param fieldDefinition Doménový objekt s definicí nového sloupce.
     * @return Unikátní identifikátor vloženého objektu z Room.
     */
    suspend fun addField(collectionId: Long, fieldDefinition: FieldDefinition): Long {
        val entity = fieldDefinition.toEntity().copy(collectionId = collectionId)
        return fieldDefinitionDao.insertField(entity)
    }

    /**
     * Aktualizace atributů definice pole. Vyvolá uložení do vnitřní SQLite skrze odpovídající mechanismus DAO.
     * @param fieldDefinition Validovaný pozměněný pole objekt.
     */
    suspend fun updateField(fieldDefinition: FieldDefinition) {
        fieldDefinitionDao.updateField(fieldDefinition.toEntity())
    }

    /**
     * Vymazání existujícího konfiguračního pole (záznamy již obsažené v Record se tím ovšem automaticky nesmažou;
     * dynamika EAV však zajistí zneviditelnění v UI).
     * @param fieldDefinition Referenční objekt ke smazání.
     */
    suspend fun deleteField(fieldDefinition: FieldDefinition) {
        fieldDefinitionDao.deleteField(fieldDefinition.toEntity())
    }
}
