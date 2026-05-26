package com.example.instagramdetector.service

import android.accessibilityservice.AccessibilityService

object AccessibilityServiceHolder {

    @Volatile
    var service: AccessibilityService? = null
        private set

    fun bind(service: AccessibilityService) {
        this.service = service
    }

    fun unbind() {
        service = null
    }
}
