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
