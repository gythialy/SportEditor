package io.github.sporteditor.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.libxposed.service.XposedService
import io.github.sporteditor.SportEditorApp
import io.github.sporteditor.debug.DebugLogLevel
import io.github.sporteditor.debug.DebugLogStore
import io.github.sporteditor.debug.DebugLogType
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable snapshot rendered by the settings screen.
 */
data class SettingsState(
    /** True only when remote preferences are in use, i.e. changes reach the hooked apps. */
    val connected: Boolean = false,
    val frameworkName: String? = null,
    val frameworkVersion: String? = null,
    val apiVersion: Int? = null,
    val targets: Map<AppTarget, TargetSettings> = emptyMap(),
    /** Minimum severity captured by hooked processes; lower levels are dropped on the hook. */
    val captureLevel: DebugLogLevel = DebugLogLevel.INFO
) {
    fun settingsFor(target: AppTarget): TargetSettings = targets[target] ?: target.defaultSettings
}

/**
 * Single source of truth for per-app settings.
 *
 * Settings are stored in LSPosed remote preferences so that the hooked app can read them.
 * While the framework service is unavailable, a local preference file keeps the UI usable and
 * records which targets were edited; those targets are pushed to remote preferences on reconnect.
 * Only dirty targets are pushed, so untouched remote values are never overwritten.
 */
class SettingsRepository(context: Context, private val debugLog: DebugLogStore) {

