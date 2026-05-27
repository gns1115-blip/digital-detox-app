package com.example.instagramdetector.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.example.instagramdetector.InstagramDetectorApplication
import com.example.instagramdetector.R
import com.example.instagramdetector.InstagramDetectionState
import com.example.instagramdetector.datastore.OverlaySelectionRecord
import com.example.instagramdetector.detection.ShortsReelsWatchTracker
import com.example.instagramdetector.detox.DetoxPrefs
import com.example.instagramdetector.service.UsageTimerController
import com.example.instagramdetector.ui.MainActivity
import com.example.instagramdetector.util.canDrawOverlays
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private enum class OverlayMode {
        NONE,
        INTENT,
        TIME_EXPIRED_BLOCK,
        SHORT_FORM_BREAK,
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var blockedPackageName: String? = null
    private var overlayStep by mutableStateOf<OverlayStep>(OverlayStep.SelectReason)
    private var blockSecondsRemaining by mutableIntStateOf(DEFAULT_SCROLL_BLOCK_SECONDS)
    private var overlayMode: OverlayMode = OverlayMode.NONE
    private var blockCountdownJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                if (!DetoxPrefs.isProtectionEnabled(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (!canDrawOverlays() || overlayMode == OverlayMode.SHORT_FORM_BREAK) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startAsForeground()
                overlayMode = OverlayMode.INTENT
                blockedPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                overlayStep = OverlayStep.SelectReason
                showOverlay(blockTouches = false)
            }

            ACTION_SHOW_USAGE_TIME_EXPIRED -> {
                if (!DetoxPrefs.isProtectionEnabled(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (!canDrawOverlays()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                dismissOverlay()
                startAsForeground()
                overlayMode = OverlayMode.TIME_EXPIRED_BLOCK
                blockedPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                val blockSeconds = intent.getIntExtra(EXTRA_BLOCK_DURATION_SECONDS, DEFAULT_SCROLL_BLOCK_SECONDS)
                startTimedBlockOverlay(
                    initialStep = OverlayStep.TimeExpiredBlock(blockSeconds),
                    blockSeconds = blockSeconds,
                ) {
                    overlayMode = OverlayMode.NONE
                    dismissOverlayAndStop()
                }
            }

            ACTION_SHOW_SHORT_FORM_BREAK -> {
                if (!DetoxPrefs.isProtectionEnabled(this)) {
                    ShortsReelsWatchTracker.onBreakOverlayDismissed()
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (!canDrawOverlays()) {
                    ShortsReelsWatchTracker.onBreakOverlayDismissed()
                    stopSelf()
                    return START_NOT_STICKY
                }
                dismissOverlay()
                startAsForeground()
                overlayMode = OverlayMode.SHORT_FORM_BREAK
                blockedPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                val blockSeconds = intent.getIntExtra(EXTRA_BLOCK_DURATION_SECONDS, DEFAULT_SCROLL_BLOCK_SECONDS)
                ShortsReelsWatchTracker.markBreakOverlayShown()
                startTimedBlockOverlay(
                    initialStep = OverlayStep.ShortFormBreak(blockSeconds),
                    blockSeconds = blockSeconds,
                ) {
                    ShortsReelsWatchTracker.onBreakOverlayDismissed()
                    overlayMode = OverlayMode.NONE
                    dismissOverlayAndStop()
                }
            }

            ACTION_HIDE -> {
                cancelBlockCountdown()
                overlayMode = OverlayMode.NONE
                dismissOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        cancelBlockCountdown()
        overlayMode = OverlayMode.NONE
        serviceScope.cancel()
        dismissOverlay()
        super.onDestroy()
    }

    private fun startTimedBlockOverlay(
        initialStep: OverlayStep,
        blockSeconds: Int,
        onFinished: () -> Unit,
    ) {
        blockSecondsRemaining = blockSeconds
        overlayStep = initialStep
        showOverlay(blockTouches = true)
        startBlockCountdown(blockSeconds, onFinished)
    }

    private fun startBlockCountdown(blockSeconds: Int, onFinished: () -> Unit) {
        cancelBlockCountdown()
        blockCountdownJob = serviceScope.launch {
            for (remaining in blockSeconds downTo 1) {
                blockSecondsRemaining = remaining
                overlayStep = when (overlayMode) {
                    OverlayMode.TIME_EXPIRED_BLOCK -> OverlayStep.TimeExpiredBlock(remaining)
                    OverlayMode.SHORT_FORM_BREAK -> OverlayStep.ShortFormBreak(remaining)
                    else -> overlayStep
                }
                delay(1_000L)
            }
            onFinished()
        }
    }

    private fun cancelBlockCountdown() {
        blockCountdownJob?.cancel()
        blockCountdownJob = null
    }

    private fun startAsForeground() {
        createNotificationChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun showOverlay(blockTouches: Boolean) {
        if (overlayView != null) return

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindow,
            )
            setContent {
                MaterialTheme {
                    val step = when (overlayMode) {
                        OverlayMode.TIME_EXPIRED_BLOCK ->
                            OverlayStep.TimeExpiredBlock(blockSecondsRemaining)
                        OverlayMode.SHORT_FORM_BREAK ->
                            OverlayStep.ShortFormBreak(blockSecondsRemaining)
                        else -> overlayStep
                    }
                    InstagramOverlayContent(
                        step = step,
                        onReasonSelected = { reason ->
                            overlayStep = OverlayStep.SelectDuration(reason)
                        },
                        onDurationSelected = { duration ->
                            onSessionConfigured(duration)
                        },
                    )
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager?.addView(composeView, layoutParams)
        overlayView = composeView
    }

    private fun onSessionConfigured(duration: UsageDuration) {
        val packageName = blockedPackageName ?: return
        val reason = (overlayStep as? OverlayStep.SelectDuration)?.reason ?: return

        val timestamp = System.currentTimeMillis()
        val record = OverlaySelectionRecord(
            timestamp = timestamp,
            packageName = packageName,
            selectedReason = reason,
        )
        val repository = (application as InstagramDetectorApplication).overlaySelectionRepository
        serviceScope.launch(Dispatchers.IO) {
            repository.saveRecord(record)
        }

        AppSessionGate.grantSession(packageName, reason, duration)
        InstagramDetectionState.onReasonSelected(reason, timestamp, packageName)
        UsageTimerController.start(this, packageName, duration.minutes)

        overlayMode = OverlayMode.NONE
        dismissOverlayAndStop()
    }

    private fun dismissOverlayAndStop() {
        cancelBlockCountdown()
        dismissOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun dismissOverlay() {
        overlayView?.let { view ->
            windowManager?.removeView(view)
        }
        overlayView = null
        blockedPackageName = null
        overlayStep = OverlayStep.SelectReason
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.overlay_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.overlay_notification_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val (title, text) = when (overlayMode) {
            OverlayMode.SHORT_FORM_BREAK -> getString(R.string.reels_break_notification_title) to
                getString(R.string.reels_break_notification_text)
            OverlayMode.TIME_EXPIRED_BLOCK -> getString(R.string.overlay_notification_title) to
                getString(R.string.usage_expired_notification_text)
            else -> getString(R.string.overlay_notification_title) to
                getString(R.string.overlay_notification_text)
        }
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_SHOW = "com.example.instagramdetector.overlay.SHOW"
        const val ACTION_SHOW_USAGE_TIME_EXPIRED =
            "com.example.instagramdetector.overlay.SHOW_USAGE_TIME_EXPIRED"
        const val ACTION_SHOW_SHORT_FORM_BREAK =
            "com.example.instagramdetector.overlay.SHOW_SHORT_FORM_BREAK"
        const val ACTION_HIDE = "com.example.instagramdetector.overlay.HIDE"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_BLOCK_DURATION_SECONDS = "extra_block_duration_seconds"
        const val DEFAULT_SCROLL_BLOCK_SECONDS = 30

        private const val NOTIFICATION_CHANNEL_ID = "overlay_service"
        private const val NOTIFICATION_ID = 1001
    }
}
