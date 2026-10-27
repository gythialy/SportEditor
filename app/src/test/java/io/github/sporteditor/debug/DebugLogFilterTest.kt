package io.github.sporteditor.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugLogFilterTest {

    private fun entry(type: DebugLogType, message: String = type.name, level: DebugLogLevel = DebugLogLevel.INFO, source: String = "test") =
        DebugLogEntry(
            time = 0L,
            type = type,
            level = level,
            source = source,
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

    @Test
    fun queryMatchesMessageOrSourceIgnoringCase() {
        val entries = listOf(
            entry(DebugLogType.HOOK, message = "step type=19 raw=10.0 -> 500.0"),
            entry(DebugLogType.CONFIG, message = "Textarea updated"),
            entry(DebugLogType.FRAMEWORK, message = "framework bound"),
            entry(DebugLogType.HOOK, message = "hooks installed")
        )

        // 关键词命中消息或来源，大小写不敏感；空白查询不缩小范围。
        assertEquals(
            listOf("Textarea updated"),
            DebugLogFilter.apply(entries, emptySet(), "textArea").map { it.message }
        )
        assertEquals(
            listOf("framework bound"),
            DebugLogFilter.apply(entries, emptySet(), "BOUND").map { it.message }
        )
        assertEquals(
            listOf("hooks installed"),
            DebugLogFilter.apply(entries, emptySet(), "hooks").map { it.message }
        )
        assertEquals(entries.size, DebugLogFilter.apply(entries, emptySet(), "   ").size)
    }

    @Test
    fun typeAndQueryFiltersStack() {
        val entries = listOf(
            entry(DebugLogType.HOOK, message = "hooks installed"),
            entry(DebugLogType.HOOK, message = "package ready"),
            entry(DebugLogType.CONFIG, message = "hooks in settings")
        )

        assertEquals(
            listOf("hooks installed"),
            DebugLogFilter.apply(entries, setOf(DebugLogType.HOOK), "hooks").map { it.message }
        )
    }

    @Test
    fun capturePolicy_acceptsOnlyAtOrAboveThreshold() {
        assertTrue(LogCapturePolicy.accepts(DebugLogLevel.INFO, DebugLogLevel.INFO))
        assertTrue(LogCapturePolicy.accepts(DebugLogLevel.WARN, DebugLogLevel.INFO))
        assertFalse(LogCapturePolicy.accepts(DebugLogLevel.INFO, DebugLogLevel.WARN))
        assertTrue(LogCapturePolicy.accepts(DebugLogLevel.ERROR, DebugLogLevel.WARN))
        assertFalse(LogCapturePolicy.accepts(DebugLogLevel.WARN, DebugLogLevel.ERROR))
    }
}
