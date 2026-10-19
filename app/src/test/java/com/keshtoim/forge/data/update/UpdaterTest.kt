package com.keshtoim.forge.data.update

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UpdaterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val server = MockWebServer().apply { start() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val updater = Updater(context, scope, UpdateChecker(apiUrl = server.url("/").toString().trimEnd('/')), automatic = true)

    @After
    fun tearDown() {
        scope.cancel()
        server.shutdown()
    }

    private fun release(tag: String) = MockResponse().setResponseCode(200).setBody(
        """{"tag_name":"$tag","body":"Notes","html_url":"https://github.com/r/$tag","assets":[{"name":"forge-$tag.apk","browser_download_url":"https://github.com/r/a.apk","size":1}]}""",
    )

    private fun <T> await(block: suspend () -> T) = runBlocking { withTimeout(10_000) { block() } }

    @Test
    fun `a manual check reports either the new version or that the app is up to date`() {
        server.enqueue(release("v99.0.0"))
        updater.check()
        val available = await { updater.state.first { it is UpdateState.Available } } as UpdateState.Available
        assertEquals("99.0.0", available.update.version)

        server.enqueue(release("v0.0.1"))
        updater.check()
        await { updater.state.first { it == UpdateState.UpToDate } }

        server.enqueue(MockResponse().setResponseCode(500))
        updater.check()
        await { updater.state.first { it == UpdateState.Failed(UpdateFailure.CHECK, null) } }
    }

    @Test
    fun `automatic checks run at most once a day and only while enabled`() {
        updater.setAutoCheck(false)
        updater.checkIfDue()
        assertEquals(0, server.requestCount)

        updater.setAutoCheck(true)
        server.enqueue(release("v0.0.1"))
        updater.checkIfDue()
        await { while (server.requestCount == 0) kotlinx.coroutines.delay(20) }
        // A quiet check that finds nothing leaves no trace on screen.
        await { updater.state.first { it == UpdateState.Idle } }

        updater.checkIfDue()
        Thread.sleep(300)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `sha256 is the lowercase hex GitHub publishes in a digest`() {
        val file = File(context.cacheDir, "a.bin").apply { writeText("forge") }
        assertEquals("71b41d6dd48dc58eba8f5cf9edf30fef6597fdf285a521bb8fcbad4b3d50887d", sha256(file))
    }
}
