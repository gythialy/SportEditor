package io.github.gythialy.sporteditor.data

/**
 * How the reported step value is derived from the raw sensor value.
 *
 * The [storageKey] is persisted in LSPosed remote preferences and therefore must stay stable.
 */
enum class StepMode(val storageKey: String) {
    /** reported = raw × value */
    MULTIPLY("multiply"),

    /** reported = raw + value */
    ADD("add"),

    /** reported = raw + value × callback count */
    AUTO("auto"),

    /** reported = value */
    REPLACE("replace")
    ;

    companion object {
        fun fromStorageKey(key: String?, fallback: StepMode = MULTIPLY): StepMode = entries.firstOrNull { it.storageKey == key } ?: fallback
    }
}
