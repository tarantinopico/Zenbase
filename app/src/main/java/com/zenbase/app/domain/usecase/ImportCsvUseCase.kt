package com.zenbase.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.zenbase.app.domain.model.FieldDefinition
import com.zenbase.app.domain.repository.FieldDefinitionRepository
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * UseCase pro import záznamů z vybraného CSV souboru do určené kolekce.
 */
class ImportCsvUseCase @Inject constructor(
    private val fieldDefinitionRepository: FieldDefinitionRepository,
    private val addRecordUseCase: AddRecordUseCase,
    private val context: Context
) {
    /**
     * Přečte obsah Uri, najde shodu v záhlaví a přidá záznamy do `collectionId`.
     * Předpokládá standardní formát oddělený čárkami s escapováním pomocí úvozovek.
     * Pro komplexnější MVP je zde jednoduchý parser.
     */
    suspend operator fun invoke(collectionId: Long, uri: Uri): Int {
        val fields = fieldDefinitionRepository.getFieldsForCollection(collectionId).first()
        var importCount = 0
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            val headerLine = reader.readLine() ?: return 0
            
            val headers = parseCsvLine(headerLine)
            val headerToFieldMap = mutableMapOf<Int, FieldDefinition>()
            
            headers.forEachIndexed { index, header ->
                val field = fields.find { it.fieldLabel == header }
                if (field != null) {
                    headerToFieldMap[index] = field
                }
            }
            
            if (headerToFieldMap.isEmpty()) return 0
            
            val gson = Gson()
            var line = reader.readLine()
            while (line != null) {
                val values = parseCsvLine(line)
                val dataMap = mutableMapOf<String, Any?>()
                
                headerToFieldMap.forEach { (index, field) ->
                    if (index < values.size) {
                        val stringValue = values[index]
                        val parsedValue = parseValue(stringValue, field.fieldType)
                        if (parsedValue != null) {
                            dataMap[field.fieldName] = parsedValue
                        }
                    }
                }
                
                if (dataMap.isNotEmpty()) {
                    try {
                        addRecordUseCase(collectionId, gson.toJson(dataMap))
                        importCount++
                    } catch (e: Exception) {
                        // Ignorujeme chybu u konkrétního záznamu a pokračujeme
                    }
                }
                line = reader.readLine()
            }
        }
        return importCount
    }
    
    private fun parseValue(value: String, type: com.zenbase.app.domain.model.FieldType): Any? {
        if (value.isBlank()) return null
        return when (type) {
            is com.zenbase.app.domain.model.FieldType.NumberType -> value.toDoubleOrNull()
            is com.zenbase.app.domain.model.FieldType.CheckboxType -> value.equals("Ano", ignoreCase = true)
            is com.zenbase.app.domain.model.FieldType.LocationType -> {
                val parts = value.split(",")
                if (parts.size == 2) {
                    mapOf("lat" to (parts[0].toDoubleOrNull() ?: 0.0), "lng" to (parts[1].toDoubleOrNull() ?: 0.0))
                } else null
            }
            else -> value
        }
    }
    
    // Jednoduchý parser CSV řádku podporující texty v uvozovkách
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    current.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString())
                current.clear()
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
