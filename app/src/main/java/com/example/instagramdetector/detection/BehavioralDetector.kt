package com.example.instagramdetector.detection

import android.view.accessibility.AccessibilityEvent

/**
 * 사용자의 조작 패턴을 분석하여 숏폼 시청 여부를 판단합니다.
 * 디바운스 및 상태 관리를 강화한 버전입니다.
 */
object BehavioralDetector {
    private const val RESET_THRESHOLD_MS = 60_000L // 1분간 활동 없으면 리셋
    private const val MONITOR_WINDOW_MS = 5 * 60_000L // 5분간 관찰
    private const val DEBOUNCE_MS = 300L // 너무 빠른 이벤트 무시
    
    private var scrollCount = 0
    private var clickCount = 0
    private var lastActivityAt = 0L
    private var lastEventAt = 0L
    private var windowStartedAt = 0L

    fun recordEvent(eventType: Int) {
        val now = System.currentTimeMillis()
        
        // 디바운스 처리
        if (now - lastEventAt < DEBOUNCE_MS) return
        lastEventAt = now
        
        // 너무 오랜만에 활동하면 초기화
        if (now - lastActivityAt > RESET_THRESHOLD_MS) {
            reset()
            windowStartedAt = now
        }
        
        lastActivityAt = now
        
        when (eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> scrollCount++
            AccessibilityEvent.TYPE_VIEW_CLICKED -> clickCount++
        }
    }

    fun isDoomscrolling(): Boolean {
        val now = System.currentTimeMillis()
        val duration = now - windowStartedAt
        
        // 최소 관찰 시간이 지나지 않았으면 false
        if (duration < MONITOR_WINDOW_MS) return false
        
        // 5분 동안 스크롤 20회 이상, 클릭 5회 이상 발생 시 탐닉으로 판단
        // 수치는 사용자 경험에 따라 조정 가능
        return scrollCount >= 20 && clickCount >= 5
    }

    fun reset() {
        scrollCount = 0
        clickCount = 0
        lastActivityAt = System.currentTimeMillis()
        windowStartedAt = System.currentTimeMillis()
    }
}
