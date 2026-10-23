package io.github.gythialy.sporteditor.data

/**
 * How each day's reported increment is derived from the raw sensor delta.
 *
 * The reported sensor value itself is an absolute counter that continues across days; the
 * modes only decide how much a single day may add. The [storageKey] is persisted in LSPosed
 * remote preferences and therefore must stay stable.
 */
enum class StepMode(val storageKey: String) {
    /** 当日增量 = 真实步数增量 × value */
    MULTIPLY("multiply"),

    /** 当日增量 = 真实步数增量 + value（value 每天只加一次） */
    ADD("add"),

    /** 当日增量 = value × 回调次数（每天重新计数） */
    AUTO("auto"),

    /** 当日增量 = value（当天固定） */
    REPLACE("replace")
    ;

    companion object {
        fun fromStorageKey(key: String?, fallback: StepMode = MULTIPLY): StepMode = entries.firstOrNull { it.storageKey == key } ?: fallback
    }
}
