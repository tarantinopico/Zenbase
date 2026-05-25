package com.example.domain.model

/**
 * Doménová reprezentace uživatelem vytvořené kolekce.
 */
data class Collection(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Abstraktní reprezentace datového typu pro definici pole.
 * Každý typ má vlastní sadu volitelných metadat.
 */
sealed class FieldType {
    /** Textové pole reprezentované řetězcem. */
    data class TextType(val defaultValue: String?) : FieldType()
    
    /** Číselné pole reprezentované přesným základním číslem. */
    data class NumberType(val defaultValue: Double?) : FieldType()
    
    /** Časové pole uchovávající údaj jako celočíselnou hodnotu (v millis epochy). */
    object DateType : FieldType()
    
    /** Binární hodnota reprezentovaná zaškrtávacím políčkem (pravda/nepravda). */
    object CheckboxType : FieldType()
    
    /** Pole typu dropdown s výběrem jedné volby ze seznamu možností. */
    data class DropdownType(val options: List<String>) : FieldType()
    
    /** Vypočítávané pole s předem definovaným vzorcem kalkulace. */
    data class ComputedType(val formula: String) : FieldType()
}

/**
 * Doménový model popisující jedno datové pole (sloupec) uvnitř konkrétní kolekce.
 */
data class FieldDefinition(
    val id: Long,
    val collectionId: Long,
    val fieldName: String,
    val fieldLabel: String,
    val fieldType: FieldType,
    val isRequired: Boolean,
    val orderIndex: Int
)
