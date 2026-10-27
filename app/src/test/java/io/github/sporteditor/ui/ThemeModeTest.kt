package io.github.sporteditor.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun fromKey_returnsEveryKnownMode() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromKey(mode.storageKey))
        }
    }

    @Test
    fun fromKey_fallsBackToSystemForUnknownOrMissingValues() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("sepia"))
    }
}
