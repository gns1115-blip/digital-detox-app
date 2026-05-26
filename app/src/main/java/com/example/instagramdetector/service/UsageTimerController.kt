package com.example.instagramdetector.service

import android.content.Context
import android.content.Intent
import android.os.Build

object UsageTimerController {

    fun start(context: Context, packageName: String, durationMinutes: Int) {
        val intent = Intent(context, UsageTimerService::class.java).apply {
            action = UsageTimerService.ACTION_START
            putExtra(UsageTimerService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(UsageTimerService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun cancel(context: Context) {
        val intent = Intent(context, UsageTimerService::class.java).apply {
            action = UsageTimerService.ACTION_CANCEL
        }
        context.startService(intent)
    }
}
