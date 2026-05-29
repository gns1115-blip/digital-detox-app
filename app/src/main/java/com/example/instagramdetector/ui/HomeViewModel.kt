package com.example.instagramdetector.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramdetector.InstagramDetectorApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as InstagramDetectorApplication

    private val _currentTime = MutableStateFlow(LocalDateTime.now())
    val currentTime = _currentTime.asStateFlow()

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel = _batteryLevel.asStateFlow()

    val dailyStats = app.appStatsRepository.dailyStats

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            _batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
        }
    }

    init {
        // Clock update
        viewModelScope.launch {
            while (true) {
                _currentTime.value = LocalDateTime.now()
                delay(1000)
            }
        }

        // Battery update
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        application.registerReceiver(batteryReceiver, filter)
        
        // Initial battery
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            application.registerReceiver(null, ifilter)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        _batteryLevel.value = (level * 100 / scale.toFloat()).toInt()
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(batteryReceiver)
    }

    fun getFormattedTime(): String = _currentTime.value.format(DateTimeFormatter.ofPattern("HH:mm"))
    fun getFormattedDate(): String = _currentTime.value.format(DateTimeFormatter.ofPattern("M월 d일 (E)", java.util.Locale.KOREAN))
}
