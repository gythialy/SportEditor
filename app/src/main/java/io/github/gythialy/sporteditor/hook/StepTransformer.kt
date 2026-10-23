package io.github.gythialy.sporteditor.hook

import io.github.gythialy.sporteditor.data.StepMode
import io.github.gythialy.sporteditor.data.TargetSettings
import java.time.LocalDate
import kotlin.random.Random

/**
 * Applies a [TargetSettings] to a raw sensor step value.
 *
 * One instance belongs to one hooked process. It keeps the auto-increment counter and the
 * per-day baseline used to cap a day's steps:
 *
 *  - On the first event, a new day, or when the raw counter moves backwards (device reboot),
 *    the current raw value becomes the day's baseline.
 *  - [TargetSettings.maxSteps] is the total reported value the target app may see for today,
 *    so every mode is capped by that absolute limit, not by an accumulated delta.
 *  - The daily limit is [TargetSettings.maxSteps] plus a per-day random value in 0..100.
 *    Lowering the limit mid-day takes effect immediately: the report stops growing at the
 *    new cap instead of staying at the already-breached value.
 *
 * The class is intentionally free of Android dependencies so it can be unit tested.
 */
internal class StepTransformer(
    private val clock: () -> LocalDate = { LocalDate.now() },
    private val extraSteps: (epochDay: Long) -> Int = { epochDay ->
        Random(epochDay).nextInt(0, DAILY_JITTER_LIMIT + 1)
    },
    private val store: DailyStateStore? = null
) {
    private var restored = false
    private var day: LocalDate? = null
    private var baselineRaw = 0f
    private var dayStartReported = 0f
    private var dailyLimit = 0
    private var lastReported = 0f
    private var autoCount = 0

    init {
        restore()
    }

    @Synchronized
    fun transform(raw: Float, settings: TargetSettings): Float {
        if (!settings.enabled) return raw
        if (!restored) restore()

        val today = clock()
        val counterReset = raw < baselineRaw
        if (day == null || today != day || counterReset) {
            beginDay(today, raw, settings, counterReset)
        }

        // The daily limit can shrink at any time (the user lowers TargetSettings.maxSteps, or
        // the day's random jitter differs). Keep the persisted budget only when it is not
        // larger than today's budget, so an already-breached cap stops immediately instead of
        // staying at the old value for the rest of the day.
        val todayLimit = (settings.maxSteps + extraSteps(today.toEpochDay()))
            .coerceIn(1, TargetSettings.MAX_VALUE)
        if (todayLimit != dailyLimit) {
            dailyLimit = todayLimit
        }

        val reported = when (settings.mode) {
            StepMode.MULTIPLY -> raw * settings.value
            StepMode.ADD -> raw + settings.value
            StepMode.REPLACE -> settings.value.toFloat()
            StepMode.AUTO -> autoValue(raw, settings)
        }

        // maxSteps is the total reported value the target app may see for today, so every mode
        // is capped by the absolute daily limit rather than an accumulated delta.
        val capped = reported.coerceAtMost(dailyLimit.toFloat())

        lastReported = sanitize(capped)
        persist()
        return lastReported
    }

    private fun restore() {
        if (restored) return
        val state = store?.load() ?: return
        restored = true
        day = runCatching { LocalDate.ofEpochDay(state.epochDay) }.getOrNull()
        baselineRaw = state.baselineRaw
        dayStartReported = state.dayStartReported
        dailyLimit = state.dailyLimit
        lastReported = state.lastReported
        autoCount = state.autoCount
    }

    private fun persist() {
        val currentDay = day ?: return
        store?.save(
            DailyState(
                epochDay = currentDay.toEpochDay(),
                baselineRaw = baselineRaw,
                dayStartReported = dayStartReported,
                dailyLimit = dailyLimit,
                lastReported = lastReported,
                autoCount = autoCount
            )
        )
    }

    private fun beginDay(today: LocalDate, raw: Float, settings: TargetSettings, counterReset: Boolean) {
        dailyLimit = (settings.maxSteps + extraSteps(today.toEpochDay()))
            .coerceIn(1, TargetSettings.MAX_VALUE)

        // Track the raw value so a reboot (counter rolling back) can be detected. The daily
        // cap itself is absolute: the target app never sees more than dailyLimit today.
        baselineRaw = raw
        dayStartReported = 0f
        day = today
        autoCount = 0
    }

    private fun autoValue(raw: Float, settings: TargetSettings): Float {
        val increment = settings.value.toLong() * autoCount
        return if (increment > TargetSettings.AUTO_CAP) {
            autoCount = 0
            raw
        } else {
            autoCount++
            raw + increment
        }
    }

    private fun sanitize(value: Float): Float = when {
        value.isNaN() || value.isInfinite() -> 0f
        value < 0f -> 0f
        value > MAX_STEPS -> MAX_STEPS
        else -> value
    }

    companion object {
        /** Upper bound reported to target apps to avoid overflow in their own math. */
        const val MAX_STEPS = 9_999_999f

        /** At most this many extra steps are added to the configured daily limit. */
        const val DAILY_JITTER_LIMIT = 100
    }
}
