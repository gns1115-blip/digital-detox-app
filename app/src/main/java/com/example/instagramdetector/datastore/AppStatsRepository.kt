package com.example.instagramdetector.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.appStatsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_daily_stats",
)

data class DailyAppStats(
    val todayDate: String,
    val todayUsageTimeMs: Long,
    val blockCount: Int,
    val lastBlockAtMillis: Long?,
)

class AppStatsRepository(
    private val context: Context,
) {
    private val dataStore = context.appStatsDataStore

    val dailyStats: Flow<DailyAppStats> = dataStore.data.map { preferences ->
        toDailyStats(preferences)
    }

    suspend fun addUsageTime(durationMs: Long) {
        if (durationMs <= 0L) return
        dataStore.edit { preferences ->
            ensureToday(preferences)
            val current = preferences[KEY_USAGE_TIME_MS] ?: 0L
            preferences[KEY_USAGE_TIME_MS] = current + durationMs
        }
    }

    suspend fun recordBlock(timestamp: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            ensureToday(preferences)
            val count = preferences[KEY_BLOCK_COUNT] ?: 0
            preferences[KEY_BLOCK_COUNT] = count + 1
            preferences[KEY_LAST_BLOCK_AT] = timestamp
        }
    }

    private fun ensureToday(preferences: androidx.datastore.preferences.core.MutablePreferences) {
        val today = todayString()
        val savedDate = preferences[KEY_DATE]
        if (savedDate != today) {
            preferences[KEY_DATE] = today
            preferences[KEY_USAGE_TIME_MS] = 0L
            preferences[KEY_BLOCK_COUNT] = 0
            preferences.remove(KEY_LAST_BLOCK_AT)
        }
    }

    private fun toDailyStats(preferences: Preferences): DailyAppStats {
        val today = todayString()
        val savedDate = preferences[KEY_DATE]
        if (savedDate != today) {
            return DailyAppStats(
                todayDate = today,
                todayUsageTimeMs = 0L,
                blockCount = 0,
                lastBlockAtMillis = null,
            )
        }
        return DailyAppStats(
            todayDate = today,
            todayUsageTimeMs = preferences[KEY_USAGE_TIME_MS] ?: 0L,
            blockCount = preferences[KEY_BLOCK_COUNT] ?: 0,
            lastBlockAtMillis = preferences[KEY_LAST_BLOCK_AT],
        )
    }

    private fun todayString(): String =
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    companion object {
        private val KEY_DATE = stringPreferencesKey("stats_date")
        private val KEY_USAGE_TIME_MS = longPreferencesKey("today_usage_time_ms")
        private val KEY_BLOCK_COUNT = intPreferencesKey("block_count")
        private val KEY_LAST_BLOCK_AT = longPreferencesKey("last_block_at")
    }
}
