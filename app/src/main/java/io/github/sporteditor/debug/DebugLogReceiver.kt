package io.github.sporteditor.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.sporteditor.SportEditorApp

/**
 * Receives diagnostics from hooked processes. Exported only because the sender runs in
 * another app's process; every payload must carry the token stored in remote preferences.
 */
class DebugLogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? SportEditorApp ?: return
        app.debugLogStore.record(intent)
    }
}
