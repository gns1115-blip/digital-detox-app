package com.example.instagramdetector.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramdetector.InstagramDetectorApplication
import com.example.instagramdetector.util.AppInfo
import com.example.instagramdetector.util.AppInfoProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as InstagramDetectorApplication
    private val collator = Collator.getInstance(Locale.KOREAN)

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val monitoredPackages = app.monitoredAppsRepository.monitoredPackages

    val filteredApps: StateFlow<List<AppInfo>> = combine(_allApps, _searchQuery) { apps, query ->
        if (query.isBlank()) apps else apps.filter { it.name.contains(query, ignoreCase = true) }
    }.combine(monitoredPackages) { filtered, monitored ->
        // Sort: Monitored first, then alphabetical (KOR -> ENG)
        filtered.sortedWith(
            compareByDescending<AppInfo> { monitored.contains(it.packageName) }
                .thenBy(collator) { it.name }
        )
    }.run {
        val state = MutableStateFlow<List<AppInfo>>(emptyList())
        viewModelScope.launch { collect { state.value = it } }
        state.asStateFlow()
    }

    init {
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch {
            _allApps.value = AppInfoProvider.getInstalledApps(getApplication())
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleMonitoring(packageName: String) {
        viewModelScope.launch {
            val current = app.monitoredAppsRepository.monitoredPackages.run {
                var set = emptySet<String>()
                val job = launch { collect { set = it } }
                delay(20)
                job.cancel()
                set
            }
            if (current.contains(packageName)) {
                app.monitoredAppsRepository.removeApp(packageName)
            } else {
                app.monitoredAppsRepository.addApp(packageName)
            }
        }
    }
    
    private suspend fun kotlinx.coroutines.flow.Flow<Set<String>>.getCurrentValue(): Set<String> {
        var value = emptySet<String>()
        val job = viewModelScope.launch {
            collect { value = it }
        }
        kotlinx.coroutines.delay(10)
        job.cancel()
        return value
    }
}
