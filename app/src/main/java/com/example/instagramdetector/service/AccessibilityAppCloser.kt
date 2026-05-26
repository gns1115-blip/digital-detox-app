package com.example.instagramdetector.service

import android.accessibilityservice.AccessibilityService

/**
 * GLOBAL_ACTION_HOME을 사용해 대상 앱을 안정적으로 백그라운드로 보냅니다.
 * (BACK 반복보다 홈 이동이 기기·앱별로 일관됩니다.)
 */
object AccessibilityAppCloser {

    fun goHome(): Boolean {
        val service = AccessibilityServiceHolder.service ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }
}
