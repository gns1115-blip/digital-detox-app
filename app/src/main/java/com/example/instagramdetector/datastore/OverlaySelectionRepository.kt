package com.example.instagramdetector.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.instagramdetector.overlay.OverlayReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.overlaySelectionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "overlay_selection_history",
)

class OverlaySelectionRepository(
    private val context: Context,
) {
    private val dataStore = context.overlaySelectionDataStore

    val recentRecords: Flow<List<OverlaySelectionRecord>> = dataStore.data.map { preferences ->
        decodeRecords(preferences[HISTORY_JSON_KEY].orEmpty())
    }

    suspend fun saveRecord(record: OverlaySelectionRecord) {
        dataStore.edit { preferences ->
            val updated = buildList {
                add(record)
                addAll(decodeRecords(preferences[HISTORY_JSON_KEY].orEmpty()))
            }.take(MAX_RECORDS)
            preferences[HISTORY_JSON_KEY] = encodeRecords(updated)
        }
    }

    companion object {
        private val HISTORY_JSON_KEY = stringPreferencesKey("history_json")
        private const val MAX_RECORDS = 10
    }
}

private fun encodeRecords(records: List<OverlaySelectionRecord>): String {
    val array = JSONArray()
    records.forEach { record ->
        array.put(
            JSONObject().apply {
                put("timestamp", record.timestamp)
                put("packageName", record.packageName)
                put("selectedReason", record.selectedReason.name)
            },
        )
    }
    return array.toString()
}

private fun decodeRecords(json: String): List<OverlaySelectionRecord> {
    if (json.isBlank()) return emptyList()

    return runCatching {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val reasonName = item.getString("selectedReason")
                val reason = OverlayReason.entries.firstOrNull { it.name == reasonName }
                    ?: continue
                add(
                    OverlaySelectionRecord(
                        timestamp = item.getLong("timestamp"),
                        packageName = item.getString("packageName"),
                        selectedReason = reason,
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())
}
