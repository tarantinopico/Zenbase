package com.zenbase.app.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Hlavní konfigurace Room databáze pro systém Zenbase.
 * Pokaždé když se modifikuje schéma, musí se inkrementovat číslo verze
 * a poskytnout příslušná migrace, nebo nastavit destruktivní migraci.
 */
@Database(
    entities = [
        CollectionEntity::class,
        FieldDefinitionEntity::class,
        RecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ZenbaseDatabase : RoomDatabase() {

    /**
     * Zpřístupňuje DAO pro manipulaci s objekty typu Collection.
     */
    abstract fun collectionDao(): CollectionDao

    /**
     * Zpřístupňuje DAO pro manipulaci se schématem polí kolekcí.
     */
    abstract fun fieldDefinitionDao(): FieldDefinitionDao

    /**
     * Zpřístupňuje DAO pro manipulaci s dynamickými záznamy kolekcí v EAV modelu.
     */
    abstract fun recordDao(): RecordDao
}
