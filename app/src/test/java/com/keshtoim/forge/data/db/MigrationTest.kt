package com.keshtoim.forge.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // The driver-based helper; the SupportSQLite one rejects the absolute path it builds itself.
    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = context.getDatabasePath(DB),
        driver = AndroidSQLiteDriver(),
        databaseClass = ForgeDatabase::class,
    )

    @Test
    fun `migration 1 to 2 keeps exercises and gives them no secondary muscles`() = runBlocking {
        helper.createDatabase(1).use {
            it.execSQL(
                "INSERT INTO exercises (id, builtInKey, name, muscleGroup, type, archived) " +
                    "VALUES (1, NULL, 'Shrug', 'BACK', 'WEIGHT_REPS', 0)",
            )
        }
        helper.runMigrationsAndValidate(2).close()

        val db = Room.databaseBuilder(context, ForgeDatabase::class.java, DB).build()
        val exercise = db.exerciseDao().observeAll().first().single()
        assertEquals("Shrug", exercise.name)
        assertEquals(emptyList<MuscleGroup>(), exercise.secondaryMuscles)
        db.close()
    }

    @Test
    fun `migration 2 to 3 gives seeded barbell exercises a bar and leaves the rest on total weight`() = runBlocking {
        helper.createDatabase(2).use {
            it.execSQL(
                "INSERT INTO exercises (id, builtInKey, name, muscleGroup, secondaryMuscles, type, archived) VALUES " +
                    "(1, 'SQUAT', NULL, 'QUADS', '', 'WEIGHT_REPS', 0), " +
                    "(2, 'SKULL_CRUSHER', NULL, 'TRICEPS', '', 'WEIGHT_REPS', 0), " +
                    "(3, 'DUMBBELL_CURL', NULL, 'BICEPS', '', 'WEIGHT_REPS', 0), " +
                    "(4, NULL, 'Landmine', 'SHOULDERS', '', 'WEIGHT_REPS', 0)",
            )
        }
        helper.runMigrationsAndValidate(3).close()

        val db = Room.databaseBuilder(context, ForgeDatabase::class.java, DB).build()
        val bars = db.exerciseDao().observeAll().first().associate { it.id to it.barKg }
        assertEquals(mapOf(1L to 20.0, 2L to 10.0, 3L to null, 4L to null), bars)
        db.close()
    }

    @Test
    fun `migration 6 to 7 carries weigh-ins over into body measurements`() = runBlocking {
        helper.createDatabase(6).use {
            it.execSQL("INSERT INTO body_weights (id, epochDay, weightKg) VALUES (1, 20000, 80.5), (2, 20007, 79.8)")
        }
        helper.runMigrationsAndValidate(7).close()

        val db = Room.databaseBuilder(context, ForgeDatabase::class.java, DB).build()
        val entries = db.bodyMeasurementDao().observeAll().first()
        assertEquals(listOf(20000L to 80.5, 20007L to 79.8), entries.map { it.epochDay to it.weightKg })
        assertEquals(null, entries.first().waistCm)
        db.close()
    }

    @Test
    fun `secondary muscles survive a round trip`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, ForgeDatabase::class.java).build()
        val muscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)
        db.exerciseDao().insert(Exercise(name = "Dip", muscleGroup = MuscleGroup.CHEST, secondaryMuscles = muscles, type = ExerciseType.REPS))

        assertEquals(muscles, db.exerciseDao().observeAll().first().single().secondaryMuscles)
        db.close()
    }

    private companion object {
        const val DB = "migration-test"
    }
}
