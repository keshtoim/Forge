package com.keshtoim.forge.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Exercise::class, Template::class, TemplateExercise::class, Workout::class, WorkoutExercise::class, WorkoutSet::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class ForgeDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateDao(): TemplateDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun backupDao(): BackupDao

    companion object {
        fun build(context: Context): ForgeDatabase =
            Room.databaseBuilder(context, ForgeDatabase::class.java, "forge.db").build()
    }
}
