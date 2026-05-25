package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.database.CollectionDao
import com.example.database.FieldDefinitionDao
import com.example.database.RecordDao
import com.example.database.ZenbaseDatabase
import com.example.domain.engine.FormulaEvaluator
import com.example.domain.repository.CollectionRepository
import com.example.domain.repository.FieldDefinitionRepository
import com.example.domain.repository.RecordRepository
import com.example.domain.usecase.AddFieldUseCase
import com.example.domain.usecase.AddRecordUseCase
import com.example.domain.usecase.CreateCollectionUseCase
import com.example.domain.usecase.DeleteRecordUseCase
import com.example.domain.usecase.GetRecordsForCollectionUseCase
import com.example.domain.usecase.UpdateRecordUseCase

/**
 * Jednoduchý Dependency Injection Container nahrazující Hilt pro tento konkrétní kontejnerový build.
 */
class AppContainer(context: Context) {
    val database: ZenbaseDatabase by lazy {
        Room.databaseBuilder(
            context,
            ZenbaseDatabase::class.java,
            "zenbase_database"
        ).build()
    }

    val collectionDao: CollectionDao by lazy { database.collectionDao() }
    val fieldDefinitionDao: FieldDefinitionDao by lazy { database.fieldDefinitionDao() }
    val recordDao: RecordDao by lazy { database.recordDao() }

    val collectionRepository: CollectionRepository by lazy { CollectionRepository(collectionDao) }
    val fieldDefinitionRepository: FieldDefinitionRepository by lazy { FieldDefinitionRepository(fieldDefinitionDao) }
    val recordRepository: RecordRepository by lazy { RecordRepository(recordDao) }

    val formulaEvaluator: FormulaEvaluator by lazy { FormulaEvaluator() }

    val createCollectionUseCase: CreateCollectionUseCase by lazy { CreateCollectionUseCase(collectionRepository) }
    val addFieldUseCase: AddFieldUseCase by lazy { AddFieldUseCase(fieldDefinitionRepository) }
    val getRecordsForCollectionUseCase: GetRecordsForCollectionUseCase by lazy { GetRecordsForCollectionUseCase(recordRepository) }
    val addRecordUseCase: AddRecordUseCase by lazy { AddRecordUseCase(recordRepository, fieldDefinitionRepository, formulaEvaluator) }
    val updateRecordUseCase: UpdateRecordUseCase by lazy { UpdateRecordUseCase(recordRepository, fieldDefinitionRepository, formulaEvaluator) }
    val deleteRecordUseCase: DeleteRecordUseCase by lazy { DeleteRecordUseCase(recordRepository) }
}