    private val localPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(LOCAL_PREFERENCES, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(SettingsState(captureLevel = debugLog.captureLevel.value))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bindingGeneration = AtomicInteger()

    @Volatile
    private var preferences: SharedPreferences = localPreferences

    @Volatile
    private var boundService: XposedService? = null

    @Volatile
    private var remoteActive = false

    @Volatile
    private var frameworkName: String? = null

    @Volatile
    private var frameworkVersion: String? = null

    @Volatile
    private var apiVersion: Int? = null

    /** Capture threshold exposed to the diagnostics screen. */
    private val _captureLevel = MutableStateFlow(debugLog.captureLevel.value)
    val captureLevel: StateFlow<DebugLogLevel> = _captureLevel.asStateFlow()

    private val changeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    init {
        debugLog.log(DebugLogType.CONFIG, "settings", "repository initialized")
        runCatching { localPreferences.registerOnSharedPreferenceChangeListener(changeListener) }
        refresh()
        SportEditorApp.addServiceListener(::onServiceChanged)
    }

    @Synchronized
    fun update(target: AppTarget, transform: (TargetSettings) -> TargetSettings) {
        val next = target.sanitize(transform(_state.value.settingsFor(target)).normalized())
        val store = preferences

        runCatching {
            store.edit {
                putBoolean(SettingsKeys.enabled(target), next.enabled)
                putString(SettingsKeys.mode(target), next.mode.storageKey)
                putInt(SettingsKeys.value(target), next.value)
                putInt(SettingsKeys.maxSteps(target), next.maxSteps)
            }
        }.onFailure {
            debugLog.log(
                DebugLogType.HOOK,
                DebugLogLevel.ERROR,
                "settings",
                "write failed for " + target.key + ": " + it.message
            )
        }

        debugLog.log(
            DebugLogType.CONFIG,
            "settings",
            target.key + ": enabled=" + next.enabled + " mode=" + next.mode.storageKey +
                " value=" + next.value
        )

        if (store === localPreferences) {
            markPendingLocked(target)
        }
        _state.update { it.copy(targets = it.targets + (target to next)) }
    }

    fun reset(target: AppTarget) {
        update(target) { target.defaultSettings }
    }

    /**
     * Updates the capture threshold: it is stored in the active preference store (remote when
     * bound, otherwise local with a push on the next connection) and is applied to the store
     * immediately, so both in-app logging and hooked processes follow without a restart.
     */
    fun setCaptureLevel(level: DebugLogLevel) {
        val store = preferences
        runCatching {
            store.edit { putString(SettingsKeys.DEBUG_CAPTURE_LEVEL, level.name) }
        }
        if (store === localPreferences) {
            // Remember the offline choice: without this marker the next bind would read the stale
            // remote value and silently overwrite the level the user just picked.
            runCatching { localPreferences.edit { putString(KEY_PENDING_CAPTURE_LEVEL, level.name) } }
            // Reconnect pushes the locally chosen value into remote preferences.
            val remote = runCatching { boundService?.getRemotePreferences(SettingsKeys.GROUP) }.getOrNull()
            if (remote != null) {
                runCatching { remote.edit { putString(SettingsKeys.DEBUG_CAPTURE_LEVEL, level.name) } }
                    .onSuccess { localPreferences.edit { remove(KEY_PENDING_CAPTURE_LEVEL) } }
            }
        }
        debugLog.setCaptureLevel(level)
    }

    /**
     * Reads the capture level from [preferences]; null when that store has no value yet. Callers
     * fall back to [DebugLogStore.captureLevel], because the store owns the persisted threshold —
     * a missing key must not reset the user's choice to INFO before the framework binds.
     */
    private fun readCaptureLevel(preferences: SharedPreferences): DebugLogLevel? = runCatching {
        preferences.getString(SettingsKeys.DEBUG_CAPTURE_LEVEL, null)
            ?.let { name -> runCatching { DebugLogLevel.valueOf(name) }.getOrNull() }
    }.getOrNull()

    /**
     * Resolves the remote preference store and framework metadata off the main thread, then
     * applies the binding. Binder calls must not run on the UI thread.
     */
    private fun onServiceChanged(service: XposedService?) {
        debugLog.log(
            DebugLogType.FRAMEWORK,
            "service",
            if (service == null) "service changed: unbound" else "service changed: bound"
        )
        val generation = bindingGeneration.incrementAndGet()
        ioScope.launch {
            val remote = service?.let { runCatching { it.getRemotePreferences(SettingsKeys.GROUP) }.getOrNull() }
            val name = service?.let { runCatching { it.frameworkName }.getOrNull() }
            val version = service?.let { runCatching { it.frameworkVersion }.getOrNull() }
            val api = service?.let { runCatching { it.apiVersion }.getOrNull() }

            // A newer bind/died event superseded this one while its binder calls were running.
            if (bindingGeneration.get() != generation) return@launch
            applyBinding(service, remote, name, version, api)
        }
    }

    @Synchronized
    private fun applyBinding(service: XposedService?, remote: SharedPreferences?, name: String?, version: String?, api: Int?) {
        if (service === boundService && remoteActive == (remote != null)) return

        boundService = service
        runCatching { preferences.unregisterOnSharedPreferenceChangeListener(changeListener) }

        if (remote != null) {
            migrateLocalSettingsLocked(remote)
            migrateCaptureLevelLocked(remote)
            syncPendingLocalSettingsLocked(remote)
            syncPendingCaptureLevelLocked(remote)
            preferences = remote
            remoteActive = true
            if (remote.getString(SettingsKeys.DEBUG_TOKEN, null) != debugLog.token) {
                runCatching {
                    remote.edit { putString(SettingsKeys.DEBUG_TOKEN, debugLog.token) }
                }.onSuccess {
                    debugLog.log(
                        DebugLogType.CONFIG,
                        "settings",
                        "debug token published to remote preferences"
                    )
                }.onFailure {
                    debugLog.log(
                        DebugLogType.CONFIG,
                        DebugLogLevel.ERROR,
                        "settings",
                        "failed to publish debug token: " + it.message
                    )
                }
            }
            debugLog.log(
                DebugLogType.FRAMEWORK,
                "framework",
                "connected: " + (name ?: "?") + " " + (version ?: "?") + " API " + (api ?: "?")
            )
        } else {
            preferences = localPreferences
            remoteActive = false
            debugLog.log(
                DebugLogType.FRAMEWORK,
                DebugLogLevel.WARN,
                "framework",
                "disconnected; local preferences active"
            )
        }

        frameworkName = name
        frameworkVersion = version
        apiVersion = api

        runCatching { preferences.registerOnSharedPreferenceChangeListener(changeListener) }
        refresh()
    }

    /**
     * First connection: import local values that exist, without inventing defaults for targets
     * the user never touched.
     */
    private fun migrateLocalSettingsLocked(remote: SharedPreferences) {
        if (remote.contains(KEY_MIGRATED)) return

        val localTargets = AppTarget.entries.filter { localPreferences.contains(SettingsKeys.value(it)) }
        remote.edit {
            localTargets.forEach { target -> putTarget(target) }
            putBoolean(KEY_MIGRATED, true)
        }
        debugLog.log(
            DebugLogType.CONFIG,
            "settings",
            "migrated " + localTargets.size + " target(s) from local preferences"
        )
    }

    /** Carries a locally chosen capture level into the fresh remote store once. */
    private fun migrateCaptureLevelLocked(remote: SharedPreferences) {
        if (remote.contains(SettingsKeys.DEBUG_CAPTURE_LEVEL)) return
        if (!localPreferences.contains(SettingsKeys.DEBUG_CAPTURE_LEVEL)) return

        val level = readCaptureLevel(localPreferences) ?: DebugLogLevel.INFO
        runCatching {
            remote.edit { putString(SettingsKeys.DEBUG_CAPTURE_LEVEL, level.name) }
        }
        debugLog.setCaptureLevel(level)
        debugLog.log(DebugLogType.CONFIG, "settings", "migrated capture level from local preferences")
    }

    /** Pushes only the targets edited while the framework service was unreachable. */
    private fun syncPendingLocalSettingsLocked(remote: SharedPreferences) {
        val pendingKeys = localPreferences.getStringSet(KEY_PENDING_TARGETS, null)?.toSet().orEmpty()
        if (pendingKeys.isEmpty()) return

        val pendingTargets = pendingKeys.mapNotNull(AppTarget::fromKey)
        if (pendingTargets.isNotEmpty()) {
            remote.edit { pendingTargets.forEach { target -> putTarget(target) } }
            debugLog.log(
                DebugLogType.CONFIG,
                "settings",
                "synced " + pendingTargets.size + " pending target(s) to remote preferences"
            )
        }
        localPreferences.edit { remove(KEY_PENDING_TARGETS) }
    }

    /** Pushes a capture level chosen while the framework was unreachable. */
    private fun syncPendingCaptureLevelLocked(remote: SharedPreferences) {
        val pending = localPreferences.getString(KEY_PENDING_CAPTURE_LEVEL, null) ?: return
        val level = runCatching { DebugLogLevel.valueOf(pending) }.getOrNull() ?: return
        remote.edit { putString(SettingsKeys.DEBUG_CAPTURE_LEVEL, level.name) }
        localPreferences.edit { remove(KEY_PENDING_CAPTURE_LEVEL) }
        debugLog.log(
            DebugLogType.CONFIG,
            "settings",
            "synced capture level=" + level.name + " to remote preferences"
        )
    }

    private fun SharedPreferences.Editor.putTarget(target: AppTarget) {
        val defaults = target.defaultSettings
        putBoolean(
            SettingsKeys.enabled(target),
            localPreferences.getBoolean(SettingsKeys.enabled(target), defaults.enabled)
        )
        putString(
            SettingsKeys.mode(target),
            localPreferences.getString(SettingsKeys.mode(target), defaults.mode.storageKey)
        )
        putInt(
            SettingsKeys.value(target),
            localPreferences.getInt(SettingsKeys.value(target), defaults.value)
        )
        putInt(
            SettingsKeys.maxSteps(target),
            localPreferences.getInt(SettingsKeys.maxSteps(target), defaults.maxSteps)
        )
    }

    private fun markPendingLocked(target: AppTarget) {
        val pending = localPreferences.getStringSet(KEY_PENDING_TARGETS, null)
            ?.toMutableSet()
            ?: mutableSetOf()
        pending.add(target.key)
        localPreferences.edit { putStringSet(KEY_PENDING_TARGETS, pending) }
    }

    private fun refresh() {
        val captureLevel = readCaptureLevel(preferences) ?: debugLog.captureLevel.value
        if (captureLevel != debugLog.captureLevel.value) {
            debugLog.setCaptureLevel(captureLevel)
        }
        _captureLevel.value = captureLevel
        _state.update { previous ->
            previous.copy(
                connected = remoteActive,
                frameworkName = frameworkName,
                frameworkVersion = frameworkVersion,
                apiVersion = apiVersion,
                targets = AppTarget.entries.associateWith(::read),
                captureLevel = captureLevel
            )
        }
    }

    private fun read(target: AppTarget): TargetSettings {
        val defaults = target.defaultSettings
        val settings = TargetSettings(
            enabled = runCatching {
                preferences.getBoolean(SettingsKeys.enabled(target), defaults.enabled)
            }.getOrDefault(defaults.enabled),
            mode = runCatching {
                StepMode.fromStorageKey(
                    preferences.getString(SettingsKeys.mode(target), null),
                    defaults.mode
                )
            }.getOrDefault(defaults.mode),
            value = runCatching {
                preferences.getInt(SettingsKeys.value(target), defaults.value)
            }.getOrDefault(defaults.value),
            maxSteps = runCatching {
                preferences.getInt(SettingsKeys.maxSteps(target), defaults.maxSteps)
            }.getOrDefault(defaults.maxSteps)
        ).normalized()
        return target.sanitize(settings)
    }

    private companion object {
        const val LOCAL_PREFERENCES = "local_settings"
        const val KEY_MIGRATED = "migrated_from_local"
        const val KEY_PENDING_TARGETS = "pending_targets"
        const val KEY_PENDING_CAPTURE_LEVEL = "pending_capture_level"
    }
}
