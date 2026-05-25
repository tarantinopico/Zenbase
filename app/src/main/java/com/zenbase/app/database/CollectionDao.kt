package com.zenbase.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Objekt pro přístup k datům (DAO) pro tabulku [CollectionEntity].
 */
@Dao
interface CollectionDao {

    /**
     * Získá všechny kolekce seřazené podle času poslední aktualizace sestupně.
     * Reaktivní dotaz vrací [Flow].
     */
    @Query("SELECT * FROM collectionentity ORDER BY updatedAt DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    /**
     * Vloží novou kolekci do databáze.
     * @return Generované ID nové kolekce.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    /**
     * Aktualizuje existující kolekci.
     */
    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    /**
     * Odstraní kolekci z databáze.
     */
    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)
}
