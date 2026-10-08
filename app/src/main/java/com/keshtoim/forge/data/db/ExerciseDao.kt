package com.keshtoim.forge.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.keshtoim.forge.data.BuiltInExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE archived = 0")
    fun observeActive(): Flow<List<Exercise>>

    // Includes archived ones: templates and history may still reference them.
    @Query("SELECT * FROM exercises")
    fun observeAll(): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(exercises: List<Exercise>)

    // Idempotent: the unique builtInKey makes this add only exercises missing from the table.
    suspend fun seedBuiltIns() = insertIgnore(BuiltInExercise.entries.map { it.toEntity() })

    @Query("SELECT EXISTS(SELECT 1 FROM workout_exercises WHERE exerciseId = :exerciseId)")
    suspend fun isUsedInWorkouts(exerciseId: Long): Boolean

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)
}
