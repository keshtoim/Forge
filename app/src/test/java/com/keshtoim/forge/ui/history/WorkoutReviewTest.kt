package com.keshtoim.forge.ui.history

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.ai.AiConnection
import com.keshtoim.forge.data.ai.AiProvider
import com.keshtoim.forge.data.ai.AiSettings
import com.keshtoim.forge.data.ai.SecretCipher
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.ForgeDatabase
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.StartItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WorkoutReviewTest {
    private val app = ApplicationProvider.getApplicationContext<ForgeApplication>()
    private val server = MockWebServer().apply { start() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val db = Room.inMemoryDatabaseBuilder(app, ForgeDatabase::class.java).build()

    @After
    fun tearDown() {
        scope.cancel()
        server.shutdown()
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun finishedWorkout(exercise: Long, weight: Double, at: Long): Long {
        val id = db.workoutDao().start("Push", null, listOf(StartItem(exercise, 1)), now = at)
        db.workoutDao().observeDetail(id).first()!!.exercises.single().sets.forEach { db.workoutDao().update(it.copy(weightKg = weight, reps = 8, completed = true)) }
        db.workoutDao().finish(id, at + 3_600_000)
        return id
    }

    @Test
    fun `a review compares with the workout before and is saved with it`() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val connectivity = app.getSystemService(ConnectivityManager::class.java)
        shadowOf(connectivity).setNetworkCapabilities(
            connectivity.activeNetwork,
            ShadowNetworkCapabilities.newInstance().also { shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) },
        )
        val settings = AiSettings(
            app,
            object : SecretCipher {
                override fun encrypt(plain: String) = plain
                override fun decrypt(stored: String) = stored
            },
        ).apply {
            setProvider(AiProvider.OPENAI)
            setKey(AiProvider.OPENAI, "sk-test")
            setEnabled(true)
        }
        val bench = db.exerciseDao().insert(Exercise(builtInKey = "BENCH_PRESS", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS))
        finishedWorkout(bench, 95.0, at = 1_000_000)
        val today = finishedWorkout(bench, 100.0, at = 900_000_000)
        val viewModel = WorkoutDetailViewModel(
            SavedStateHandle(mapOf("id" to today)), db.workoutDao(), db.exerciseDao(),
            AiConnection(app, settings, scope, server.url("/").toString()), app.preferences, db.bodyMeasurementDao(),
        )
        // The screen keeps these collected; the review reads their current values.
        val watching = listOf(launch(Dispatchers.Default) { viewModel.detail.collect {} }, launch(Dispatchers.Default) { viewModel.exercises.collect {} })
        withTimeout(5_000) { viewModel.detail.first { it != null } }
        server.enqueue(
            MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(
                """{"id":"c1","object":"chat.completion","created":1,"model":"gpt-5","choices":[{"index":0,"message":{"role":"assistant","content":"Жим вырос на 5 кг.","refusal":null},"finish_reason":"stop","logprobs":null}]}""",
            ),
        )

        viewModel.review(mapOf(bench to "Жим лёжа"))
        val saved = withTimeout(10_000) { db.workoutDao().observeDetail(today).first { it?.workout?.review != null } }!!

        assertEquals("Жим вырос на 5 кг.", saved.workout.review)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("last time: 95 kg x 8, done"))
        assertFalse(body.contains("last time: 100 kg"))
        watching.forEach { it.cancel() }
    }
}
