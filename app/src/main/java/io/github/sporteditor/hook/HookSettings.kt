package io.github.sporteditor.hook

import android.content.SharedPreferences
import io.github.sporteditor.data.AppTarget
import io.github.sporteditor.data.SettingsKeys
import io.github.sporteditor.data.StepMode
import io.github.sporteditor.data.TargetSettings

/**
 * Snapshot of the settings for one hooked package.
 *
 * LSPosed remote preferences are read-only inside the target process. A change listener keeps
 * the snapshot in sync while the target app is running.
 */
internal class HookSettings(
    private val prefs: SharedPreferences?,
    private val target: AppTarget,
    private val onChanged: (TargetSettings) -> Unit = {}
) {
    @Volatile
    private var snapshot: TargetSettings = target.defaultSettings

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    init {
        refresh()
        prefs?.let { preferences ->
            runCatching { preferences.registerOnSharedPreferenceChangeListener(listener) }
        }
    }

    fun current(): TargetSettings = snapshot

    private fun refresh() {
        val preferences = prefs ?: return
        runCatching {
            val defaults = target.defaultSettings
            val next = target.sanitize(
                TargetSettings(
                    enabled = preferences.getBoolean(SettingsKeys.enabled(target), defaults.enabled),
                    mode = StepMode.fromStorageKey(
                        preferences.getString(SettingsKeys.mode(target), null),
                        defaults.mode
                    ),
                    value = preferences.getInt(SettingsKeys.value(target), defaults.value),
                    maxSteps = preferences.getInt(SettingsKeys.maxSteps(target), defaults.maxSteps)
                ).normalized()
            )
            if (next != snapshot) {
                snapshot = next
                onChanged(next)
            }
        }
    }
}
