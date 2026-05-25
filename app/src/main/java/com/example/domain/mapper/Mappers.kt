package com.example.domain.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonObject
import com.example.database.CollectionEntity
import com.example.database.FieldDefinitionEntity
import com.example.domain.model.Collection
import com.example.domain.model.FieldDefinition
import com.example.domain.model.FieldType

private val gson = Gson()

/**
 * Převádí doménový model Collection na databázovou entitu CollectionEntity.
 */
fun Collection.toEntity(): CollectionEntity {
    return CollectionEntity(
        id = this.id,
        name = this.name,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Převádí databázovou entitu CollectionEntity na doménový model Collection.
 */
fun CollectionEntity.toDomain(): Collection {
    return Collection(
        id = this.id,
        name = this.name,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Převádí doménový model FieldDefinition na databázovou entitu FieldDefinitionEntity.
 */
fun FieldDefinition.toEntity(): FieldDefinitionEntity {
    val fieldTypeStr: String
    var optionsJsonStr: String? = null

    when (val t = this.fieldType) {
        is FieldType.TextType -> {
            fieldTypeStr = "TEXT"
            if (t.defaultValue != null) {
                val json = JsonObject()
                json.addProperty("defaultValue", t.defaultValue)
                optionsJsonStr = gson.toJson(json)
            }
        }
        is FieldType.NumberType -> {
            fieldTypeStr = "NUMBER"
            if (t.defaultValue != null) {
                val json = JsonObject()
                json.addProperty("defaultValue", t.defaultValue)
                optionsJsonStr = gson.toJson(json)
            }
        }
        is FieldType.DateType -> {
            fieldTypeStr = "DATE"
        }
        is FieldType.CheckboxType -> {
            fieldTypeStr = "CHECKBOX"
        }
        is FieldType.DropdownType -> {
            fieldTypeStr = "DROPDOWN"
            optionsJsonStr = gson.toJson(t.options)
        }
        is FieldType.ComputedType -> {
            fieldTypeStr = "COMPUTED"
            val json = JsonObject()
            json.addProperty("formula", t.formula)
            optionsJsonStr = gson.toJson(json)
        }
    }

    return FieldDefinitionEntity(
        id = this.id,
        collectionId = this.collectionId,
        fieldName = this.fieldName,
        fieldLabel = this.fieldLabel,
        fieldType = fieldTypeStr,
        isRequired = this.isRequired,
        orderIndex = this.orderIndex,
        optionsJson = optionsJsonStr,
        createdAt = System.currentTimeMillis()
    )
}

/**
 * Převádí databázovou entitu FieldDefinitionEntity na doménový model FieldDefinition.
 */
fun FieldDefinitionEntity.toDomain(): FieldDefinition {
    val type: FieldType = when (this.fieldType) {
        "TEXT" -> {
            var defaultVal: String? = null
            if (!this.optionsJson.isNullOrEmpty()) {
                try {
                    val root = gson.fromJson(this.optionsJson, JsonObject::class.java)
                    if (root.has("defaultValue") && !root.get("defaultValue").isJsonNull) {
                        defaultVal = root.get("defaultValue").asString
                    }
                } catch (e: Exception) {
                    // Ignorujeme chyby při parsování
                }
            }
            FieldType.TextType(defaultVal)
        }
        "NUMBER" -> {
            var defaultVal: Double? = null
            if (!this.optionsJson.isNullOrEmpty()) {
                try {
                    val root = gson.fromJson(this.optionsJson, JsonObject::class.java)
                    if (root.has("defaultValue") && !root.get("defaultValue").isJsonNull) {
                        defaultVal = root.get("defaultValue").asDouble
                    }
                } catch (e: Exception) {
                    // Ignorujeme chyby
                }
            }
            FieldType.NumberType(defaultVal)
        }
        "DATE" -> FieldType.DateType
        "CHECKBOX" -> FieldType.CheckboxType
        "DROPDOWN" -> {
            var options = emptyList<String>()
            if (!this.optionsJson.isNullOrEmpty()) {
                try {
                    val listType = object : TypeToken<List<String>>() {}.type
                    options = gson.fromJson(this.optionsJson, listType) ?: emptyList()
                } catch (e: Exception) {
                    // V případě problému ponecháme prázdný list
                }
            }
            FieldType.DropdownType(options)
        }
        "COMPUTED" -> {
            var formula = ""
            if (!this.optionsJson.isNullOrEmpty()) {
                try {
                    val root = gson.fromJson(this.optionsJson, JsonObject::class.java)
                    if (root.has("formula")) {
                        formula = root.get("formula").asString
                    }
                } catch (e: Exception) {
                    // Ignorujeme
                }
            }
            FieldType.ComputedType(formula)
        }
        else -> FieldType.TextType(null) // Fallback jako spolehlivý default
    }

    return FieldDefinition(
        id = this.id,
        collectionId = this.collectionId,
        fieldName = this.fieldName,
        fieldLabel = this.fieldLabel,
        fieldType = type,
        isRequired = this.isRequired,
        orderIndex = this.orderIndex
    )
}
