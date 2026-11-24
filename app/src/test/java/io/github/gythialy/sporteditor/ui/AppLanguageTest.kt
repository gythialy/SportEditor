package io.github.gythialy.sporteditor.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun noPreference_defaultsToChinese() {
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag(null))
    }

    @Test
    fun unknownTag_fallsBackToChinese() {
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag("fr"))
    }

    @Test
    fun englishTag_isRecognized() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
    }
}
