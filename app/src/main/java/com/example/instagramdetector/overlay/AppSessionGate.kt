package com.example.instagramdetector.overlay

import com.example.instagramdetector.detection.MonitoredApps

/**
 * Overlay에서 선택한 사용 세션(이유·허용 시간)을 관리합니다.
 */
object AppSessionGate {

    @Volatile
    private var allowedPackage: String? = null

    @Volatile
    private var sessionStartedAtMillis: Long? = null

    @Volatile
    private var sessionExpiresAtMillis: Long? = null

    @Volatile
    var lastSelectedReason: OverlayReason? = null
        private set

    @Volatile
    var lastSelectedDuration: UsageDuration? = null
        private set

    fun isInstagramPackage(packageName: String): Boolean =
        MonitoredApps.isInstagram(packageName)

    fun isSessionAllowed(packageName: String): Boolean {
        if (allowedPackage != packageName) return false
        val expiresAt = sessionExpiresAtMillis ?: return false
        return System.currentTimeMillis() < expiresAt
    }

    fun grantSession(packageName: String, reason: OverlayReason, duration: UsageDuration) {
        val now = System.currentTimeMillis()
        allowedPackage = packageName
        lastSelectedReason = reason
        lastSelectedDuration = duration
        sessionStartedAtMillis = now
        sessionExpiresAtMillis = now + duration.minutes * 60_000L
    }

    fun consumeSessionUsageDuration(): Long {
        val startedAt = sessionStartedAtMillis ?: return 0L
        return (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
    }

    fun revokeSessionIfActive(): Boolean {
        val hadSession = allowedPackage != null
        allowedPackage = null
        sessionStartedAtMillis = null
        sessionExpiresAtMillis = null
        return hadSession
    }
}
