package com.keshtoim.forge.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RenameTable
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.keshtoim.forge.data.BuiltInExercise

@Database(
    entities = [Exercise::class, Template::class, TemplateExercise::class, Workout::class, WorkoutExercise::class, WorkoutSet::class, PlannedWorkout::class, BodyMeasurement::class],
    version = 8,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = ForgeDatabase.BarWeightMigration::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7, spec = ForgeDatabase.MeasurementsMigration::class),
        AutoMigration(from = 7, to = 8),
    ],
)
@TypeConverters(Converters::class)
abstract class ForgeDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateDao(): TemplateDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun backupDao(): BackupDao
    abstract fun planDao(): PlanDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao

    // Existing installs already have the built-ins seeded, so barbell ones get their default bar here.
    class BarWeightMigration : AutoMigrationSpec {
        override fun onPostMigrate(connection: SQLiteConnection) {
            BuiltInExercise.entries.forEach { builtIn ->
                builtIn.barKg?.let { connection.execSQL("UPDATE exercises SET barKg = $it WHERE builtInKey = '${builtIn.name}'") }
            }
        }
    }

    // The weight log grew into a log of all body measurements; existing weigh-ins carry over.
    @RenameTable(fromTableName = "body_weights", toTableName = "body_measurements")
    class MeasurementsMigration : AutoMigrationSpec

    companion object {
        fun build(context: Context): ForgeDatabase =
            Room.databaseBuilder(context, ForgeDatabase::class.java, "forge.db").build()
    }
}
