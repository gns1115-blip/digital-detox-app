package com.example.instagramdetector.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.instagramdetector.InstagramDetectionState
import com.example.instagramdetector.detox.DetoxPrefs
import com.example.instagramdetector.detection.MonitoredApps
import com.example.instagramdetector.detection.ShortsReelsDetector
import com.example.instagramdetector.detection.ShortsReelsWatchTracker
import com.example.instagramdetector.overlay.AppSessionGate
import com.example.instagramdetector.overlay.OverlayController
import com.example.instagramdetector.util.canDrawOverlays

class AppAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceHolder.bind(this)
    }

    override fun onDestroy() {
        AccessibilityServiceHolder.unbind()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        if (!MonitoredApps.isMonitored(packageName)) {
            handleLeftMonitoredApp()
            return
        }

        if (DetoxPrefs.isProtectionEnabled(this) && AppLaunchBlocker.isBlocked(packageName)) {
            AccessibilityAppCloser.goHome()
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (AppSessionGate.isInstagramPackage(packageName)) {
                    handleInstagramForeground(packageName)
                }
                monitorShortFormFeed(packageName, event)
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            -> monitorShortFormFeed(packageName, event)
        }
    }

    private fun handleInstagramForeground(packageName: String) {
        InstagramDetectionState.onInstagramOpened(packageName)

        if (AppSessionGate.isSessionAllowed(packageName)) return
        if (!DetoxPrefs.isProtectionEnabled(this)) return
        if (!canDrawOverlays()) return
        if (ShortsReelsWatchTracker.isBreakOverlayActive) return

        OverlayController.show(this, packageName)
    }

    private fun monitorShortFormFeed(packageName: String, event: AccessibilityEvent) {
        if (ShortsReelsWatchTracker.isBreakOverlayActive) return

        val root = rootInActiveWindow
        val inShortFormFeed = try {
            ShortsReelsDetector.isShortsOrReelsScreen(packageName, event, root)
        } finally {
            root?.recycle()
        }

        ShortsReelsWatchTracker.onWatchStateChanged(inShortFormFeed)

        if (!ShortsReelsWatchTracker.shouldTriggerBreak()) return
        if (!DetoxPrefs.isProtectionEnabled(this)) return
        if (!canDrawOverlays()) return

        OverlayController.showShortFormBreak(this, packageName)
    }

    private fun handleLeftMonitoredApp() {
        UsageTimerController.cancel(this)
        ShortsReelsWatchTracker.reset()
        val hadSession = AppSessionGate.revokeSessionIfActive()
        if (hadSession) {
            OverlayController.hide(this)
        }
    }

    override fun onInterrupt() {
        // No long-running operation to cancel.
    }
}
