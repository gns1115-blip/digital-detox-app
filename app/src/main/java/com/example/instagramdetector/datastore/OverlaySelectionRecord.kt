package com.example.instagramdetector.datastore

import com.example.instagramdetector.overlay.OverlayReason

data class OverlaySelectionRecord(
    val timestamp: Long,
    val packageName: String,
    val selectedReason: OverlayReason,
)
