package com.zenbase.app.domain.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonObject
import com.zenbase.app.database.CollectionEntity
import com.zenbase.app.database.FieldDefinitionEntity
import com.zenbase.app.domain.model.Collection
import com.zenbase.app.domain.model.FieldDefinition
import com.zenbase.app.domain.model.FieldType

private val gson = Gson()

/**
 * Převádí doménový model Collection na databázovou entitu CollectionEntity.
 */
fun Collection.toEntity(): CollectionEntity {
    return CollectionEntity(
        id = this.id,
        name = this.name,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        accentColor = this.accentColor,
        iconName = this.iconName
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
        updatedAt = this.updatedAt,
        accentColor = this.accentColor,
        iconName = this.iconName
    )
}

/**
 * Převádí doménový model FieldDefinition na databázovou entitu FieldDefinitionEntity.
 */
fun FieldDefinition.toEntity(): FieldDefinitionEntity {
    val fieldTypeStr: String
    var optionsJsonStr: String? = null

    when (val t = this.fieldType) {
        is FieldType.TextType -> fieldTypeStr = "TEXT"
        is FieldType.NumberType -> fieldTypeStr = "NUMBER"
        is FieldType.DateType -> fieldTypeStr = "DATE"
        is FieldType.CheckboxType -> fieldTypeStr = "CHECKBOX"
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
        is FieldType.ImageType -> fieldTypeStr = "IMAGE"
        is FieldType.UrlType -> fieldTypeStr = "URL"
        is FieldType.RatingType -> fieldTypeStr = "RATING"
        is FieldType.BarcodeType -> fieldTypeStr = "BARCODE"
        is FieldType.LocationType -> fieldTypeStr = "LOCATION"
    }

    val defaultValJson = if (this.defaultValue != null) gson.toJson(this.defaultValue) else null

    return FieldDefinitionEntity(
        id = this.id,
        collectionId = this.collectionId,
        fieldName = this.fieldName,
        fieldLabel = this.fieldLabel,
        fieldType = fieldTypeStr,
        isRequired = this.isRequired,
        orderIndex = this.orderIndex,
        optionsJson = optionsJsonStr,
        createdAt = System.currentTimeMillis(),
        defaultValueJson = defaultValJson,
        isUnique = this.isUnique,
        validationRegex = this.validationRegex
    )
}

/**
 * Převádí databázovou entitu FieldDefinitionEntity na doménový model FieldDefinition.
 */
fun FieldDefinitionEntity.toDomain(): FieldDefinition {
    val type: FieldType = when (this.fieldType) {
        "TEXT" -> FieldType.TextType
        "NUMBER" -> FieldType.NumberType
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
        "IMAGE" -> FieldType.ImageType
        "URL" -> FieldType.UrlType
        "RATING" -> FieldType.RatingType
        "BARCODE" -> FieldType.BarcodeType
        "LOCATION" -> FieldType.LocationType
        else -> FieldType.TextType // Fallback jako spolehlivý default
    }

    var defaultVal: Any? = null
    if (!this.defaultValueJson.isNullOrEmpty()) {
        try {
            val typeToken = object : TypeToken<Any?>() {}.type
            defaultVal = gson.fromJson(this.defaultValueJson, typeToken)
        } catch (e: Exception) {}
    }

    return FieldDefinition(
        id = this.id,
        collectionId = this.collectionId,
        fieldName = this.fieldName,
        fieldLabel = this.fieldLabel,
        fieldType = type,
        isRequired = this.isRequired,
        orderIndex = this.orderIndex,
        isUnique = this.isUnique,
        validationRegex = this.validationRegex,
        defaultValue = defaultVal
    )
}
