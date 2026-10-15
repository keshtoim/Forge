package com.keshtoim.forge.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weights ORDER BY epochDay")
    fun observeAll(): Flow<List<BodyWeight>>

    @Query("DELETE FROM body_weights WHERE epochDay = :epochDay")
    suspend fun deleteDay(epochDay: Long)

    @Insert
    suspend fun insert(weight: BodyWeight)

    @Delete
    suspend fun delete(weight: BodyWeight)

    @Transaction
    suspend fun record(epochDay: Long, weightKg: Double) {
        deleteDay(epochDay)
        insert(BodyWeight(epochDay = epochDay, weightKg = weightKg))
    }
}
