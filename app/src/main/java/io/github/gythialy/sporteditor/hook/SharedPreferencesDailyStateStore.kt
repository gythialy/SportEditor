package io.github.gythialy.sporteditor.hook

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores the daily baseline in the target app's private preferences.
 *
 * A separate preference file is used so the module never touches the host app's own keys.
 * [contextProvider] is evaluated lazily because the framework context is captured after the
 * hook installer has been created.
 */
internal class SharedPreferencesDailyStateStore(private val contextProvider: () -> Context?, private val key: String) : DailyStateStore {
    override fun load(): DailyState? {
        val prefs = preferences() ?: return null
        if (!prefs.contains(key + EPOCH_DAY)) return null
        return runCatching {
            DailyState(
                epochDay = prefs.getLong(key + EPOCH_DAY, 0L),
                baselineRaw = prefs.getFloat(key + BASELINE_RAW, 0f),
                dayStartReported = prefs.getFloat(key + DAY_START_REPORTED, 0f),
                dailyLimit = prefs.getInt(key + DAILY_LIMIT, 0),
                lastReported = prefs.getFloat(key + LAST_REPORTED, 0f),
                autoCount = prefs.getInt(key + AUTO_COUNT, 0)
            )
        }.getOrNull()?.takeIf { it.dailyLimit > 0 }
    }

    override fun save(state: DailyState) {
        val prefs = preferences() ?: return
        prefs.edit()
            .putLong(key + EPOCH_DAY, state.epochDay)
            .putFloat(key + BASELINE_RAW, state.baselineRaw)
            .putFloat(key + DAY_START_REPORTED, state.dayStartReported)
            .putInt(key + DAILY_LIMIT, state.dailyLimit)
            .putFloat(key + LAST_REPORTED, state.lastReported)
            .putInt(key + AUTO_COUNT, state.autoCount)
            .apply()
    }

    private fun preferences(): SharedPreferences? = contextProvider()?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val PREFERENCES_NAME = "sporteditor_daily_state"
        const val EPOCH_DAY = ".epoch_day"
        const val BASELINE_RAW = ".baseline_raw"
        const val DAY_START_REPORTED = ".day_start_reported"
        const val DAILY_LIMIT = ".daily_limit"
        const val LAST_REPORTED = ".last_reported"
        const val AUTO_COUNT = ".auto_count"
    }
}
