package com.zenbase.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Objekt pro přístup k datům (DAO) pro tabulku [RecordEntity].
 */
@Dao
interface RecordDao {

    /**
     * Získá veškeré záznamy pro danou kolekci, seřazené od nejnověji upravených.
     */
    @Query("SELECT * FROM recordentity WHERE collectionId = :collectionId ORDER BY updatedAt DESC")
    fun getRecordsForCollection(collectionId: Long): Flow<List<RecordEntity>>

    /**
     * Vrátí prvek reprezentující jediný záznam dle zadaného ID (vrací null pokud neexistuje).
     */
    @Query("SELECT * FROM recordentity WHERE id = :id")
    suspend fun getRecordById(id: Long): RecordEntity?

    /**
     * Vloží nový dynamický záznam do databáze.
     * @return ID vloženého záznamu.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity): Long

    /**
     * Aktualizuje existující dynamický záznam databáze.
     */
    @Update
    suspend fun updateRecord(record: RecordEntity)

    /**
     * Odstraní konkrétní záznam z databáze.
     */
    @Delete
    suspend fun deleteRecord(record: RecordEntity)
    @Query("SELECT * FROM RecordEntity")
    suspend fun getAllRecordsOnce(): List<RecordEntity>
}
