package com.saxonthune.ranktheplanet.data.sql

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saxonthune.ranktheplanet.data.TemplateRepository
import com.saxonthune.ranktheplanet.data.op.Op
import com.saxonthune.ranktheplanet.data.op.OpLogWriter
import com.saxonthune.ranktheplanet.data.op.opJson
import com.saxonthune.ranktheplanet.db.AppDatabase
import com.saxonthune.ranktheplanet.domain.CollectionId
import com.saxonthune.ranktheplanet.domain.FieldType
import com.saxonthune.ranktheplanet.domain.ReviewTemplate
import com.saxonthune.ranktheplanet.domain.TemplateField
import com.saxonthune.ranktheplanet.domain.TemplateFieldConfig
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.saxonthune.ranktheplanet.data.op.nowIso
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class SqlTemplateRepository(
    private val database: AppDatabase,
    private val opLogWriter: OpLogWriter,
) : TemplateRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(collectionId: CollectionId): Flow<ReviewTemplate?> =
        database.collectionQueries.observeById(collectionId.value)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .flatMapLatest { collection ->
                if (collection == null) flowOf(null)
                else database.templateFieldQueries
                    .selectByCollectionAndVersion(collection.id, collection.template_version)
                    .asFlow()
                    .mapToList(Dispatchers.Default)
                    .map { rows ->
                        if (rows.isEmpty()) null
                        else ReviewTemplate(
                            collectionId = collectionId,
                            version = collection.template_version.toInt(),
                            fields = rows.map { it.toDomain() }.toImmutableList(),
                        )
                    }
            }

    override suspend fun define(
        collectionId: CollectionId,
        fields: List<TemplateField>,
    ): Result<ReviewTemplate> = withContext(Dispatchers.Default) {
        runCatching {
            database.transactionWithResult {
                database.collectionQueries.observeById(collectionId.value)
                    .executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Collection not found: ${collectionId.value}")

                val existingFields = database.templateFieldQueries
                    .selectByCollectionAndVersion(collectionId.value, 1L)
                    .executeAsList()
                if (existingFields.isNotEmpty()) {
                    throw IllegalStateException("Template at version 1 already exists for: ${collectionId.value}")
                }

                val now = nowIso()
                fields.forEachIndexed { idx, field ->
                    database.templateFieldQueries.insertField(
                        collection_id = collectionId.value,
                        version = 1L,
                        ordinal = idx.toLong(),
                        name = field.name,
                        label = field.label,
                        type = field.type.name.lowercase(),
                        config = field.config.toJson(),
                    )
                }
                database.collectionQueries.bumpLastModifiedAndTemplateVersion(now, 1L, collectionId.value)
                val immFields = fields.mapIndexed { i, f -> f.copy(ordinal = i) }.toImmutableList()
                opLogWriter.append(Op.TemplateDefined(
                    collectionId = collectionId.value,
                    version = 1,
                    fieldsJson = opJson.encodeToString(immFields.map { it.name }),
                ))
                ReviewTemplate(collectionId = collectionId, version = 1, fields = immFields)
            }
        }
    }

    override suspend fun edit(
        collectionId: CollectionId,
        fields: List<TemplateField>,
    ): Result<ReviewTemplate> = withContext(Dispatchers.Default) {
        runCatching {
            database.transactionWithResult {
                val collection = database.collectionQueries.observeById(collectionId.value)
                    .executeAsOneOrNull()
                    ?: throw IllegalArgumentException("Collection not found: ${collectionId.value}")

                val newVersion = collection.template_version + 1L
                val now = nowIso()
                fields.forEachIndexed { idx, field ->
                    database.templateFieldQueries.insertField(
                        collection_id = collectionId.value,
                        version = newVersion,
                        ordinal = idx.toLong(),
                        name = field.name,
                        label = field.label,
                        type = field.type.name.lowercase(),
                        config = field.config.toJson(),
                    )
                }
                database.collectionQueries.bumpLastModifiedAndTemplateVersion(now, newVersion, collectionId.value)
                val immFields = fields.mapIndexed { i, f -> f.copy(ordinal = i) }.toImmutableList()
                opLogWriter.append(Op.TemplateEdited(
                    collectionId = collectionId.value,
                    newVersion = newVersion.toInt(),
                    fieldsJson = opJson.encodeToString(immFields.map { it.name }),
                ))
                ReviewTemplate(collectionId = collectionId, version = newVersion.toInt(), fields = immFields)
            }
        }
    }
}

private fun com.saxonthune.ranktheplanet.db.Template_field.toDomain(): TemplateField = TemplateField(
    name = name,
    label = label,
    type = fieldTypeFromString(type),
    config = parseTemplateFieldConfig(config),
    ordinal = ordinal.toInt(),
)

private fun fieldTypeFromString(s: String): FieldType = when (s.lowercase()) {
    "score" -> FieldType.Score
    "text" -> FieldType.Text
    "enum" -> FieldType.Enum
    "boolean" -> FieldType.Boolean
    "date" -> FieldType.Date
    else -> FieldType.Text
}

private fun TemplateFieldConfig?.toJson(): String? = when (this) {
    null -> null
    is TemplateFieldConfig.Score -> """{"kind":"score","min":$min,"max":$max,"step":$step,"render":"$render"}"""
    TemplateFieldConfig.TextField -> """{"kind":"text"}"""
    is TemplateFieldConfig.Enum -> """{"kind":"enum","options":${options.joinToString(",", "[", "]") { "\"$it\"" }}}"""
    TemplateFieldConfig.BooleanField -> """{"kind":"boolean"}"""
    TemplateFieldConfig.Date -> """{"kind":"date"}"""
}

private fun parseTemplateFieldConfig(json: String?): TemplateFieldConfig? {
    if (json == null) return null
    val obj = opJson.parseToJsonElement(json).jsonObject
    return when (obj["kind"]?.jsonPrimitive?.content) {
        "score" -> TemplateFieldConfig.Score(
            min = obj["min"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            max = obj["max"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 5.0,
            step = obj["step"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.5,
            render = obj["render"]?.jsonPrimitive?.content ?: "stars",
        )
        "text" -> TemplateFieldConfig.TextField
        "enum" -> TemplateFieldConfig.Enum(
            options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content }?.toImmutableList()
                ?: persistentListOf(),
        )
        "boolean" -> TemplateFieldConfig.BooleanField
        "date" -> TemplateFieldConfig.Date
        else -> null
    }
}
