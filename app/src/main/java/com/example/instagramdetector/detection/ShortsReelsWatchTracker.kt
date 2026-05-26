package com.example.instagramdetector.detection

/**
 * Reels/Shorts 연속 시청 시간을 추적하고 10분 초과 시 휴식 Overlay를 트리거합니다.
 */
object ShortsReelsWatchTracker {

    const val BREAK_THRESHOLD_MS = 10 * 60 * 1000L
    private const val COOLDOWN_AFTER_BREAK_MS = 3 * 60 * 1000L

    @Volatile
    private var watchingStartedAtMillis: Long? = null

    @Volatile
    private var isWatchingShortForm: Boolean = false

    @Volatile
    private var lastBreakShownAtMillis: Long? = null

    @Volatile
    var isBreakOverlayActive: Boolean = false
        private set

    fun onWatchStateChanged(inShortFormFeed: Boolean, now: Long = System.currentTimeMillis()) {
        if (isBreakOverlayActive) return

        if (inShortFormFeed) {
            if (!isWatchingShortForm) {
                isWatchingShortForm = true
                watchingStartedAtMillis = now
            }
        } else {
            isWatchingShortForm = false
            watchingStartedAtMillis = null
        }
    }

    fun shouldTriggerBreak(now: Long = System.currentTimeMillis()): Boolean {
        if (!isWatchingShortForm || isBreakOverlayActive) return false

        val startedAt = watchingStartedAtMillis ?: return false
        val lastBreak = lastBreakShownAtMillis
        if (lastBreak != null && now - lastBreak < COOLDOWN_AFTER_BREAK_MS) return false
        return now - startedAt >= BREAK_THRESHOLD_MS
    }

    fun markBreakOverlayShown(now: Long = System.currentTimeMillis()) {
        isBreakOverlayActive = true
        lastBreakShownAtMillis = now
        watchingStartedAtMillis = now
    }

    fun onBreakOverlayDismissed(now: Long = System.currentTimeMillis()) {
        isBreakOverlayActive = false
        if (isWatchingShortForm) {
            watchingStartedAtMillis = now
        }
    }

    fun reset() {
        isWatchingShortForm = false
        watchingStartedAtMillis = null
        isBreakOverlayActive = false
    }
}
