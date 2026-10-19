package com.keshtoim.forge.data.update

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    private val server = MockWebServer().apply { start() }
    private val checker = UpdateChecker(server.url("/").toString().trimEnd('/'))

    @After
    fun tearDown() = server.shutdown()

    private fun release(tag: String, assets: String = """[{"name":"forge-$tag.apk","browser_download_url":"https://github.com/x/forge-$tag.apk","size":9400000,"digest":"sha256:abc123"}]""") =
        MockResponse().setResponseCode(200).setBody(
            """{"tag_name":"$tag","body":"## Forge\n- Суперсеты","html_url":"https://github.com/keshtoim/Forge/releases/tag/$tag","draft":false,"prerelease":false,"assets":$assets}""",
        )

    @Test
    fun `a newer release with an apk is offered`() {
        server.enqueue(release("v0.3.0"))
        val update = runBlocking { checker.check("0.2.0") }!!
        assertEquals("0.3.0", update.version)
        assertEquals("https://github.com/x/forge-v0.3.0.apk", update.apkUrl)
        assertEquals("abc123", update.sha256)
        assertEquals(9_400_000L, update.size)
        assertTrue(update.notes.contains("Суперсеты"))
        assertEquals("/repos/keshtoim/Forge/releases/latest", server.takeRequest().path)
    }

    @Test
    fun `nothing is offered when up to date, without an apk or before the first release`() {
        server.enqueue(release("v0.2.0"))
        assertNull(runBlocking { checker.check("0.2.0") })
        server.enqueue(release("v0.3.0", assets = "[]"))
        assertNull(runBlocking { checker.check("0.2.0") })
        server.enqueue(MockResponse().setResponseCode(404))
        assertNull(runBlocking { checker.check("0.2.0") })
    }

    @Test
    fun `versions compare by number`() {
        assertTrue(isNewer("0.10.0", "0.9.3"))
        assertTrue(isNewer("v1.0.0", "0.9.9"))
        assertTrue(isNewer("0.2.1", "0.2"))
        assertFalse(isNewer("0.2.0", "0.2.0"))
        assertFalse(isNewer("0.2.0-beta", "0.2.0"))
    }
}
