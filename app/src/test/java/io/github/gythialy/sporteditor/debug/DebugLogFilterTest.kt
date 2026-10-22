package io.github.gythialy.sporteditor.debug

import org.junit.Assert.assertEquals
import org.junit.Test

class DebugLogFilterTest {

    private fun entry(type: DebugLogType, message: String = type.name) = DebugLogEntry(
        time = 0L,
        type = type,
        level = DebugLogLevel.INFO,
        source = "test",
        message = message
    )

    @Test
    fun emptySelectionShowsEverything() {
        val entries = DebugLogType.entries.map { entry(it) }
        assertEquals(entries, DebugLogFilter.apply(entries, emptySet()))
    }

    @Test
    fun singleTypeFiltersOutOthers() {
        val entries = DebugLogType.entries.map { entry(it) }
        val filtered = DebugLogFilter.apply(entries, setOf(DebugLogType.HOOK))
        assertEquals(listOf(DebugLogType.HOOK), filtered.map { it.type })
    }

    @Test
    fun multipleTypesKeepOrder() {
        val entries = listOf(
            entry(DebugLogType.FRAMEWORK),
            entry(DebugLogType.HOOK),
            entry(DebugLogType.ERROR),
            entry(DebugLogType.CONFIG)
        )
        val filtered = DebugLogFilter.apply(
            entries,
            setOf(DebugLogType.CONFIG, DebugLogType.FRAMEWORK)
        )
        assertEquals(
            listOf(DebugLogType.FRAMEWORK, DebugLogType.CONFIG),
            filtered.map { it.type }
        )
    }
}
