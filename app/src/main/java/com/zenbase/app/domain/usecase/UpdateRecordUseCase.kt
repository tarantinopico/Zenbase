package com.zenbase.app.domain.usecase

import com.zenbase.app.database.RecordEntity
import com.zenbase.app.domain.repository.RecordRepository
import com.zenbase.app.domain.repository.FieldDefinitionRepository
import com.zenbase.app.domain.engine.FormulaEvaluator
import com.zenbase.app.domain.model.FieldType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Účelová systémová operace určená k provedení opravy aktuálního stavu obsaženého dynamického datového typu
 * uvnitř starší EAV Record Entity s integrací prověřit případných vazebních re-vyhodnocení uvnitř vypočtených buněk.
 */
class UpdateRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val fieldDefinitionRepository: FieldDefinitionRepository,
    private val formulaEvaluator: FormulaEvaluator
) {
    private val gson = Gson()

    /**
     * Nahradí json segment příslušné entity za nový s prozkoumáním validačního řetězce nadřazené definice.
     * @param record Konkrétní obměnívaná identita record elementu zachytávající dosud aktuální formu dat.
     * @param newDataJson Výměnné podklady od zadavatele obsahující čerstvě vyplněné vstupy.
     */
    suspend operator fun invoke(record: RecordEntity, newDataJson: String) {
        val type = object : TypeToken<MutableMap<String, Any?>>() {}.type
        val recordDataMap: MutableMap<String, Any?> = gson.fromJson(newDataJson, type) ?: mutableMapOf()

        val fields = fieldDefinitionRepository.getFieldsForCollectionFlow(record.collectionId).first()

        for (field in fields) {
            if (field.isRequired && field.fieldType !is FieldType.ComputedType) {
                val value = recordDataMap[field.fieldName]
                if (value == null || value.toString().isBlank()) {
                    throw IllegalArgumentException("Odmítnuto uložení: Vyplňte plnohodnotně prvek '${field.fieldLabel}'.")
                }
            }
        }

        val computedFields = fields.filter { it.fieldType is FieldType.ComputedType }
        for (field in computedFields) {
            val formula = (field.fieldType as FieldType.ComputedType).formula
            try {
                val result = formulaEvaluator.evaluate(formula, recordDataMap)
                recordDataMap[field.fieldName] = result
            } catch (e: Exception) {
                recordDataMap[field.fieldName] = 0.0
            }
        }

        val enrichedJson = gson.toJson(recordDataMap)
        val updatedRecord = record.copy(dataJson = enrichedJson)
        recordRepository.updateRecord(updatedRecord)
    }
}
