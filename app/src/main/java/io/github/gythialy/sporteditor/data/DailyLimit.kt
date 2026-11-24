package io.github.gythialy.sporteditor.data

import java.time.LocalDate
import kotlin.random.Random

/**
 * Daily step budget shared by the hooked process and the settings UI.
 *
 * The configured [TargetSettings.maxSteps] is raised by a per-day pseudo random jitter so the
 * reported value does not look machine-generated. The hook and the UI must compute the exact
 * same number, therefore the jitter is derived from the epoch day instead of a random source.
 */
object DailyLimit {

    /** At most this many extra steps are added to the configured daily limit. */
    const val JITTER_LIMIT = 100

    /** Deterministic 0..[JITTER_LIMIT] jitter for [epochDay]. */
    fun jitter(epochDay: Long): Int = Random(epochDay).nextInt(0, JITTER_LIMIT + 1)

    /** Effective limit for today, i.e. the value the hooked app is actually capped at. */
    fun effectiveToday(maxSteps: Int): Int = effective(maxSteps, LocalDate.now().toEpochDay())

    /** Effective limit for an explicit [epochDay]. */
    fun effective(maxSteps: Int, epochDay: Long): Int = effective(maxSteps, jitter(epochDay))

    /** Effective limit from an already resolved [jitter] value; unit tests use this overload. */
    fun effective(maxSteps: Int, jitter: Int): Int = (maxSteps.coerceAtLeast(1) + jitter).coerceIn(1, TargetSettings.MAX_VALUE)
}
