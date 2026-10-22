package io.github.gythialy.sporteditor.hook

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.gythialy.sporteditor.data.AppTarget
import io.github.gythialy.sporteditor.data.SettingsKeys
import io.github.gythialy.sporteditor.debug.DebugLogLevel
import io.github.gythialy.sporteditor.debug.DebugLogSender
import io.github.gythialy.sporteditor.debug.DebugLogType
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * libxposed (modern Xposed API) entry point.
 *
 * Registered by name in `META-INF/xposed/java_init.list`; the framework instantiates it once
 * per hooked process.
 */
class SportEditorModule : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(
            Log.INFO,
            TAG,
            "loaded in ${param.processName}: framework=$frameworkName $frameworkVersion " +
                "(versionCode=$frameworkVersionCode, libApi=$apiVersion)"
        )
    }

    override fun onPackageReady(param: PackageReadyParam) {
        val target = AppTarget.fromPackageName(param.packageName) ?: return
        if (installedPackages.contains(param.packageName)) return

        val prefs = openRemotePreferences()
        if (prefs == null) {
            // Without remote preferences the module cannot know the user's configuration.
            // Do not silently rewrite steps using defaults the user never chose.
            log(Log.WARN, TAG, param.packageName + ": remote preferences unavailable, hooks skipped")
            return
        }

        debugPreferences = prefs
        debugTarget = param.packageName
        sendDebug(DebugLogType.HOOK, "package ready")

        val settings = HookSettings(prefs, target) { updated ->
            log(Log.INFO, TAG, param.packageName + ": config updated -> " + updated)
            sendDebug(DebugLogType.HOOK, "config updated -> " + updated)
        }
        log(Log.INFO, TAG, param.packageName + ": config at install -> " + settings.current())
        sendDebug(DebugLogType.HOOK, "config at install -> " + settings.current())

        runCatching {
            SensorHookInstaller(
                module = this,
                target = target,
                settings = settings,
                onContext = ::attachDebugContext,
                onStepEvent = { type, raw, reported ->
                    log(
                        Log.INFO,
                        TAG,
                        "step type=" + type + " raw=" + raw + " -> " + reported
                    )
                    sendDebug(
                        DebugLogType.HOOK,
                        "step type=" + type + " raw=" + raw + " -> " + reported
                    )
                }
            ).install(param.classLoader)
        }.onSuccess {
            // Only remember the package after a successful install so a retry is possible.
            installedPackages.add(param.packageName)
            sendDebug(DebugLogType.HOOK, "hooks installed")
        }.onFailure {
            log(Log.ERROR, TAG, "failed to install hooks for " + param.packageName, it)
            sendDebug(
                DebugLogType.ERROR,
                DebugLogLevel.ERROR,
                "hook install failed: " + it.message
            )
        }
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        // Re-installing hooks after hot reload would require replaying package callbacks, which
        // the framework does not do. Reject the reload and ask for a process restart instead.
        log(Log.WARN, TAG, "hot reload rejected; restart the target app to load the new module")
        sendDebug(
            DebugLogType.HOOK,
            DebugLogLevel.WARN,
            "hot reload rejected; restart the target app"
        )
        return false
    }

    /**
     * Called from SensorHookInstaller once SystemSensorManager exposes the app Context. The
     * sender is created lazily and any diagnostics buffered before that are flushed in order.
     */
    @Synchronized
    private fun attachDebugContext(context: Context) {
        if (debugSender != null) return
        val prefs = debugPreferences ?: return
        val sender = DebugLogSender(context, prefs)
        debugSender = sender
        val source = debugTarget ?: "hook"
        while (true) {
            val pending = synchronized(pendingDebug) { pendingDebug.pollFirst() } ?: break
            sender.send(pending.type, pending.level, source, pending.message)
        }
    }

    private fun sendDebug(type: DebugLogType, message: String) = sendDebug(type, DebugLogLevel.INFO, message)

    private fun sendDebug(type: DebugLogType, level: DebugLogLevel, message: String) {
        val sender = debugSender
        if (sender != null) {
            sender.send(type, level, debugTarget ?: "hook", message)
            return
        }
        synchronized(pendingDebug) {
            if (pendingDebug.size >= MAX_PENDING_LOGS) pendingDebug.pollFirst()
            pendingDebug.addLast(PendingDebug(type, level, message))
        }
    }

    private fun openRemotePreferences(): SharedPreferences? {
        if (frameworkProperties and XposedInterface.PROP_CAP_REMOTE == 0L) return null
        return runCatching { getRemotePreferences(SettingsKeys.GROUP) }.getOrElse {
            log(Log.WARN, TAG, "getRemotePreferences failed", it)
            null
        }
    }

    private data class PendingDebug(val type: DebugLogType, val level: DebugLogLevel, val message: String)

    @Volatile
    private var debugSender: DebugLogSender? = null

    @Volatile
    private var debugPreferences: SharedPreferences? = null

    @Volatile
    private var debugTarget: String? = null

    private val pendingDebug = ArrayDeque<PendingDebug>()

    private companion object {
        const val TAG = "SportEditor"
        const val MAX_PENDING_LOGS = 50

        /** Packages already hooked in this process. */
        val installedPackages: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }
}
