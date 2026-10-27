package io.github.sporteditor.data

/**
 * Keys shared by the module app and the hooked process through LSPosed remote preferences.
 */
object SettingsKeys {
    const val GROUP = "sport_editor_settings"

    fun enabled(target: AppTarget): String = "${target.key}.enabled"

    fun mode(target: AppTarget): String = "${target.key}.mode"

    fun value(target: AppTarget): String = "${target.key}.value"

    fun maxSteps(target: AppTarget): String = "${target.key}.max_steps"

    /** Random token used by the debug log broadcast channel (read-only in hooked apps). */
    const val DEBUG_TOKEN = "debug.token"

    /**
     * Minimum log severity captured by hooked processes; lower levels are dropped before the
     * broadcast. Stored as [io.github.sporteditor.debug.DebugLogLevel] name and read
     * live by the module, so a change takes effect without restarting target apps.
     */
    const val DEBUG_CAPTURE_LEVEL = "debug.capture_level"
}
