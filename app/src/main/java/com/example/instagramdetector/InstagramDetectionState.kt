package com.example.instagramdetector

import com.example.instagramdetector.overlay.OverlayReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object InstagramDetectionState {
    private val _detectionCount = MutableStateFlow(0)
    val detectionCount: StateFlow<Int> = _detectionCount.asStateFlow()

    private val _lastDetectedAtMillis = MutableStateFlow<Long?>(null)
    val lastDetectedAtMillis: StateFlow<Long?> = _lastDetectedAtMillis.asStateFlow()

    private val _lastDetectedPackage = MutableStateFlow<String?>(null)
    val lastDetectedPackage: StateFlow<String?> = _lastDetectedPackage.asStateFlow()

    private val _lastSelectedReason = MutableStateFlow<OverlayReason?>(null)
    val lastSelectedReason: StateFlow<OverlayReason?> = _lastSelectedReason.asStateFlow()

    private val _lastReasonSelectedAtMillis = MutableStateFlow<Long?>(null)
    val lastReasonSelectedAtMillis: StateFlow<Long?> = _lastReasonSelectedAtMillis.asStateFlow()

    fun onInstagramOpened(packageName: String) {
        _detectionCount.value += 1
        _lastDetectedAtMillis.value = System.currentTimeMillis()
        _lastDetectedPackage.value = packageName
    }

    fun onReasonSelected(reason: OverlayReason, timestamp: Long, packageName: String) {
        _lastSelectedReason.value = reason
        _lastReasonSelectedAtMillis.value = timestamp
        _lastDetectedPackage.value = packageName
    }
}
