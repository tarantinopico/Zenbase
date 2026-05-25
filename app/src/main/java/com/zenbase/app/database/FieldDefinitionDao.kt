package com.zenbase.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Objekt pro přístup k datům (DAO) pro tabulku [FieldDefinitionEntity].
 */
@Dao
interface FieldDefinitionDao {

    /**
     * Získá definice polí pro konkrétní kolekci, seřazené podle 'orderIndex'.
     * Vrací změny databázových záznamů reaktivně jako [Flow].
     */
    @Query("SELECT * FROM fielddefinitionentity WHERE collectionId = :collectionId ORDER BY orderIndex")
    fun getFieldsForCollection(collectionId: Long): Flow<List<FieldDefinitionEntity>>

    /**
     * Vloží definici nového pole do databáze.
     * @return ID vloženého záznamu.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: FieldDefinitionEntity): Long

    /**
     * Aktualizuje definici pole.
     */
    @Update
    suspend fun updateField(field: FieldDefinitionEntity)

    /**
     * Smaže definici pole.
     */
    @Delete
    suspend fun deleteField(field: FieldDefinitionEntity)
    @Query("SELECT * FROM FieldDefinitionEntity")
    suspend fun getAllFieldsOnce(): List<FieldDefinitionEntity>
}
