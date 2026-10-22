package io.github.gythialy.sporteditor.data

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
}
