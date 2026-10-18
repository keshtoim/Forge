package com.keshtoim.forge.ui.workout

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.ai.AiConnection
import com.keshtoim.forge.data.ai.AiFailure
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CoachChatTest {
    private val app = ApplicationProvider.getApplicationContext<ForgeApplication>()
    private val server = MockWebServer().apply { start() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var db: ForgeDatabase
    private lateinit var viewModel: ActiveWorkoutViewModel
    private var bench = 0L

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val connectivity = app.getSystemService(ConnectivityManager::class.java)
        val capabilities = ShadowNetworkCapabilities.newInstance().also { shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
        shadowOf(connectivity).setNetworkCapabilities(connectivity.activeNetwork, capabilities)

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
        db = Room.inMemoryDatabaseBuilder(app, ForgeDatabase::class.java).build()
        bench = db.exerciseDao().insert(Exercise(builtInKey = "BENCH_PRESS", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS))
        val workoutId = db.workoutDao().start("Push", null, listOf(StartItem(bench, 2)), now = System.currentTimeMillis())
        viewModel = ActiveWorkoutViewModel(
            SavedStateHandle(mapOf("id" to workoutId)), db.workoutDao(), db.exerciseDao(), app.restTimer,
            AiConnection(app, settings, scope, server.url("/").toString()), app.preferences, db.bodyMeasurementDao(),
        )
        withTimeout(5_000) { viewModel.detail.first { it != null } }
        Unit
    }

    @After
    fun tearDown() {
        scope.cancel()
        server.shutdown()
        db.close()
        Dispatchers.resetMain()
    }

    private fun answer(code: Int, text: String = "") = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(
        if (code == 200) {
            """{"id":"c1","object":"chat.completion","created":1,"model":"gpt-5","choices":[{"index":0,"message":{"role":"assistant","content":"$text","refusal":null},"finish_reason":"stop","logprobs":null}]}"""
        } else {
            """{"error":{"message":"Incorrect API key provided","type":"invalid_request_error","code":"invalid_api_key"}}"""
        },
    )

    private fun awaitIdle() = runBlocking {
        withTimeout(10_000) { while (viewModel.coachBusy) delay(20) }
    }

    @Test
    fun `the coach gets the workout and a failed question can be retried`() {
        server.enqueue(answer(401))
        viewModel.askCoach("Какой вес дальше?", mapOf(bench to "Жим лёжа"), focusId = null)
        awaitIdle()
        assertEquals(AiFailure.BAD_KEY, viewModel.coachFailure)

        server.enqueue(answer(200, "Попробуй 60 кг"))
        viewModel.askCoach("И сколько повторов?", mapOf(bench to "Жим лёжа"), focusId = null)
        awaitIdle()

        assertEquals(listOf("Какой вес дальше?\nИ сколько повторов?", "Попробуй 60 кг"), viewModel.chat.map { it.text })
        server.takeRequest()
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("Жим лёжа") && body.contains("Answer in "))
    }
}
