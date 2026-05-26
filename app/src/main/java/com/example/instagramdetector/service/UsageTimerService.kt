package com.example.instagramdetector.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.instagramdetector.InstagramDetectorApplication
import com.example.instagramdetector.R
import com.example.instagramdetector.overlay.AppSessionGate
import com.example.instagramdetector.overlay.OverlayController
import com.example.instagramdetector.util.VibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UsageTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var countdownJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 5)
                startCountdown(packageName, durationMinutes)
            }

            ACTION_CANCEL -> stopCountdownAndSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startCountdown(packageName: String, durationMinutes: Int) {
        countdownJob?.cancel()
        startAsForeground()

        countdownJob = serviceScope.launch {
            delay(durationMinutes * 60_000L)
            onUsageTimeExpired(packageName)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun onUsageTimeExpired(packageName: String) {
        val app = application as InstagramDetectorApplication
        val usageDurationMs = AppSessionGate.consumeSessionUsageDuration()

        VibrationHelper.vibrate(applicationContext)
        AppSessionGate.revokeSessionIfActive()
        AccessibilityAppCloser.goHome()
        AppLaunchBlocker.startBlock(AppLaunchBlocker.DEFAULT_BLOCK_SECONDS)

        serviceScope.launch(Dispatchers.IO) {
            app.appStatsRepository.addUsageTime(usageDurationMs)
            app.appStatsRepository.recordBlock()
        }

        OverlayController.showUsageTimeExpired(
            context = applicationContext,
            packageName = packageName,
            blockSeconds = AppLaunchBlocker.DEFAULT_BLOCK_SECONDS,
        )
    }

    private fun stopCountdownAndSelf() {
        countdownJob?.cancel()
        countdownJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startAsForeground() {
        createNotificationChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.timer_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.timer_notification_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.example.instagramdetector.ui.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.timer_notification_title))
            .setContentText(getString(R.string.timer_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.instagramdetector.timer.START"
        const val ACTION_CANCEL = "com.example.instagramdetector.timer.CANCEL"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

        private const val NOTIFICATION_CHANNEL_ID = "usage_timer_service"
        private const val NOTIFICATION_ID = 1002
    }
}
