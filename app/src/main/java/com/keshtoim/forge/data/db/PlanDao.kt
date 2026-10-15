package com.keshtoim.forge.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM planned_workouts")
    fun observeAll(): Flow<List<PlannedWorkout>>

    @Query("SELECT * FROM planned_workouts WHERE id = :id")
    suspend fun get(id: Long): PlannedWorkout?

    @Insert
    suspend fun insert(plan: PlannedWorkout): Long

    @Update
    suspend fun update(plan: PlannedWorkout)

    @Delete
    suspend fun delete(plan: PlannedWorkout)

    suspend fun save(plan: PlannedWorkout): Long = if (plan.id == 0L) insert(plan) else plan.id.also { update(plan) }
}
