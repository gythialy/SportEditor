package io.github.gythialy.sporteditor.hook

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores the per-day accumulation state in the target app's private preferences.
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
                lastRaw = prefs.getFloat(key + LAST_RAW, prefs.getFloat(key + BASELINE_RAW, 0f)),
                rawDeltaOffset = prefs.getFloat(key + RAW_DELTA_OFFSET, 0f),
                dayStartReported = prefs.getFloat(key + DAY_START_REPORTED, 0f),
                dailyLimit = prefs.getInt(key + DAILY_LIMIT, 0),
                lastReported = prefs.getFloat(key + LAST_REPORTED, 0f),
                autoCount = prefs.getInt(key + AUTO_COUNT, 0),
                version = prefs.getInt(key + VERSION, 0),
                lastElapsed = prefs.getLong(key + LAST_ELAPSED, 0L)
            )
        }.getOrNull()?.takeIf { it.dailyLimit > 0 }
    }

    override fun save(state: DailyState) {
        val prefs = preferences() ?: return
        prefs.edit()
            .putLong(key + EPOCH_DAY, state.epochDay)
            .putFloat(key + BASELINE_RAW, state.baselineRaw)
            .putFloat(key + LAST_RAW, state.lastRaw)
            .putFloat(key + RAW_DELTA_OFFSET, state.rawDeltaOffset)
            .putFloat(key + DAY_START_REPORTED, state.dayStartReported)
            .putInt(key + DAILY_LIMIT, state.dailyLimit)
            .putFloat(key + LAST_REPORTED, state.lastReported)
            .putInt(key + AUTO_COUNT, state.autoCount)
            .putInt(key + VERSION, state.version)
            .putLong(key + LAST_ELAPSED, state.lastElapsed)
            .apply()
    }

    private fun preferences(): SharedPreferences? = contextProvider()?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val PREFERENCES_NAME = "sporteditor_daily_state"
        const val EPOCH_DAY = ".epoch_day"
        const val BASELINE_RAW = ".baseline_raw"
        const val LAST_RAW = ".last_raw"
        const val RAW_DELTA_OFFSET = ".raw_delta_offset"
        const val DAY_START_REPORTED = ".day_start_reported"
        const val DAILY_LIMIT = ".daily_limit"
        const val LAST_REPORTED = ".last_reported"
        const val AUTO_COUNT = ".auto_count"
        const val VERSION = ".version"
        const val LAST_ELAPSED = ".last_elapsed"
    }
}
