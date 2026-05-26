package com.example.instagramdetector.overlay

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.instagramdetector.util.canDrawOverlays

object OverlayController {

    fun show(context: Context, packageName: String) {
        if (!context.canDrawOverlays()) return
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
        }
        startOverlayService(context, intent)
    }

    fun showUsageTimeExpired(context: Context, packageName: String, blockSeconds: Int) {
        if (!context.canDrawOverlays()) return
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_USAGE_TIME_EXPIRED
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(OverlayService.EXTRA_BLOCK_DURATION_SECONDS, blockSeconds)
        }
        startOverlayService(context, intent)
    }

    fun showShortFormBreak(context: Context, packageName: String) {
        if (!context.canDrawOverlays()) return
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_SHORT_FORM_BREAK
            putExtra(OverlayService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(
                OverlayService.EXTRA_BLOCK_DURATION_SECONDS,
                OverlayService.DEFAULT_SCROLL_BLOCK_SECONDS,
            )
        }
        startOverlayService(context, intent)
    }

    fun hide(context: Context) {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_HIDE
        }
        context.startService(intent)
    }

    private fun startOverlayService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
