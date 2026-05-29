package com.example.instagramdetector.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.monitoredAppsDataStore: DataStore<Preferences> by preferencesDataStore(name = "monitored_apps")

class MonitoredAppsRepository(context: Context) {
    private val dataStore = context.monitoredAppsDataStore
    private val KEY_MONITORED_PACKAGES = stringSetPreferencesKey("monitored_packages")

    val monitoredPackages: Flow<Set<String>> = dataStore.data.map { 
        it[KEY_MONITORED_PACKAGES] ?: emptySet() 
    }

    suspend fun addApp(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_MONITORED_PACKAGES] ?: emptySet()
            prefs[KEY_MONITORED_PACKAGES] = current + packageName
        }
    }

    suspend fun removeApp(packageName: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_MONITORED_PACKAGES] ?: emptySet()
            prefs[KEY_MONITORED_PACKAGES] = current - packageName
        }
    }

    suspend fun isMonitored(packageName: String): Boolean {
        return monitoredPackages.first().contains(packageName)
    }
}
