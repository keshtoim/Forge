package com.keshtoim.forge.rest

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class RestTimerServiceTest {
    private val app = ApplicationProvider.getApplicationContext<ForgeApplication>()

    @Test
    fun `service started after rest was skipped still enters foreground before stopping`() {
        app.restTimer.stop()

        val service = Robolectric.buildService(RestTimerService::class.java, Intent(app, RestTimerService::class.java))
            .create()
            .startCommand(0, 1)
            .get()

        assertNotNull(shadowOf(service).lastForegroundNotification)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test
    fun `skip action clears the rest`() {
        app.restTimer.start()

        Robolectric.buildService(RestTimerService::class.java, Intent(app, RestTimerService::class.java).setAction("com.keshtoim.forge.rest.SKIP"))
            .create()
            .startCommand(0, 1)

        assertNull(app.restTimer.state.value)
    }
}
