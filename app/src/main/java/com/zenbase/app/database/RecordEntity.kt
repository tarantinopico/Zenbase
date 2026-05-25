package com.zenbase.app.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entita představující jeden dynamický záznam v uživatelem definované kolekci.
 * Realizuje EAV model pomocí JSON datového sloupce.
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
data class RecordEntity(
    /**
     * Primární klíč záznamu, automaticky generovaný.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Cizí klíč patřičné [CollectionEntity].
     */
    val collectionId: Long,

    /**
     * JSON objekt reprezentující uložená data. Klíče odpovídají `fieldName` z definic polí.
     * Příklad: {"nazev":"Kniha","cena":299.9,"aktivni":1}
     */
    val dataJson: String,

    /**
     * Časové razítko vytvoření záznamu.
     */
    val createdAt: Long,

    /**
     * Časové razítko poslední aktualizace záznamu.
     */
    val updatedAt: Long
)
