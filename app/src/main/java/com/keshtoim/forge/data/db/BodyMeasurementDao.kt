package com.keshtoim.forge.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {
    @Query("SELECT * FROM body_measurements ORDER BY epochDay")
    fun observeAll(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements WHERE epochDay = :epochDay")
    suspend fun getDay(epochDay: Long): BodyMeasurement?

    @Upsert
    suspend fun upsert(measurement: BodyMeasurement)

    @Delete
    suspend fun delete(measurement: BodyMeasurement)

    // A weigh-in and a tape measurement taken the same day end up in one row.
    @Transaction
    suspend fun record(epochDay: Long, update: (BodyMeasurement) -> BodyMeasurement) {
        upsert(update(getDay(epochDay) ?: BodyMeasurement(epochDay = epochDay)))
    }
}
