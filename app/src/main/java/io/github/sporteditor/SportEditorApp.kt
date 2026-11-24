package io.github.sporteditor

import android.app.Application
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import io.github.sporteditor.data.SettingsRepository
import io.github.sporteditor.debug.DebugLogLevel
import io.github.sporteditor.debug.DebugLogStore
import io.github.sporteditor.debug.DebugLogType
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Owns the connection to the LSPosed framework service and exposes it to the UI.
 */
class SportEditorApp :
    Application(),
    XposedServiceHelper.OnServiceListener {

    /**
     * Created lazily so a Direct Boot process never touches credential-protected storage
     * before the user unlocks the device.
     */
    val debugLogStore: DebugLogStore by lazy { DebugLogStore(this) }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this, debugLogStore)
    }

    override fun onCreate() {
        super.onCreate()
        debugLogStore.log(
            DebugLogType.FRAMEWORK,
            "app",
            "onCreate: version=" + BuildConfig.VERSION_NAME + " debug=" + BuildConfig.DEBUG
        )
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        val changed = service !== serviceHolder
        serviceHolder = service
        if (changed) {
            debugLogStore.log(DebugLogType.FRAMEWORK, "service", "framework service bound")
            notifyListeners(service)
        }
    }

    override fun onServiceDied(service: XposedService) {
        if (service === serviceHolder) {
            serviceHolder = null
            debugLogStore.log(
                DebugLogType.FRAMEWORK,
                DebugLogLevel.WARN,
                "service",
                "framework service died"
            )
            notifyListeners(null)
        }
    }

    companion object {
        @Volatile
        private var serviceHolder: XposedService? = null

        /**
         * Re-checks that the bound framework service still answers.
         *
         * Disabling the module in LSPosed or killing the framework does not always deliver
         * `onServiceDied` to a process that is already running, so the cached binder can outlive the
         * connection: the UI keeps claiming "connected" while nothing reaches the hooked apps.
         * Calling a trivial method on the service is the cheapest liveness probe — it throws
         * [XposedService.ServiceException] (a dead binder) instead of returning a value.
         */
        fun revalidateService(): Boolean {
            val holder = serviceHolder ?: return false
            // Two ways the answer can be "no": the binder is gone, or the framework is still
            // answering but this module is in no app's scope — remote preferences, and therefore
            // every setting, would reach nothing either way.
            val alive = runCatching { holder.apiVersion }.isSuccess
            val scoped = runCatching { holder.scope.any() }.getOrDefault(false)
            if (!alive || !scoped) {
                serviceHolder = if (alive) holder else null
                notifyListeners(null)
            }
            return alive && scoped
        }

        private val listeners = CopyOnWriteArraySet<(XposedService?) -> Unit>()

        val service: XposedService?
            get() = serviceHolder

        fun addServiceListener(listener: (XposedService?) -> Unit) {
            listeners += listener
            listener(serviceHolder)
        }

        fun removeServiceListener(listener: (XposedService?) -> Unit) {
            listeners -= listener
        }

        private fun notifyListeners(service: XposedService?) {
            listeners.forEach { it(service) }
        }
    }
}
