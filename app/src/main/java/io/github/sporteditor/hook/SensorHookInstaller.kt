package io.github.sporteditor.hook

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.os.SystemClock
import android.util.Log
import android.util.SparseArray
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.sporteditor.data.AppTarget
import io.github.sporteditor.data.TargetSettings
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Installs the step-sensor hooks for one hooked package.
 *
 * Two complementary hooks are used:
 *  - [android.hardware.SystemSensorManager] listener registration records handle -> sensor type.
 *  - [dispatchSensorEvent][android.hardware.SystemSensorManager.SensorEventQueue] rewrites step values.
 *
 * The registration hook is the primary source of the handle mapping; reading the queue's
 * mSensorsEvents field is a fallback for events delivered before the mapping was seen.
 *
 * These framework classes are intentionally hidden from the public SDK; LSPosed grants modules
 * a hidden-API exemption, which is why the lint check is suppressed here.
 */
@SuppressLint("PrivateApi")
internal class SensorHookInstaller(
    private val module: XposedModule,
    private val target: AppTarget,
    private val settings: HookSettings,
    private val onContext: ((Context) -> Unit)? = null,
    private val onSensorRegistered: ((type: Int, handle: Int) -> Unit)? = null,
    private val onStepSample: ((StepSample) -> Unit)? = null,
    private val onStepSummary: ((StepSummary) -> Unit)? = null
) {
    private val transformers = ConcurrentHashMap<Int, StepTransformer>()
    private val sensorTypes = ConcurrentHashMap<Int, Int>()
    private val stepEvents = AtomicInteger()
    private val sampler = StepLogSampler()
    private val loggedSensors = java.util.Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    @Volatile
    private var contextLogged = false

    @Volatile
    private var appContext: Context? = null

    fun install(classLoader: ClassLoader) {
        // Each phase is isolated: a reflection failure in one must not prevent the other.
        runCatching { hookContextCapture(classLoader) }
            .onFailure { module.log(Log.WARN, TAG, "context capture hook failed", it) }
        runCatching { hookListenerRegistration(classLoader) }
            .onFailure { module.log(Log.ERROR, TAG, "listener registration hook failed", it) }
        runCatching { hookSensorQueue(classLoader) }
            .onFailure { module.log(Log.ERROR, TAG, "sensor queue hook failed", it) }
        module.log(Log.INFO, TAG, "hooks installed for ${target.packageName}")
    }

    /**
     * SystemSensorManager holds the app Context. Capturing it here avoids hidden-API
     * reflection on ActivityThread and lets the module send diagnostics via broadcast.
     */
    private fun hookContextCapture(classLoader: ClassLoader) {
        if (onContext == null) return
        val managerClass = runCatching {
            Class.forName("android.hardware.SystemSensorManager", false, classLoader)
        }.getOrNull() ?: return
        val contextField = managerClass.declaredFields
            .firstOrNull { Context::class.java.isAssignableFrom(it.type) }
            ?.apply { isAccessible = true }
            ?: return

        val captureMethods = managerClass.declaredMethods.filter {
            it.name == "getDefaultSensor" || it.name == "getSensorList"
        }
        module.log(
            Log.INFO,
            TAG,
            "context field=" + contextField.name + ", capture methods=" + captureMethods.size
        )
        captureMethods.forEach { method ->
            runCatching {
                module.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        val context = runCatching {
                            contextField.get(chain.thisObject) as? Context
                        }.getOrNull()
                        if (context != null) {
                            appContext = context
                            if (!contextLogged) {
                                contextLogged = true
                                module.log(Log.INFO, TAG, "captured context from sensor manager")
                            }
                            onContext?.invoke(context)
                        }
                        chain.proceed()
                    }
            }.onFailure {
                module.log(Log.WARN, TAG, "failed to hook context capture method", it)
            }
        }
    }

    private fun hookListenerRegistration(classLoader: ClassLoader) {
        val managerClass = runCatching {
            Class.forName("android.hardware.SystemSensorManager", false, classLoader)
        }.getOrNull()
        if (managerClass == null) {
            module.log(Log.WARN, TAG, "SystemSensorManager not found in ${target.packageName}")
            return
        }
        val contextField = managerClass.declaredFields
            .firstOrNull { Context::class.java.isAssignableFrom(it.type) }
            ?.apply { isAccessible = true }

        val methods = runCatching {
            managerClass.declaredMethods.filter { it.name.startsWith("registerListener") }
        }.getOrElse {
            module.log(Log.WARN, TAG, "cannot enumerate listener methods", it)
            return
        }

        methods.forEach { method ->
            val sensorIndex = runCatching {
                method.parameterTypes.indexOfFirst { it == Sensor::class.java }
            }.getOrDefault(-1)
            if (sensorIndex < 0) return@forEach

            runCatching {
                module.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        if (onContext != null && contextField != null) {
                            val context = runCatching {
                                contextField.get(chain.thisObject) as? Context
                            }.getOrNull()
                            if (context != null) {
                                appContext = context
                                onContext?.invoke(context)
                            }
                        }
                        val sensor = chain.args.getOrNull(sensorIndex) as? Sensor
                        if (sensor != null) {
                            // Sensor#getHandle is a stable framework API but hidden from the
                            // public SDK stubs, so it is resolved reflectively.
                            sensorHandle(sensor)?.let { handle ->
                                sensorTypes[handle] = sensor.type
                                if (isStepType(sensor.type) && loggedSensors.add(handle)) {
                                    onSensorRegistered?.invoke(sensor.type, handle)
                                }
                            }
                        }
                        chain.proceed()
                    }
            }.onFailure {
                module.log(Log.WARN, TAG, "failed to hook ${method.name}", it)
            }
        }
    }

    private fun hookSensorQueue(classLoader: ClassLoader) {
        val queueClass = runCatching {
            Class.forName("android.hardware.SystemSensorManager\$SensorEventQueue", false, classLoader)
        }.getOrNull()
        if (queueClass == null) {
            module.log(Log.WARN, TAG, "SensorEventQueue not found in ${target.packageName}")
            return
        }

        val eventsField = runCatching {
            queueClass.getDeclaredField("mSensorsEvents").apply { isAccessible = true }
        }.getOrNull()
        if (eventsField == null) {
            module.log(Log.WARN, TAG, "mSensorsEvents unavailable; relying on listener registration")
        }

        val dispatchMethods = runCatching {
            queueClass.declaredMethods.filter {
                it.name == "dispatchSensorEvent" && it.parameterTypes.size == 4
            }
        }.getOrElse {
            module.log(Log.WARN, TAG, "cannot enumerate SensorEventQueue methods", it)
            return
        }
        if (dispatchMethods.isEmpty()) {
            module.log(Log.WARN, TAG, "dispatchSensorEvent not found in ${target.packageName}")
            return
        }

        dispatchMethods.forEach { method ->
            runCatching {
                module.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept { chain ->
                        rewriteStepValue(chain.args, chain.thisObject, eventsField)
                        chain.proceed()
                    }
            }.onFailure {
                module.log(Log.WARN, TAG, "failed to hook dispatchSensorEvent", it)
            }
        }
    }

    private fun rewriteStepValue(args: List<Any?>, queue: Any?, eventsField: Field?) {
        val handle = args.getOrNull(0) as? Int ?: return
        val values = args.getOrNull(1) as? FloatArray ?: return
        if (values.isEmpty()) return

        val type = resolveSensorType(handle, queue, eventsField)
        if (type != Sensor.TYPE_STEP_COUNTER && type != Sensor.TYPE_STEP_DETECTOR) return

        val current = settings.current()
        if (!current.enabled) return

        // Counter and detector are independent sensors; keep separate auto-increment state.
        val transformer = transformers.getOrPut(type) {
            StepTransformer(
                store = SharedPreferencesDailyStateStore(
                    contextProvider = { appContext },
                    key = target.key + "." + type
                ),
                incremental = type == Sensor.TYPE_STEP_DETECTOR,
                elapsedRealtime = SystemClock::elapsedRealtime
            )
        }
        val raw = values[0]
        values[0] = transformer.transform(raw, current)
        val reported = values[0]
        val count = stepEvents.incrementAndGet()
        // Verbose samples for the first few events, then a low-rate summary so diagnostics can
        // keep following a long-running app without flooding the log ring buffer.
        when (sampler.record(SystemClock.elapsedRealtime())) {
            StepLogSampler.Action.VERBOSE -> onStepSample?.invoke(
                StepSample(
                    sensorType = type,
                    raw = raw,
                    reported = reported,
                    settings = current
                )
            )

            StepLogSampler.Action.SUMMARY -> onStepSummary?.invoke(StepSummary(type, count, raw, reported))

            StepLogSampler.Action.NONE -> Unit
        }
    }

    private fun sensorHandle(sensor: Sensor): Int? = runCatching {
        Sensor::class.java.getMethod("getHandle").invoke(sensor) as Int
    }.getOrNull()

    private fun isStepType(type: Int): Boolean = type == Sensor.TYPE_STEP_COUNTER || type == Sensor.TYPE_STEP_DETECTOR

    private fun resolveSensorType(handle: Int, queue: Any?, eventsField: Field?): Int {
        sensorTypes[handle]?.let { return it }

        val resolved = runCatching {
            val events = eventsField?.get(queue) as? SparseArray<*>
            (events?.get(handle) as? SensorEvent)?.sensor?.type
        }.getOrNull()

        if (resolved != null && resolved > 0) {
            sensorTypes[handle] = resolved
        }
        return resolved ?: -1
    }

    private companion object {
        const val TAG = "SportEditor"
    }
}

/**
 * One verbose step event as delivered by the target app's sensor queue after rewriting.
 * Field values mirror the log line so diagnostics can parse it back.
 */
internal data class StepSample(val sensorType: Int, val raw: Float, val reported: Float, val settings: TargetSettings)

/** Low-rate aggregate emitted after the verbose per-event samples are used up. */
internal data class StepSummary(val sensorType: Int, val events: Int, val raw: Float, val reported: Float)

internal fun sensorTypeName(type: Int): String = when (type) {
    android.hardware.Sensor.TYPE_STEP_COUNTER -> "STEP_COUNTER"
    android.hardware.Sensor.TYPE_STEP_DETECTOR -> "STEP_DETECTOR"
    else -> "TYPE_" + type
}
