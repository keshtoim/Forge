package com.keshtoim.forge.ui.templates

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.ai.AiConnection
import com.keshtoim.forge.data.ai.AiProvider
import com.keshtoim.forge.data.ai.AiSettings
import com.keshtoim.forge.data.ai.SecretCipher
import com.keshtoim.forge.data.db.Equipment
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.ForgeDatabase
import com.keshtoim.forge.data.db.MuscleGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
class ProgramViewModelTest {
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

    @Test
    fun `a program is built from the catalog and saved as templates`() = runBlocking {
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
        val press = db.exerciseDao().insert(Exercise(builtInKey = "DUMBBELL_BENCH_PRESS", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS))
        val row = db.exerciseDao().insert(Exercise(builtInKey = "DUMBBELL_ROW", muscleGroup = MuscleGroup.BACK, type = ExerciseType.WEIGHT_REPS))
        val squat = db.exerciseDao().insert(Exercise(builtInKey = "SQUAT", muscleGroup = MuscleGroup.QUADS, type = ExerciseType.WEIGHT_REPS))
        val viewModel = ProgramViewModel(AiConnection(app, settings, scope, server.url("/").toString()), db.exerciseDao(), db.templateDao(), app.preferences, db.bodyMeasurementDao())
        val watching = launch(Dispatchers.Default) { viewModel.exercises.collect {} }
        withTimeout(5_000) { viewModel.exercises.first { it.size == 3 } }
        viewModel.equipment = setOf(Equipment.DUMBBELL)
        val program = """{"name":"Гантели","notes":"Прибавляй по 1 кг.","workouts":[{"name":"День A","exercises":[{"id":$press,"sets":4,"superset":1},{"id":$row,"sets":3,"superset":1}]}]}"""
        server.enqueue(
            MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(
                """{"id":"c1","object":"chat.completion","created":1,"model":"gpt-5","choices":[{"index":0,"message":{"role":"assistant","content":${org.json.JSONObject.quote(program)},"refusal":null},"finish_reason":"stop","logprobs":null}]}""",
            ),
        )

        viewModel.generate(mapOf(press to "Жим гантелей лёжа", row to "Тяга гантели в наклоне", squat to "Приседания со штангой"))
        withTimeout(10_000) { while (viewModel.draft == null && viewModel.error == null) delay(20) }

        val body = server.takeRequest().body.readUtf8()
        // Only the equipment the athlete has goes into the catalog.
        assertTrue(body.contains("Жим гантелей лёжа") && body.contains("Тяга гантели в наклоне"))
        assertFalse(body.contains("Приседания со штангой"))

        var saved = false
        viewModel.save { saved = true }
        withTimeout(5_000) { while (!saved) delay(20) }
        val template = db.templateDao().observeAll().first().single()
        assertEquals("Гантели · День A", template.template.name)
        assertEquals(listOf(4, 4), template.items.sortedBy { it.position }.map { it.targetSets })
        assertEquals(listOf(1, 1), template.items.map { it.superset })
        watching.cancel()
    }
}
