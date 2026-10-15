package com.keshtoim.forge.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.keshtoim.forge.data.BuiltInExercise

@Database(
    entities = [Exercise::class, Template::class, TemplateExercise::class, Workout::class, WorkoutExercise::class, WorkoutSet::class, PlannedWorkout::class],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = ForgeDatabase.BarWeightMigration::class),
        AutoMigration(from = 3, to = 4),
    ],
)
@TypeConverters(Converters::class)
abstract class ForgeDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateDao(): TemplateDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun backupDao(): BackupDao
    abstract fun planDao(): PlanDao

    // Existing installs already have the built-ins seeded, so barbell ones get their default bar here.
    class BarWeightMigration : AutoMigrationSpec {
        override fun onPostMigrate(connection: SQLiteConnection) {
            BuiltInExercise.entries.forEach { builtIn ->
                builtIn.barKg?.let { connection.execSQL("UPDATE exercises SET barKg = $it WHERE builtInKey = '${builtIn.name}'") }
            }
        }
    }

    companion object {
        fun build(context: Context): ForgeDatabase =
            Room.databaseBuilder(context, ForgeDatabase::class.java, "forge.db").build()
    }
}
