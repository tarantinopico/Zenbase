package com.example.domain.usecase

import com.example.domain.model.FieldDefinition
import com.example.domain.repository.FieldDefinitionRepository
import javax.inject.Inject

/**
 * Implementační vrchní komponenta navržena za účelem správy přidání libovolných sloupcových konfigurací.
 */
class AddFieldUseCase @Inject constructor(
    private val fieldDefinitionRepository: FieldDefinitionRepository
) {
    /**
     * Zastřešuje přidání zvalidovaného formátu doménové entity představující sloupec - field definition.
     * @param collectionId Do které tabulky se prvek chystá.
     * @param fieldDefinition Popisovač nově vytvořeného konfiguračního pole.
     * @return Long interní databázové identifikační číslo.
     */
    suspend operator fun invoke(collectionId: Long, fieldDefinition: FieldDefinition): Long {
        if (fieldDefinition.fieldName.isBlank() || fieldDefinition.fieldLabel.isBlank()) {
            throw IllegalArgumentException("Název pole a zobrazovaný název nesmí sestávat z prazdných řetězců.")
        }
        return fieldDefinitionRepository.addField(collectionId, fieldDefinition)
    }
}
