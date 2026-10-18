package com.keshtoim.forge.ui.plan

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.ForgeDatabase
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.Template
import com.keshtoim.forge.data.db.TemplateExercise
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PlanViewModelTest {
    private val app = ApplicationProvider.getApplicationContext<ForgeApplication>()
    private lateinit var db: ForgeDatabase
    private var bench = 0L
    private var squat = 0L

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        db = Room.inMemoryDatabaseBuilder(app, ForgeDatabase::class.java).build()
        bench = db.exerciseDao().insert(Exercise(builtInKey = "BENCH_PRESS", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS))
        squat = db.exerciseDao().insert(Exercise(builtInKey = "SQUAT", muscleGroup = MuscleGroup.QUADS, type = ExerciseType.WEIGHT_REPS))
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(planId: Long = 0) =
        PlanViewModel(SavedStateHandle(mapOf("id" to planId, "epochDay" to 20_000L)), db.planDao(), db.templateDao(), db.exerciseDao(), app.planReminders)

    // ViewModel writes go through Room's own executor, so poll the DB instead of advancing a test dispatcher.
    private fun <T : Any> await(read: suspend () -> T?): T = runBlocking {
        withTimeout(5_000) {
            var value = read()
            while (value == null) {
                delay(20)
                value = read()
            }
            value
        }
    }

    @Test
    fun `own exercises are kept in a hidden template that goes when the plan switches to a template`() {
        val vm = viewModel()
        vm.own = true
        vm.addExercises(longArrayOf(bench, squat))
        var saved = false
        vm.save("Своя тренировка") { saved = true }
        val plan = await { db.planDao().getAll().singleOrNull()?.takeIf { saved } }
        val own = runBlocking { db.templateDao().get(plan.templateId)!! }
        assertTrue(own.template.hidden)
        assertEquals("Своя тренировка", own.template.name)
        assertEquals(listOf(bench, squat), own.items.sortedBy { it.position }.map { it.exerciseId })

        val push = runBlocking { db.templateDao().save(Template(name = "Push"), listOf(TemplateExercise(templateId = 0, exerciseId = bench, position = 0, targetSets = 3))) }
        val edit = viewModel(plan.id)
        await { edit.items.takeIf { it.size == 2 } }
        assertNull(edit.templateId)
        edit.own = false
        edit.templateId = push
        edit.save("") {}
        await { runBlocking { db.templateDao().get(own.template.id) }.let { if (it == null) Unit else null } }
        assertEquals(push, runBlocking { db.planDao().get(plan.id)!!.templateId })
    }

    @Test
    fun `deleting a plan removes its own exercise list`() {
        val vm = viewModel()
        vm.own = true
        vm.addExercises(longArrayOf(bench))
        vm.save("Своя тренировка") {}
        val plan = await { db.planDao().getAll().singleOrNull() }

        val edit = viewModel(plan.id)
        await { edit.items.takeIf { it.isNotEmpty() } }
        var deleted = false
        edit.delete { deleted = true }
        await { deleted.takeIf { it } }
        assertEquals(emptyList<Template>(), runBlocking { db.templateDao().observeAll().first() }.map { it.template })
    }
}
