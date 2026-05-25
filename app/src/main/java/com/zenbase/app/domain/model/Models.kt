package com.zenbase.app.domain.model

/**
 * Doménová reprezentace uživatelem vytvořené kolekce.
 */
data class Collection(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val accentColor: Int? = null,
    val iconName: String? = null
)

/**
 * Abstraktní reprezentace datového typu pro definici pole.
 * Každý typ má vlastní sadu volitelných metadat.
 */
sealed class FieldType {
    /** Textové pole reprezentované řetězcem. */
    object TextType : FieldType()
    
    /** Číselné pole reprezentované přesným základním číslem. */
    object NumberType : FieldType()
    
    /** Časové pole uchovávající údaj jako celočíselnou hodnotu (v millis epochy). */
    object DateType : FieldType()
    
    /** Binární hodnota reprezentovaná zaškrtávacím políčkem (pravda/nepravda). */
    object CheckboxType : FieldType()
    
    /** Pole typu dropdown s výběrem jedné volby ze seznamu možností. */
    data class DropdownType(val options: List<String>) : FieldType()
    
    /** Vypočítávané pole s předem definovaným vzorcem kalkulace. */
    data class ComputedType(val formula: String) : FieldType()
    
    /** Pole pro uložení URI obrázku (String). */
    object ImageType : FieldType()
    
    /** Pole pro ověřenou URL adresu (String). */
    object UrlType : FieldType()
    
    /** Pole pro hodnocení 0-5 (celé číslo). */
    object RatingType : FieldType()
    
    /** Pole pro hodnotu čárového kódu. */
    object BarcodeType : FieldType()
    
    /** Pole pro zeměpisnou délku a šířku uloženou jako mapa či objekt. */
    object LocationType : FieldType()
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
    val orderIndex: Int,
    val isUnique: Boolean = false,
    val validationRegex: String? = null,
    val defaultValue: Any? = null
)
