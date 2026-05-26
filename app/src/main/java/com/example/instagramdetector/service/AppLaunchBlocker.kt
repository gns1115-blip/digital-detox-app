package com.example.instagramdetector.service

import com.example.instagramdetector.detection.MonitoredApps

/**
 * 사용 시간 만료 후 일정 시간 동안 대상 앱 재실행을 차단합니다.
 */
object AppLaunchBlocker {

    const val DEFAULT_BLOCK_SECONDS = 30

    @Volatile
    private var blockedUntilMillis: Long = 0L

    fun startBlock(
        durationSeconds: Int = DEFAULT_BLOCK_SECONDS,
        now: Long = System.currentTimeMillis(),
    ) {
        blockedUntilMillis = now + durationSeconds * 1_000L
    }

    fun isBlocked(packageName: String, now: Long = System.currentTimeMillis()): Boolean {
        if (now >= blockedUntilMillis) return false
        return packageName in MonitoredApps.relaunchBlockTargets
    }

    fun remainingBlockSeconds(now: Long = System.currentTimeMillis()): Int {
        if (now >= blockedUntilMillis) return 0
        return ((blockedUntilMillis - now) / 1_000L).toInt().coerceAtLeast(0)
    }

    fun clear() {
        blockedUntilMillis = 0L
    }
}
