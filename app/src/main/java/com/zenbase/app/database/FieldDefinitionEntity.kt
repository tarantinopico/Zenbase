package com.zenbase.app.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entita definující jedno pole (sloupec) v kolekci.
 * Ukládá metadata pro každý typ pole, vázaná na konkrétní [CollectionEntity] id.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["collectionId"])]
)
data class FieldDefinitionEntity(
    /**
     * Primární klíč pole, automaticky generovaný.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Cizí klíč do [CollectionEntity].
     */
    val collectionId: Long,

    /**
     * Interní název pole sloužící jako klíč (např. "cena_bez_dph").
     */
    val fieldName: String,

    /**
     * Zobrazovaný název pole pro uživatele (např. "Cena bez DPH").
     */
    val fieldLabel: String,

    /**
     * Výčtový typ pole uložený jako řetězec.
     * Povolené hodnoty: TEXT, NUMBER, DATE, CHECKBOX, DROPDOWN, COMPUTED.
     */
    val fieldType: String,

    /**
     * Zda je vyplnění pole povinné.
     */
    val isRequired: Boolean,

    /**
     * Pořadí pole v uživatelském rozhraní (formuláři).
     */
    val orderIndex: Int,

    /**
     * JSON řetězec pro doplňková metadata (např. možnosti pro DROPDOWN nebo vzorec pro COMPUTED).
     */
    val optionsJson: String?,

    /**
     * Časové razítko vytvoření definice pole (v epoch milisekundách).
     */
    val createdAt: Long,

    /**
     * Volitelná výchozí hodnota pole uložená ve formátu JSON.
     */
    val defaultValueJson: String? = null,

    /**
     * Informace, zda hodnota v tomto poli musí být napříč záznamy kolekce unikátní.
     */
    val isUnique: Boolean = false,

    /**
     * Volitelný regulární výraz pro stringovou validaci (pouze pro TEXT a URL).
     */
    val validationRegex: String? = null
)
