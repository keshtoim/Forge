package com.keshtoim.forge.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.MainActivity
import com.keshtoim.forge.R
import com.keshtoim.forge.data.db.PlannedWorkout
import com.keshtoim.forge.data.nextReminder
import com.keshtoim.forge.data.time
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PlanReminders(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    // Replaces the plan's pending alarm, or cancels it when the plan has no reminder ahead.
    fun schedule(plan: PlannedWorkout) {
        val intent = alarmIntent(plan.id)
        val at = plan.nextReminder(LocalDateTime.now())
        if (at == null) {
            alarms.cancel(intent)
            return
        }
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Exact alarms need the user's consent on Android 13+; without it a slightly late reminder is still useful.
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
        }
    }

    fun cancel(planId: Long) = alarms.cancel(alarmIntent(planId))

    fun notify(plan: PlannedWorkout, templateName: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text, templateName, plan.time.format(DateTimeFormatter.ofPattern("HH:mm"))))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE + plan.id.toInt(), notification)
    }

    private fun alarmIntent(planId: Long) = PendingIntent.getBroadcast(
        context, planId.toInt(),
        Intent(context, PlanReminderReceiver::class.java).setAction(ACTION_REMIND).putExtra(EXTRA_PLAN, planId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val ACTION_REMIND = "com.keshtoim.forge.REMIND"
        const val EXTRA_PLAN = "plan"
        private const val CHANNEL = "plan_reminders"
        // Keeps reminder ids clear of the rest timer's notifications.
        private const val NOTIFICATION_BASE = 1_000

        fun createChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.channel_plan_reminders), NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }
}

// Fires a plan's reminder and arms the next one; after a reboot or clock change it re-arms every plan.
class PlanReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as ForgeApplication
        val work: suspend () -> Unit = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                { app.database.planDao().getAll().forEach(app.planReminders::schedule) }
            }
            PlanReminders.ACTION_REMIND -> {
                { remind(app, intent.getLongExtra(PlanReminders.EXTRA_PLAN, 0)) }
            }
            else -> return
        }
        val result = goAsync()
        app.appScope.launch {
            try {
                work()
            } finally {
                result.finish()
            }
        }
    }

    private suspend fun remind(app: ForgeApplication, planId: Long) {
        // The plan may have been deleted together with its template after the alarm was set.
        val plan = app.database.planDao().get(planId)?.takeIf { it.remindBefore != null } ?: return
        val template = app.database.templateDao().get(plan.templateId) ?: return
        app.planReminders.notify(plan, template.template.name)
        app.planReminders.schedule(plan)
    }
}
