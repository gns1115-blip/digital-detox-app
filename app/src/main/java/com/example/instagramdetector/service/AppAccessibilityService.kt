package com.example.instagramdetector.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.instagramdetector.InstagramDetectionState
import com.example.instagramdetector.InstagramDetectorApplication
import com.example.instagramdetector.detox.DetoxPrefs
import com.example.instagramdetector.detection.BehavioralDetector
import com.example.instagramdetector.detection.ShortsReelsWatchTracker
import com.example.instagramdetector.overlay.AppSessionGate
import com.example.instagramdetector.overlay.OverlayController
import com.example.instagramdetector.util.canDrawOverlays
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastMonitoredPackage: String? = null
    private var lastInterventionAt = 0L
    private val INTERVENTION_COOLDOWN_MS = 5000L // 5초 쿨타임
    
    private var cachedMonitoredPackages = setOf<String>()
    
    private val ignoredPackages = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.example.instagramdetector"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityServiceHolder.bind(this)
        
        val app = application as InstagramDetectorApplication
        serviceScope.launch {
            app.monitoredAppsRepository.monitoredPackages.collectLatest { packages ->
                cachedMonitoredPackages = packages
            }
        }
    }

    override fun onDestroy() {
        AccessibilityServiceHolder.unbind()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        if (ignoredPackages.contains(packageName)) return

        val isMonitored = cachedMonitoredPackages.contains(packageName)
        
        // 쿨타임 체크 (프레임마다 발동 방지)
        val now = System.currentTimeMillis()
        val inCooldown = (now - lastInterventionAt < INTERVENTION_COOLDOWN_MS)

        // 1. 차단 상태 체크
        if (isMonitored && DetoxPrefs.isProtectionEnabled(this) && 
            AppLaunchBlocker.isBlocked(packageName)) {
            
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !inCooldown) {
                lastInterventionAt = now
                returnToDetoxLauncher()
            }
            return
        }

        // 2. 관리 대상 앱인 경우
        if (isMonitored) {
            handleMonitoredAppEvent(packageName, event, now, inCooldown)
        } else {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                handleLeftMonitoredApp()
            }
        }
    }

    private fun handleMonitoredAppEvent(packageName: String, event: AccessibilityEvent, now: Long, inCooldown: Boolean) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (lastMonitoredPackage != packageName) {
                lastMonitoredPackage = packageName
                onAppForeground(packageName)
            }
        }

        // 행동 패턴 감지
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED || 
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            BehavioralDetector.recordEvent(event.eventType)
            
            if (BehavioralDetector.isDoomscrolling() && 
                !ShortsReelsWatchTracker.isBreakOverlayActive &&
                !inCooldown &&
                DetoxPrefs.isProtectionEnabled(this) && 
                canDrawOverlays()) {
                
                lastInterventionAt = now
                BehavioralDetector.reset()
                OverlayController.showShortFormBreak(this, packageName)
            }
        }
    }

    private fun onAppForeground(packageName: String) {
        InstagramDetectionState.onInstagramOpened(packageName)

        if (!AppSessionGate.isSessionAllowed(packageName) && 
            DetoxPrefs.isProtectionEnabled(this) && 
            canDrawOverlays()) {
            
            OverlayController.show(this, packageName)
        }
    }

    private fun handleLeftMonitoredApp() {
        if (lastMonitoredPackage == null) return
        lastMonitoredPackage = null
        BehavioralDetector.reset()
        UsageTimerController.cancel(this)
        
        val hadSession = AppSessionGate.revokeSessionIfActive()
        if (hadSession) {
            OverlayController.hide(this)
            // 우리 앱(런처)으로 복귀
            returnToDetoxLauncher()
        }
    }

    private fun returnToDetoxLauncher() {
        val intent = Intent(this, com.example.instagramdetector.ui.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
