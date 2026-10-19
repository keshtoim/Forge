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
    private val checker = UpdateChecker(apiUrl = server.url("/").toString().trimEnd('/'), webUrl = server.url("/web").toString())

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

    @Test
    fun `a rate-limited api falls back to the release page redirect`() {
        val web = server.url("/web").toString()
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"API rate limit exceeded"}"""))
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "$web/keshtoim/Forge/releases/tag/v0.3.0"))
        // The asset list is HTML; whatever the APK is called, its download link is picked up.
        server.enqueue(MockResponse().setResponseCode(200).setBody("""<a href="/web/keshtoim/Forge/releases/download/v0.3.0/forge.apk" rel="nofollow">forge.apk</a>"""))

        val update = runBlocking { checker.check("0.2.0") }!!

        assertEquals("0.3.0", update.version)
        assertEquals("$web/keshtoim/Forge/releases/download/v0.3.0/forge.apk", update.apkUrl)
        assertNull(update.sha256)
        server.takeRequest()
        assertEquals("/web/keshtoim/Forge/releases/latest", server.takeRequest().path)
        assertEquals("/web/keshtoim/Forge/releases/expanded_assets/v0.3.0", server.takeRequest().path)

        // Before the first release the page redirects to the release list.
        server.enqueue(MockResponse().setResponseCode(429))
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "$web/keshtoim/Forge/releases"))
        assertNull(runBlocking { checker.check("0.2.0") })
    }
}
