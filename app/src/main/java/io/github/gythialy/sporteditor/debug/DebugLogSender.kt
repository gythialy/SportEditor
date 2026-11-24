package io.github.gythialy.sporteditor.debug

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import io.github.gythialy.sporteditor.data.SettingsKeys

/**
 * Sends hook-process diagnostics to the module app. Lives in the injected process; the
 * Context is captured from SystemSensorManager because libxposed exposes no application
 * context of its own.
 */
class DebugLogSender(private val context: Context, private val preferences: SharedPreferences) {

    fun send(type: DebugLogType, level: DebugLogLevel, source: String, message: String) {
        val token = runCatching {
            preferences.getString(SettingsKeys.DEBUG_TOKEN, null)
        }.getOrNull() ?: return

        val intent = Intent().apply {
            component = ComponentName(
                DebugLogProtocol.MODULE_PACKAGE,
                DebugLogProtocol.RECEIVER_CLASS
            )
            putExtra(DebugLogProtocol.EXTRA_TOKEN, token)
            putExtra(DebugLogProtocol.EXTRA_TYPE, type.name)
            putExtra(DebugLogProtocol.EXTRA_LEVEL, level.name)
            putExtra(DebugLogProtocol.EXTRA_SOURCE, source)
            putExtra(DebugLogProtocol.EXTRA_MESSAGE, message)
        }
        runCatching { context.sendBroadcast(intent) }.onFailure {
            android.util.Log.w("SportEditor", "debug broadcast failed", it)
        }
    }

    fun send(type: DebugLogType, source: String, message: String) = send(type, DebugLogLevel.INFO, source, message)
}
