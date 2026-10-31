package io.github.sporteditor.ui

import androidx.annotation.StringRes
import io.github.sporteditor.R
import io.github.sporteditor.data.AppTarget
import io.github.sporteditor.data.StepMode

@StringRes
fun AppTarget.labelRes(): Int = when (this) {
    AppTarget.WECHAT -> R.string.app_wechat
    AppTarget.ALIPAY -> R.string.app_alipay
    AppTarget.QQ -> R.string.app_qq
    AppTarget.WEIBO -> R.string.app_weibo
}

val AppTarget.badge: String
    get() = when (this) {
        AppTarget.WECHAT -> "微"
        AppTarget.ALIPAY -> "支"
        AppTarget.QQ -> "Q"
        AppTarget.WEIBO -> "博"
    }

@StringRes
fun StepMode.labelRes(): Int = when (this) {
    StepMode.MULTIPLY -> R.string.mode_multiply
    StepMode.ADD -> R.string.mode_add
    StepMode.AUTO -> R.string.mode_auto
    StepMode.REPLACE -> R.string.mode_replace
}

/**
 * The strategy that fits the app's sync model best: WeRun scales increments (multiply), Alipay
 * needs a steady per-callback increment (auto), and QQ/Weibo are happiest with a plain offset.
 */
fun AppTarget.recommendedMode(): StepMode = when (this) {
    AppTarget.WECHAT -> StepMode.MULTIPLY
    AppTarget.ALIPAY -> StepMode.AUTO
    AppTarget.QQ, AppTarget.WEIBO -> StepMode.ADD
}

/** Compact label used by the segmented control, where four full labels do not fit. */
@StringRes
fun StepMode.shortLabelRes(): Int = when (this) {
    StepMode.MULTIPLY -> R.string.mode_short_multiply
    StepMode.ADD -> R.string.mode_short_add
    StepMode.AUTO -> R.string.mode_short_auto
    StepMode.REPLACE -> R.string.mode_short_replace
}

@StringRes
fun StepMode.unitRes(): Int = when (this) {
    StepMode.MULTIPLY -> R.string.unit_multiply
    StepMode.ADD, StepMode.AUTO, StepMode.REPLACE -> R.string.unit_steps
}
