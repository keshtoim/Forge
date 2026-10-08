package com.keshtoim.forge.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Exercise::class, Template::class, TemplateExercise::class, Workout::class, WorkoutExercise::class, WorkoutSet::class],
    version = 1,
)
abstract class ForgeDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        fun build(context: Context): ForgeDatabase =
            Room.databaseBuilder(context, ForgeDatabase::class.java, "forge.db").build()
    }
}
