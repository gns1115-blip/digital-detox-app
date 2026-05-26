package com.example.instagramdetector.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.example.instagramdetector.service.AppAccessibilityService

fun Context.isAppAccessibilityServiceEnabled(): Boolean {
    val enabledServices = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val expectedComponent = ComponentName(
        this,
        AppAccessibilityService::class.java,
    ).flattenToString()

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    while (splitter.hasNext()) {
        val component = splitter.next()
        if (component.equals(expectedComponent, ignoreCase = true)) {
            return true
        }
    }
    return false
}

/** @deprecated Use [isAppAccessibilityServiceEnabled] */
fun Context.isInstagramAccessibilityServiceEnabled(): Boolean =
    isAppAccessibilityServiceEnabled()
