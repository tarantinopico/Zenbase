package com.zenbase.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entita reprezentující uživatelem vytvořenou kolekci.
 * Slouží jako tabulka "CollectionEntity" v Room databázi.
 */
@Entity
data class CollectionEntity(
    /**
     * Primární klíč kolekce, automaticky generovaný.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Název kolekce, definovaný uživatelem.
     */
    val name: String,

    /**
     * Časové razítko vytvoření kolekce (v epoch milisekundách).
     */
    val createdAt: Long,

    /**
     * Časové razítko poslední úpravy kolekce (v epoch milisekundách).
     */
    val updatedAt: Long,

    /**
     * Akcentní barva kolekce pro customizaci UI (ARGB Int).
     */
    val accentColor: Int? = null,

    /**
     * Název Material ikony specifikující typ kolekce.
     */
    val iconName: String? = null
)
