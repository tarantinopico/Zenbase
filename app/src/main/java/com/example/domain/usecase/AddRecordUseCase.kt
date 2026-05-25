package com.example.domain.usecase

import com.example.domain.repository.RecordRepository
import com.example.domain.repository.FieldDefinitionRepository
import com.example.domain.engine.FormulaEvaluator
import com.example.domain.model.FieldType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case pro přidání záznamu s prověřením validací vstupních podnětů (zda-li nejsou pominuty zásadní požadovaná označení) 
 * a dynamického přepočtu všech schémat pro definované "COMPUTED" varianty vlastních algoritmických rovnic.
 */
class AddRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
    private val fieldDefinitionRepository: FieldDefinitionRepository,
    private val formulaEvaluator: FormulaEvaluator
) {
    private val gson = Gson()

    /**
     * Vloží nový záznam se souhrnným provedením výpočtů a validací pro specifické EAV definice json struktury.
     * @param collectionId ID nadřazené kolekce, která schraňuje strukturu a propojena data.
     * @param dataJson JSON reprezentující hrubé hodnoty zadané uživatelem napřímo ve formuláři, případně jinou složkou.
     * @return Vygenerované databázní Long ID nově zrozené konfirmační entity po commit procesu v db.
     */
    suspend operator fun invoke(collectionId: Long, dataJson: String): Long {
        val type = object : TypeToken<MutableMap<String, Any?>>() {}.type
        val recordDataMap: MutableMap<String, Any?> = gson.fromJson(dataJson, type) ?: mutableMapOf()

        // Asynchronní natažení první reálné definované platné struktury polí v rámci doménových logik Flow cyklu
        val fields = fieldDefinitionRepository.getFieldsForCollectionFlow(collectionId).first()

        // 1. Zavedení absolutní Validace polí a restrikce chyb ze strany vstupu koncového operátora
        for (field in fields) {
            if (field.isRequired && field.fieldType !is FieldType.ComputedType) {
                val value = recordDataMap[field.fieldName]
                if (value == null || value.toString().isBlank()) {
                    throw IllegalArgumentException("Dosažena chyba vstupu: Chybí označení pro hodnotu patřící '${field.fieldLabel}'.")
                }
            }
        }

        // 2. Přepočet hodnot uvnitř komplexních COMPUTED metrik přes vnořený Formula Evaluator
        val computedFields = fields.filter { it.fieldType is FieldType.ComputedType }
        for (field in computedFields) {
            val formula = (field.fieldType as FieldType.ComputedType).formula
            try {
                val result = formulaEvaluator.evaluate(formula, recordDataMap)
                recordDataMap[field.fieldName] = result
            } catch (e: Exception) {
                // Konkrétní pole nebude zastavovat celostní proces avšak upozorní implicitně zapíše nulu.
                recordDataMap[field.fieldName] = 0.0
            }
        }

        val enrichedJson = gson.toJson(recordDataMap)
        return recordRepository.addRecord(collectionId, enrichedJson)
    }
}
