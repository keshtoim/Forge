package com.keshtoim.forge.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: ForgeDatabase
    private val workouts get() = db.workoutDao()
    private val templates get() = db.templateDao()
    private var bench = 0L
    private var squat = 0L

    private fun inMemoryDb() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), ForgeDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @Before
    fun setUp() = runTest {
        db = inMemoryDb()
        bench = db.exerciseDao().insert(Exercise(builtInKey = "BENCH_PRESS", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS))
        squat = db.exerciseDao().insert(Exercise(builtInKey = "SQUAT", muscleGroup = MuscleGroup.QUADS, type = ExerciseType.WEIGHT_REPS))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun detail(id: Long) = workouts.observeDetail(id).first()!!

    private suspend fun completeAll(workoutId: Long, weight: Double, reps: Int) {
        detail(workoutId).exercises.flatMap { it.sets }.forEach {
            workouts.update(it.copy(weightKg = weight, reps = reps, completed = true))
        }
    }

    @Test
    fun `start creates exercises with the requested number of empty sets`() = runTest {
        val id = workouts.start("Push", null, listOf(bench to 3, squat to 2), now = 1_000)

        val d = detail(id)
        assertEquals(id, workouts.getActive()?.id)
        assertEquals(listOf(bench, squat), d.exercises.sortedBy { it.workoutExercise.position }.map { it.workoutExercise.exerciseId })
        assertEquals(listOf(3, 2), d.exercises.sortedBy { it.workoutExercise.position }.map { it.sets.size })
        assertTrue(d.exercises.flatMap { it.sets }.none { it.completed })
    }

    @Test
    fun `addExercises appends after existing ones with one set each`() = runTest {
        val id = workouts.start("Push", null, listOf(bench to 1), now = 1_000)
        workouts.addExercises(id, listOf(squat))

        val added = detail(id).exercises.single { it.workoutExercise.exerciseId == squat }
        assertEquals(1, added.workoutExercise.position)
        assertEquals(1, added.sets.size)
    }

    @Test
    fun `finish drops incomplete sets and empty exercises`() = runTest {
        val id = workouts.start("Push", null, listOf(bench to 2, squat to 1), now = 1_000)
        val benchSet = detail(id).exercises.single { it.workoutExercise.exerciseId == bench }.sets.first()
        workouts.update(benchSet.copy(weightKg = 100.0, reps = 5, completed = true))

        assertTrue(workouts.finish(id, now = 2_000))

        val d = detail(id)
        assertEquals(2_000L, d.workout.finishedAt)
        assertEquals(listOf(bench), d.exercises.map { it.workoutExercise.exerciseId })
        assertEquals(1, d.exercises.single().sets.size)
        assertNull(workouts.getActive())
    }

    @Test
    fun `finish without completed sets deletes the workout`() = runTest {
        val id = workouts.start("Push", null, listOf(bench to 2), now = 1_000)

        assertFalse(workouts.finish(id, now = 2_000))
        assertNull(workouts.observeDetail(id).first())
    }

    @Test
    fun `previousSets returns the latest finished workout and ignores the active one`() = runTest {
        val old = workouts.start("A", null, listOf(bench to 1), now = 1_000)
        completeAll(old, weight = 80.0, reps = 5)
        workouts.finish(old, now = 1_500)

        val recent = workouts.start("B", null, listOf(bench to 2), now = 2_000)
        completeAll(recent, weight = 90.0, reps = 5)
        workouts.finish(recent, now = 2_500)

        val active = workouts.start("C", null, listOf(bench to 1), now = 3_000)
        completeAll(active, weight = 200.0, reps = 1)

        val previous = workouts.previousSets(bench)
        assertEquals(listOf(90.0, 90.0), previous.map { it.weightKg })
        assertTrue(workouts.previousSets(squat).isEmpty())
    }

    @Test
    fun `deleting a workout cascades to its exercises and sets`() = runTest {
        val id = workouts.start("Push", null, listOf(bench to 3), now = 1_000)
        workouts.deleteWorkout(id)

        assertTrue(db.backupDao().workoutExercises().isEmpty())
        assertTrue(db.backupDao().workoutSets().isEmpty())
    }

    @Test
    fun `template save inserts new and replaces items of existing template`() = runTest {
        val id = templates.save(Template(name = "Push"), listOf(item(bench, 3), item(squat, 4)))
        val created = templates.get(id)!!
        assertEquals(listOf(bench to 3, squat to 4), created.items.sortedBy { it.position }.map { it.exerciseId to it.targetSets })

        val sameId = templates.save(created.template.copy(name = "Push day"), listOf(item(squat, 5)))

        assertEquals(id, sameId)
        val updated = templates.get(id)!!
        assertEquals("Push day", updated.template.name)
        assertEquals(listOf(squat to 5), updated.items.map { it.exerciseId to it.targetSets })
        assertEquals(1, templates.observeAll().first().size)
    }

    @Test
    fun `deleting a template keeps workouts started from it`() = runTest {
        val templateId = templates.save(Template(name = "Push"), listOf(item(bench, 1)))
        val workoutId = workouts.start("Push", templateId, listOf(bench to 1), now = 1_000)

        templates.delete(templates.get(templateId)!!.template)

        val workout = detail(workoutId).workout
        assertNull(workout.templateId)
    }

    @Test
    fun `backup survives json round trip into an empty database`() = runTest {
        templates.save(Template(name = "Push"), listOf(item(bench, 3)))
        val id = workouts.start("Push", null, listOf(bench to 2), now = 1_000)
        completeAll(id, weight = 100.0, reps = 5)
        workouts.finish(id, now = 2_000)

        val json = Json.encodeToString(Backup.serializer(), db.backupDao().export())
        val target = inMemoryDb()
        target.backupDao().replaceAll(Json.decodeFromString(Backup.serializer(), json))

        assertEquals(db.backupDao().export(), target.backupDao().export())
        val restored = target.workoutDao().observeDetail(id).first()
        assertNotNull(restored)
        assertEquals(2, restored!!.exercises.single().sets.count { it.completed })
        target.close()
    }

    @Test
    fun `import replaces existing data`() = runTest {
        val backup = db.backupDao().export()
        workouts.start("Will be gone", null, listOf(bench to 1), now = 1_000)

        db.backupDao().replaceAll(backup)

        assertNull(workouts.getActive())
        assertEquals(2, db.backupDao().exercises().size)
    }

    private fun item(exerciseId: Long, sets: Int) =
        TemplateExercise(templateId = 0, exerciseId = exerciseId, position = 0, targetSets = sets)
}
