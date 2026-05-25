package com.zenbase.app.domain.usecase

import android.content.Context
import android.os.Environment
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zenbase.app.domain.repository.FieldDefinitionRepository
import com.zenbase.app.domain.repository.RecordRepository
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * UseCase pro export záznamů kolekce do formátu CSV.
 */
class ExportCsvUseCase @Inject constructor(
    private val fieldDefinitionRepository: FieldDefinitionRepository,
    private val recordRepository: RecordRepository,
    private val context: Context
) {
    suspend operator fun invoke(collectionId: Long) {
        val fields = fieldDefinitionRepository.getFieldsForCollection(collectionId).first()
        val records = recordRepository.getRecordsForCollection(collectionId).first()
        
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(downloadsDir, "zenbase_export_$timestamp.csv")
        
        FileWriter(file).use { writer ->
            // Header
            val header = fields.joinToString(",") { "\"${it.fieldLabel.replace("\"", "\"\"")}\"" }
            writer.append("$header\n")
            
            // Rows
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val gson = Gson()
            
            records.forEach { record ->
                val dataMap: Map<String, Any?> = gson.fromJson(record.dataJson, mapType)
                val row = fields.joinToString(",") { field ->
                    val value = dataMap[field.fieldName]
                    val formattedValue = when (field.fieldType) {
                        is com.zenbase.app.domain.model.FieldType.DateType -> {
                            val millis = (value as? Number)?.toLong() ?: 0L
                            if (millis > 0) SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis)) else ""
                        }
                        is com.zenbase.app.domain.model.FieldType.CheckboxType -> {
                            if (value == true) "Ano" else "Ne"
                        }
                        is com.zenbase.app.domain.model.FieldType.LocationType -> {
                            val map = value as? Map<*, *>
                            val lat = map?.get("lat")?.toString() ?: "0.0"
                            val lng = map?.get("lng")?.toString() ?: "0.0"
                            "$lat,$lng"
                        }
                        else -> value?.toString() ?: ""
                    }
                    "\"${formattedValue.replace("\"", "\"\"")}\""
                }
                writer.append("$row\n")
            }
        }
        
        // Setup sharing
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = ShareCompat.IntentBuilder(context)
            .setType("text/csv")
            .setStream(uri)
            .setChooserTitle("Sdílet CSV export")
            .createChooserIntent()
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        
        // Start intent from context (requires FLAG_ACTIVITY_NEW_TASK from outside activity)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
