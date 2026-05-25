package com.zenbase.app.di

import android.content.Context
import androidx.room.Room
import com.zenbase.app.database.CollectionDao
import com.zenbase.app.database.FieldDefinitionDao
import com.zenbase.app.database.RecordDao
import com.zenbase.app.database.ZenbaseDatabase
import com.zenbase.app.domain.engine.FormulaEvaluator
import com.zenbase.app.domain.repository.CollectionRepository
import com.zenbase.app.domain.repository.FieldDefinitionRepository
import com.zenbase.app.domain.repository.RecordRepository
import com.zenbase.app.domain.usecase.AddFieldUseCase
import com.zenbase.app.domain.usecase.AddRecordUseCase
import com.zenbase.app.domain.usecase.CreateCollectionUseCase
import com.zenbase.app.domain.usecase.DeleteRecordUseCase
import com.zenbase.app.domain.usecase.GetRecordsForCollectionUseCase
import com.zenbase.app.domain.usecase.UpdateRecordUseCase

import com.zenbase.app.domain.usecase.ExportCsvUseCase
import com.zenbase.app.domain.usecase.ImportCsvUseCase
import com.zenbase.app.domain.usecase.BackupRestoreUseCase

/**
 * Jednoduchý Dependency Injection Container nahrazující Hilt pro tento konkrétní kontejnerový build.
 */
class AppContainer(val context: Context) {
    val database: ZenbaseDatabase by lazy {
        Room.databaseBuilder(
            context,
            ZenbaseDatabase::class.java,
            "zenbase_database"
        ).fallbackToDestructiveMigration().build()
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
    
    val exportCsvUseCase: ExportCsvUseCase by lazy { ExportCsvUseCase(fieldDefinitionRepository, recordRepository, context) }
    val importCsvUseCase: ImportCsvUseCase by lazy { ImportCsvUseCase(fieldDefinitionRepository, addRecordUseCase, context) }
    val backupRestoreUseCase: BackupRestoreUseCase by lazy { BackupRestoreUseCase(database, context) }
}
