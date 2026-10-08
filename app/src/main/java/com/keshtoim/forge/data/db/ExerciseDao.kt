package com.keshtoim.forge.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE archived = 0")
    fun observeActive(): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(exercises: List<Exercise>)

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)
}
