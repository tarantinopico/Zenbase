package com.example.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt modul pro poskytování instancí k práci s databází pro injekci závislostí.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Poskytne singleton instanci roomové databáze [ZenbaseDatabase].
     */
    @Provides
    @Singleton
    fun provideZenbaseDatabase(
        @ApplicationContext context: Context
    ): ZenbaseDatabase {
        return Room.databaseBuilder(
            context,
            ZenbaseDatabase::class.java,
            "zenbase_database"
        ).build()
    }

    /**
     * Poskytne singleton instanci [CollectionDao].
     */
    @Provides
    @Singleton
    fun provideCollectionDao(database: ZenbaseDatabase): CollectionDao {
        return database.collectionDao()
    }

    /**
     * Poskytne singleton instanci [FieldDefinitionDao].
     */
    @Provides
    @Singleton
    fun provideFieldDefinitionDao(database: ZenbaseDatabase): FieldDefinitionDao {
        return database.fieldDefinitionDao()
    }

    /**
     * Poskytne singleton instanci [RecordDao].
     */
    @Provides
    @Singleton
    fun provideRecordDao(database: ZenbaseDatabase): RecordDao {
        return database.recordDao()
    }
}
