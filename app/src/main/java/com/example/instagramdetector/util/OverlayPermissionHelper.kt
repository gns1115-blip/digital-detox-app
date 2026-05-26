package com.example.instagramdetector.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun Context.canDrawOverlays(): Boolean = Settings.canDrawOverlays(this)

fun Context.createManageOverlayPermissionIntent(): Intent =
    Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
