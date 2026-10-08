package com.keshtoim.forge.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class TemplateWithExercises(
    @Embedded val template: Template,
    @Relation(parentColumn = "id", entityColumn = "templateId")
    val items: List<TemplateExercise>,
)

@Dao
interface TemplateDao {
    @Transaction
    @Query("SELECT * FROM templates ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun get(id: Long): TemplateWithExercises?

    @Upsert
    suspend fun upsert(template: Template): Long

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteItems(templateId: Long)

    @Insert
    suspend fun insertItems(items: List<TemplateExercise>)

    @Delete
    suspend fun delete(template: Template)

    @Transaction
    suspend fun save(template: Template, items: List<TemplateExercise>): Long {
        // Upsert returns -1 when it updated an existing row.
        val id = upsert(template).takeIf { it != -1L } ?: template.id
        deleteItems(id)
        insertItems(items.mapIndexed { i, item -> item.copy(id = 0, templateId = id, position = i) })
        return id
    }
}
