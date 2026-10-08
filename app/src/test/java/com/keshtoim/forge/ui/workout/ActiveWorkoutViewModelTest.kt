package com.keshtoim.forge.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.ForgeDatabase
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.WorkoutSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ActiveWorkoutViewModelTest {
    private val app = ApplicationProvider.getApplicationContext<ForgeApplication>()
    private lateinit var db: ForgeDatabase
    private lateinit var viewModel: ActiveWorkoutViewModel
    private var workoutId = 0L
    private var setId = 0L
    private var workoutExerciseId = 0L

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        db = Room.inMemoryDatabaseBuilder(app, ForgeDatabase::class.java).build()
        val bench = db.exerciseDao().insert(Exercise(builtInKey = "BENCH_PRESS", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS))
        workoutId = db.workoutDao().start("Push", null, listOf(bench to 1), now = 0)
        val exercise = db.workoutDao().observeDetail(workoutId).first()!!.exercises.single()
        workoutExerciseId = exercise.workoutExercise.id
        setId = exercise.sets.single().id
        viewModel = ActiveWorkoutViewModel(SavedStateHandle(mapOf("id" to workoutId)), db.workoutDao(), db.exerciseDao(), app.restTimer)
    }

    @After
    fun tearDown() {
        app.restTimer.stop()
        db.close()
        Dispatchers.resetMain()
    }

    // ViewModel writes go through Room's own executor, so poll the DB instead of advancing a test dispatcher.
    private fun <T : Any> await(read: suspend () -> T?, condition: (T) -> Boolean): T = runBlocking {
        withTimeout(5_000) {
            var value = read()
            while (value == null || !condition(value)) {
                delay(10)
                value = read()
            }
            value
        }
    }

    private fun awaitSet(condition: (WorkoutSet) -> Boolean) = await({ db.workoutDao().getSet(setId) }, condition)

    @Test
    fun `typed value is not overwritten by ghost when set is completed right away`() {
        val ghost = WorkoutSet(workoutExerciseId = 0, position = 0, weightKg = 60.0, reps = 8)

        viewModel.editSet(setId) { SetField.REPS.write(it, "10") }
        viewModel.toggleCompleted(setId, ghost)

        val set = awaitSet { it.completed }
        assertEquals(10, set.reps)
        assertEquals(60.0, set.weightKg!!, 0.0)
    }

    @Test
    fun `quick successive steps are all applied`() {
        repeat(3) { viewModel.editSet(setId) { SetField.WEIGHT.step(it, null, 1) } }

        assertEquals(7.5, awaitSet { it.weightKg == 7.5 }.weightKg!!, 0.0)
    }

    @Test
    fun `editing one field keeps a value just written to another`() {
        viewModel.editSet(setId) { SetField.WEIGHT.write(it, "100") }
        viewModel.editSet(setId) { SetField.REPS.write(it, "5") }

        val set = awaitSet { it.reps == 5 }
        assertEquals(100.0, set.weightKg!!, 0.0)
    }

    @Test
    fun `completing an empty set without ghost is ignored`() {
        viewModel.toggleCompleted(setId, ghost = null)
        viewModel.editSet(setId) { SetField.WEIGHT.write(it, "1") }

        val set = awaitSet { it.weightKg == 1.0 }
        assertTrue(!set.completed)
    }

    @Test
    fun `new set copies values of the last one`() {
        viewModel.editSet(setId) { SetField.WEIGHT.write(it, "80") }
        viewModel.addSet(workoutExerciseId)

        val sets = await({ db.workoutDao().observeDetail(workoutId).first()?.exercises?.single()?.sets }) { it.size == 2 }
        val added = sets.maxBy { it.position }
        assertEquals(80.0, added.weightKg!!, 0.0)
        assertEquals(1, added.position)
    }
}
