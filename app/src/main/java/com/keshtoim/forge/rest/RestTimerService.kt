package com.keshtoim.forge.rest

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.keshtoim.forge.ForgeApplication
import com.keshtoim.forge.MainActivity
import com.keshtoim.forge.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RestTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val timer get() = (application as ForgeApplication).restTimer
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD -> timer.adjust(15)
            ACTION_SKIP -> timer.stop()
        }
        val rest = timer.state.value
        // startForegroundService() requires startForeground() on every start, even if the rest was
        // skipped before the service got here; otherwise the system crashes the app.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ONGOING,
            rest?.let(::ongoingNotification) ?: NotificationCompat.Builder(this, CHANNEL_ONGOING)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.rest_title))
                .setSilent(true)
                .build(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (rest == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (job == null) job = scope.launch { run() }
        return START_NOT_STICKY
    }

    private suspend fun run() {
        timer.state.collectLatest { rest ->
            if (rest == null) {
                stopSelf()
                return@collectLatest
            }
            notify(NOTIFICATION_ONGOING, ongoingNotification(rest))
            val remaining = rest.endsAt - System.currentTimeMillis()
            // Keep the CPU awake so the countdown fires on time with the screen off.
            wakeLock?.takeIf { it.isHeld }?.release()
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "forge:rest")
                .apply { acquire(remaining.coerceAtLeast(0) + 5_000) }
            delay(remaining)
            vibrate()
            notify(NOTIFICATION_DONE, doneNotification())
            timer.stop()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    private fun notify(id: Int, notification: Notification) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
            Build.VERSION.SDK_INT < 33
        ) {
            NotificationManagerCompat.from(this).notify(id, notification)
        }
    }

    private fun ongoingNotification(rest: Rest) =
        NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.rest_title))
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(rest.endsAt)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp())
            .addAction(0, getString(R.string.rest_add), action(ACTION_ADD))
            .addAction(0, getString(R.string.rest_skip), action(ACTION_SKIP))
            .build()

    private fun doneNotification() =
        NotificationCompat.Builder(this, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.rest_done))
            .setContentText(getString(R.string.rest_done_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(openApp())
            .build()

    private fun openApp() = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(name: String) = PendingIntent.getService(
        this, name.hashCode(),
        Intent(this, RestTimerService::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
    }

    companion object {
        private const val CHANNEL_ONGOING = "rest_timer"
        private const val CHANNEL_DONE = "rest_done"
        private const val NOTIFICATION_ONGOING = 1
        private const val NOTIFICATION_DONE = 2
        private const val ACTION_ADD = "com.keshtoim.forge.rest.ADD"
        private const val ACTION_SKIP = "com.keshtoim.forge.rest.SKIP"

        fun createChannels(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannels(
                listOf(
                    NotificationChannel(CHANNEL_ONGOING, context.getString(R.string.channel_rest_timer), NotificationManager.IMPORTANCE_LOW),
                    NotificationChannel(CHANNEL_DONE, context.getString(R.string.channel_rest_done), NotificationManager.IMPORTANCE_HIGH)
                        .apply { enableVibration(true) },
                )
            )
        }
    }
}
