package com.keshtoim.forge.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMuscles(muscles: List<MuscleGroup>): String = muscles.joinToString(",") { it.name }

    @TypeConverter
    fun toMuscles(value: String): List<MuscleGroup> =
        if (value.isEmpty()) emptyList() else value.split(",").map(MuscleGroup::valueOf)
}
