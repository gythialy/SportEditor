package io.github.sporteditor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetSettingsTest {

    @Test
    fun normalized_clampsValueIntoSupportedRange() {
        val low = TargetSettings(true, StepMode.MULTIPLY, -10).normalized()
        val high = TargetSettings(true, StepMode.MULTIPLY, Int.MAX_VALUE).normalized()

        assertEquals(TargetSettings.MIN_VALUE, low.value)
        assertEquals(TargetSettings.MAX_MULTIPLIER, high.value)
    }

    @Test
    fun normalized_usesStepRangeForNonMultiplyModes() {
        val add = TargetSettings(true, StepMode.ADD, 50_000_000).normalized()
        val auto = TargetSettings(true, StepMode.AUTO, 50_000_000).normalized()
        val replace = TargetSettings(true, StepMode.REPLACE, 50_000_000).normalized()

        assertEquals(TargetSettings.MAX_VALUE, add.value)
        assertEquals(TargetSettings.MAX_VALUE, auto.value)
        assertEquals(TargetSettings.MAX_VALUE, replace.value)
    }

    @Test
    fun mode_mapsUnknownStorageKeyToDefault() {
        assertEquals(StepMode.MULTIPLY, StepMode.fromStorageKey(null))
        assertEquals(StepMode.MULTIPLY, StepMode.fromStorageKey("nope"))
        assertEquals(StepMode.AUTO, StepMode.fromStorageKey("auto"))
    }

    @Test
    fun mode_usesPerAppFallbackWhenUnset() {
        assertEquals(StepMode.AUTO, StepMode.fromStorageKey(null, StepMode.AUTO))
        assertEquals(StepMode.AUTO, StepMode.fromStorageKey("unknown", StepMode.AUTO))
        assertEquals(StepMode.ADD, StepMode.fromStorageKey("add", StepMode.AUTO))
    }

    @Test
    fun target_isResolvedByPackageName() {
        assertEquals(AppTarget.WECHAT, AppTarget.fromPackageName("com.tencent.mm"))
        assertEquals(AppTarget.ALIPAY, AppTarget.fromPackageName("com.eg.android.AlipayGphone"))
        assertEquals(null, AppTarget.fromPackageName("com.example.unknown"))
    }

    @Test
    fun normalized_clampsMaxSteps() {
        assertEquals(
            TargetSettings.MIN_MAX_STEPS,
            TargetSettings(true, StepMode.MULTIPLY, 100, maxSteps = -1).normalized().maxSteps
        )
        assertEquals(
            TargetSettings.MAX_VALUE,
            TargetSettings(true, StepMode.MULTIPLY, 100, maxSteps = Int.MAX_VALUE).normalized().maxSteps
        )
    }

    @Test
    fun wechat_sanitizeFallsBackFromFixed() {
        val fixed = TargetSettings(true, StepMode.REPLACE, 80_000)
        val sanitized = AppTarget.WECHAT.sanitize(fixed)
        assertEquals(StepMode.MULTIPLY, sanitized.mode)
        assertEquals(80_000, sanitized.value)
        assertEquals(StepMode.REPLACE, AppTarget.ALIPAY.sanitize(fixed).mode)
    }

    @Test
    fun defaults_areDisabledUntilTheUserEnablesThem() {
        assertEquals(false, AppTarget.WECHAT.defaultSettings.enabled)
        assertEquals(StepMode.MULTIPLY, AppTarget.WECHAT.defaultSettings.mode)
        assertEquals(50, AppTarget.WECHAT.defaultSettings.value)
        assertEquals(20_000, AppTarget.WECHAT.defaultSettings.maxSteps)
        assertEquals(StepMode.MULTIPLY, AppTarget.ALIPAY.defaultSettings.mode)
        assertEquals(10, AppTarget.ALIPAY.defaultSettings.value)
        assertEquals(15_000, AppTarget.ALIPAY.defaultSettings.maxSteps)
        // Targets without a dedicated default keep the generic multiplier profile.
        assertEquals(80_000, AppTarget.QQ.defaultSettings.maxSteps)
    }
}
