package com.example.instagramdetector.detox

import android.content.Context
import android.content.SharedPreferences

object DetoxPrefs {
    private const val PREFS_NAME = "detox_prefs"
    private const val KEY_DETOX_ENABLED = "detox_enabled"
    private const val KEY_TEMP_DISABLED_UNTIL_MS = "temp_disabled_until_ms"

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getUserToggleEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DETOX_ENABLED, true)
    }

    fun setUserToggleEnabled(context: Context, enabled: Boolean) {
        prefs(context)
            .edit()
            .putBoolean(KEY_DETOX_ENABLED, enabled)
            .apply()
    }

    fun isProtectionEnabled(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        if (!getUserToggleEnabled(context)) return false
        val until = prefs(context).getLong(KEY_TEMP_DISABLED_UNTIL_MS, 0L)
        return now >= until
    }

    fun tempDisabledUntilMs(context: Context): Long {
        return prefs(context).getLong(KEY_TEMP_DISABLED_UNTIL_MS, 0L)
    }

    fun remainingTempDisableMs(context: Context, now: Long = System.currentTimeMillis()): Long {
        val remaining = tempDisabledUntilMs(context) - now
        return if (remaining > 0L) remaining else 0L
    }

    fun disableForMinutes(context: Context, minutes: Int, now: Long = System.currentTimeMillis()) {
        val until = now + minutes * 60_000L
        prefs(context)
            .edit()
            .putLong(KEY_TEMP_DISABLED_UNTIL_MS, until)
            .apply()
    }

    fun clearTemporaryDisable(context: Context) {
        prefs(context)
            .edit()
            .putLong(KEY_TEMP_DISABLED_UNTIL_MS, 0L)
            .apply()
    }
}

