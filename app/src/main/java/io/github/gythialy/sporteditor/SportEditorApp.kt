package io.github.gythialy.sporteditor

import android.app.Application
import io.github.gythialy.sporteditor.data.SettingsRepository
import io.github.gythialy.sporteditor.debug.DebugLogLevel
import io.github.gythialy.sporteditor.debug.DebugLogStore
import io.github.gythialy.sporteditor.debug.DebugLogType
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
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
