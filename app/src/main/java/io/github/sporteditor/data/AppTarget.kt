package io.github.sporteditor.data

/**
 * Applications whose step sensor stream is modified.
 *
 * Only apps that consume [android.hardware.Sensor.TYPE_STEP_COUNTER] or
 * [android.hardware.Sensor.TYPE_STEP_DETECTOR] are listed here.
 */
enum class AppTarget(
    val key: String,
    val packageName: String,
    val defaultMode: StepMode,
    val defaultValue: Int,
    val defaultMaxSteps: Int = TargetSettings.DEFAULT_MAX_STEPS
) {
    WECHAT("wechat", "com.tencent.mm", StepMode.MULTIPLY, 50, defaultMaxSteps = 20_000),
    ALIPAY("alipay", "com.eg.android.AlipayGphone", StepMode.MULTIPLY, 10, defaultMaxSteps = 15_000),
    QQ("qq", "com.tencent.mobileqq", StepMode.MULTIPLY, 100),
    WEIBO("weibo", "com.sina.weibo", StepMode.MULTIPLY, 100)
    ;

    val defaultSettings: TargetSettings
        get() = TargetSettings(
            enabled = false,
            mode = defaultMode,
            value = defaultValue,
            maxSteps = defaultMaxSteps
        )

    /**
     * WeChat/WeRun syncs increments, so a constant fixed value cannot change the ranking.
     * Fixed is disabled in the UI and stored configurations fall back to the default mode.
     */
    fun sanitize(settings: TargetSettings): TargetSettings = if (this == WECHAT && settings.mode == StepMode.REPLACE) {
        settings.copy(mode = defaultMode)
    } else {
        settings
    }

    companion object {
        private val byPackageName = entries.associateBy(AppTarget::packageName)
        private val byKey = entries.associateBy(AppTarget::key)

        fun fromPackageName(packageName: String): AppTarget? = byPackageName[packageName]

        fun fromKey(key: String?): AppTarget? = key?.let(byKey::get)
    }
}
