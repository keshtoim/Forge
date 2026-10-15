package com.keshtoim.forge.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.data.db.Exercise
import com.keshtoim.forge.data.db.ExerciseType
import com.keshtoim.forge.data.db.MuscleGroup
import com.keshtoim.forge.data.db.PlannedWorkout
import com.keshtoim.forge.data.db.Template
import com.keshtoim.forge.data.db.TemplateExercise
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class PlanRemindersTest {
    private val app = ApplicationProvider.getApplicationContext<ForgeApplication>()
    private val alarms = shadowOf(app.getSystemService(AlarmManager::class.java))
    private val tomorrow = LocalDate.now().plusDays(1)

    private fun plan(remindBefore: Int?, templateId: Long = 1) = PlannedWorkout(
        id = 7,
        templateId = templateId,
        epochDay = tomorrow.toEpochDay(),
        minuteOfDay = 18 * 60,
        remindBefore = remindBefore,
    )

    private fun millis(at: LocalDateTime) = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `alarm is set before the workout and cleared when the reminder is turned off`() {
        app.planReminders.schedule(plan(remindBefore = 30))
        assertEquals(millis(tomorrow.atTime(17, 30)), alarms.nextScheduledAlarm!!.triggerAtTime)

        app.planReminders.schedule(plan(remindBefore = null))
        assertNull(alarms.nextScheduledAlarm)
    }

    @Test
    fun `receiver posts a reminder with the template name`() {
        val saved = runBlocking {
            val db = app.database
            val exercise = db.exerciseDao().insert(Exercise(name = "Bench", muscleGroup = MuscleGroup.CHEST, type = ExerciseType.WEIGHT_REPS))
            val templateId = db.templateDao().save(Template(name = "Push"), listOf(TemplateExercise(templateId = 0, exerciseId = exercise, position = 0, targetSets = 3)))
            plan(remindBefore = 30, templateId).copy(id = db.planDao().save(plan(remindBefore = 30, templateId).copy(id = 0)))
        }

        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        // Robolectric routes broadcasts by intent filter only, so the alarm's explicit intent is mimicked with a registered receiver.
        app.registerReceiver(PlanReminderReceiver(), IntentFilter(PlanReminders.ACTION_REMIND), Context.RECEIVER_NOT_EXPORTED)
        app.sendBroadcast(Intent(PlanReminders.ACTION_REMIND).setPackage(app.packageName).putExtra(PlanReminders.EXTRA_PLAN, saved.id))
        shadowOf(Looper.getMainLooper()).idle()

        val notifications = shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        val deadline = System.currentTimeMillis() + 5_000
        while (notifications.allNotifications.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(20)
        val text = shadowOf(notifications.allNotifications.single()).contentText
        assertEquals("Push at 18:00", text)
    }
}
