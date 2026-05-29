package com.example.instagramdetector.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.instagramdetector.InstagramDetectorApplication
import com.example.instagramdetector.R
import com.example.instagramdetector.detox.DetoxPrefs
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
    private var currentPackageName: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 8.0+ 에서는 startForegroundService 호출 후 반드시 startForeground를 호출해야 함
        startAsForeground()

        when (intent?.action) {
            ACTION_START -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 5)
                currentPackageName = packageName
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
        // startAsForeground()는 이미 onStartCommand에서 호출됨

        countdownJob = serviceScope.launch {
            delay(durationMinutes * 60_000L)
            onUsageTimeExpired(packageName)
        }
    }

    private fun onUsageTimeExpired(packageName: String) {
        recordUsageAndStats(isExpired = true)

        if (DetoxPrefs.isProtectionEnabled(applicationContext)) {
            VibrationHelper.vibrate(applicationContext)
            
            // 홈 화면 대신 우리 앱 시작 화면으로 복귀
            val intent = Intent(this, com.example.instagramdetector.ui.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)

            AppLaunchBlocker.startBlock(AppLaunchBlocker.DEFAULT_BLOCK_SECONDS)
            
            OverlayController.showUsageTimeExpired(
                context = applicationContext,
                packageName = packageName,
                blockSeconds = AppLaunchBlocker.DEFAULT_BLOCK_SECONDS,
            )
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopCountdownAndSelf() {
        recordUsageAndStats(isExpired = false)
        countdownJob?.cancel()
        countdownJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun recordUsageAndStats(isExpired: Boolean) {
        val usageDurationMs = AppSessionGate.consumeSessionUsageDuration()
        if (usageDurationMs <= 0) return

        val app = application as InstagramDetectorApplication
        val protectionEnabled = DetoxPrefs.isProtectionEnabled(applicationContext)
        
        AppSessionGate.revokeSessionIfActive()
        if (!isExpired) {
            AppLaunchBlocker.clear()
        }

        serviceScope.launch(Dispatchers.IO) {
            app.appStatsRepository.addUsageTime(usageDurationMs)
            if (isExpired && protectionEnabled) {
                app.appStatsRepository.recordBlock()
            }
        }
    }

    private fun startAsForeground() {
        createNotificationChannelIfNeeded()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
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
