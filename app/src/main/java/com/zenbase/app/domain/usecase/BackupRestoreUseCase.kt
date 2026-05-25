package com.zenbase.app.domain.usecase

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.zenbase.app.database.ZenbaseDatabase
import com.zenbase.app.database.CollectionEntity
import com.zenbase.app.database.FieldDefinitionEntity
import com.zenbase.app.database.RecordEntity
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class BackupStructure(
    val collections: List<CollectionEntity>,
    val fields: List<FieldDefinitionEntity>,
    val records: List<RecordEntity>
)

/**
 * Zastřešuje export a import celé databáze ve formátu JSON pro zálohování a obnovení.
 */
class BackupRestoreUseCase @Inject constructor(
    private val database: ZenbaseDatabase,
    private val context: Context
) {
    suspend fun createBackup(): Uri {
        val collections = database.collectionDao().getAllCollections().first()
        val fields = database.fieldDefinitionDao().getAllFieldsOnce()
        val records = database.recordDao().getAllRecordsOnce()
        
        val backup = BackupStructure(collections, fields, records)
        val json = Gson().toJson(backup)
        
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(downloadsDir, "zenbase_backup_$timestamp.json")
        
        FileWriter(file).use { writer ->
            writer.write(json)
        }
        
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    suspend fun restoreBackup(uri: Uri) {
        val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            InputStreamReader(inputStream).readText()
        } ?: throw IllegalArgumentException("Nelze přečíst zálohu.")
        
        val backup = Gson().fromJson(json, BackupStructure::class.java)
        
        database.clearAllTables()
        
        backup.collections.forEach { database.collectionDao().insertCollection(it) }
        backup.fields.forEach { database.fieldDefinitionDao().insertField(it) }
        backup.records.forEach { database.recordDao().insertRecord(it) }
    }
}
