package io.github.sporteditor.debug

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DebugLogType {
    FRAMEWORK,
    CONFIG,
    HOOK,
    ERROR
}

enum class DebugLogLevel {
    INFO,
    WARN,
    ERROR
}

data class DebugLogEntry(val time: Long, val type: DebugLogType, val level: DebugLogLevel, val source: String, val message: String)

/** Pure filter used by the UI and unit tests. An empty selection means "all types". */
object DebugLogFilter {
    fun apply(entries: List<DebugLogEntry>, types: Set<DebugLogType>): List<DebugLogEntry> = apply(entries, types, null)

    /**
     * [query] is a display-time filter over the message and source; an empty or blank query
     * keeps every entry of the selected types.
     */
    fun apply(entries: List<DebugLogEntry>, types: Set<DebugLogType>, query: String?): List<DebugLogEntry> {
        var filtered = if (types.isEmpty()) entries else entries.filter { it.type in types }
        val needle = query?.trim().orEmpty()
        if (needle.isNotEmpty()) {
            filtered = filtered.filter { entry ->
                entry.message.contains(needle, ignoreCase = true) || entry.source.contains(needle, ignoreCase = true)
            }
        }
        return filtered
    }
}

/**
 * Capture gate shared by the module app and the hooked processes: an entry at or above the
 * configured threshold survives, anything below it is dropped before being recorded.
 */
object LogCapturePolicy {
    fun accepts(level: DebugLogLevel, threshold: DebugLogLevel): Boolean = level.ordinal >= threshold.ordinal
}

/** Contract shared by the module app and the injected process (broadcast channel). */
object DebugLogProtocol {
    const val MODULE_PACKAGE = "io.github.sporteditor"
    const val RECEIVER_CLASS = "$MODULE_PACKAGE.debug.DebugLogReceiver"

    const val EXTRA_TOKEN = "token"
    const val EXTRA_TYPE = "type"
    const val EXTRA_LEVEL = "level"
    const val EXTRA_SOURCE = "source"
    const val EXTRA_MESSAGE = "message"

    const val MAX_SOURCE_LENGTH = 80
    const val MAX_MESSAGE_LENGTH = 4000
}

/**
 * In-memory ring buffer plus a bounded on-disk log. The store lives in the module app
 * process; hooked processes can append entries through [DebugLogReceiver].
 */
class DebugLogStore(context: Context) {

    private val appContext = context.applicationContext
    private val preferences: SharedPreferences =
        appContext.getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
    private val file = File(appContext.filesDir, "diagnostics.log")
    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SportEditor-debug-log").apply { isDaemon = true }
    }

    private val _entries = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val entries: StateFlow<List<DebugLogEntry>> = _entries.asStateFlow()

    /** Minimum severity that survives capture; the UI updates it through [setCaptureLevel]. */
    private val _captureLevel = MutableStateFlow(readCaptureLevel())
    val captureLevel: StateFlow<DebugLogLevel> = _captureLevel.asStateFlow()

    /** Random token that hooked processes must present when sending a log entry. */
    val token: String = preferences.getString(KEY_TOKEN, null)
        ?: UUID.randomUUID().toString().also { value ->
            preferences.edit { putString(KEY_TOKEN, value) }
        }

    init {
        _entries.value = load()
    }

    @Synchronized
    fun log(type: DebugLogType, level: DebugLogLevel, source: String, message: String) {
        // The capture gate applies to both in-app records and hook broadcasts.
        if (!LogCapturePolicy.accepts(level, _captureLevel.value)) return
        val entry = DebugLogEntry(
            time = System.currentTimeMillis(),
            type = type,
            level = level,
            source = sanitize(source, DebugLogProtocol.MAX_SOURCE_LENGTH),
            message = sanitize(message, DebugLogProtocol.MAX_MESSAGE_LENGTH)
        )
        _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
        writer.execute { append(entry) }
    }

    fun log(type: DebugLogType, source: String, message: String) = log(type, DebugLogLevel.INFO, source, message)

    /**
     * Appends an entry received from a hooked process. Returns false for a bad token or
     * malformed payload; callers should treat it as an untrusted broadcast.
     */
    @Synchronized
    fun record(intent: Intent): Boolean {
        if (!isTrusted(intent)) return false

        val type = intent.getStringExtra(DebugLogProtocol.EXTRA_TYPE)
            ?.let { runCatching { DebugLogType.valueOf(it) }.getOrNull() }
            ?: return false
        val level = intent.getStringExtra(DebugLogProtocol.EXTRA_LEVEL)
            ?.let { runCatching { DebugLogLevel.valueOf(it) }.getOrNull() }
            ?: DebugLogLevel.INFO
        val source = intent.getStringExtra(DebugLogProtocol.EXTRA_SOURCE) ?: "hook"
        val message = intent.getStringExtra(DebugLogProtocol.EXTRA_MESSAGE) ?: return false

        log(type, level, source, message)
        return true
    }

    @Synchronized
    fun clear() {
        _entries.value = emptyList()
        writer.execute { runCatching { file.delete() } }
    }

    fun snapshot(): String = buildString {
        appendLine("Sport Editor diagnostics")
        appendLine()
        _entries.value.forEach { entry ->
            appendLine("[" + entry.type + "] " + entry.level + " " + entry.source + ": " + entry.message)
        }
    }

    /** Persists the capture threshold so it survives app restarts. */
    fun setCaptureLevel(level: DebugLogLevel) {
        _captureLevel.value = level
        preferences.edit { putString(KEY_CAPTURE_LEVEL, level.name) }
    }

    private fun readCaptureLevel(): DebugLogLevel = preferences.getString(KEY_CAPTURE_LEVEL, null)
        ?.let { name -> runCatching { DebugLogLevel.valueOf(name) }.getOrNull() }
        ?: DebugLogLevel.INFO

    private fun isTrusted(intent: Intent): Boolean {
        val received = intent.getStringExtra(DebugLogProtocol.EXTRA_TOKEN) ?: return false
        if (received.length != token.length) return false
        var diff = 0
        for (index in token.indices) {
            diff = diff or (token[index].code xor received[index].code)
        }
        return diff == 0
    }

    private fun load(): List<DebugLogEntry> {
        if (!file.isFile) return emptyList()
        return runCatching {
            file.readLines()
                .mapNotNull(::parse)
                .takeLast(MAX_ENTRIES)
        }.getOrDefault(emptyList())
    }

    private fun parse(line: String): DebugLogEntry? {
        val parts = line.split('\t', limit = 5)
        if (parts.size != 5) return null
        val time = parts[0].toLongOrNull() ?: return null
        val type = runCatching { DebugLogType.valueOf(parts[1]) }.getOrNull() ?: return null
        val level = runCatching { DebugLogLevel.valueOf(parts[2]) }.getOrNull() ?: return null
        return DebugLogEntry(time, type, level, parts[3], parts[4])
    }

    private fun append(entry: DebugLogEntry) {
        runCatching {
            if (file.length() > MAX_FILE_BYTES) {
                file.writeText(
                    _entries.value.takeLast(MAX_ENTRIES)
                        .joinToString("\n", postfix = "\n", transform = ::serialize)
                )
            } else {
                file.appendText(serialize(entry) + "\n")
            }
        }
    }

    private fun serialize(entry: DebugLogEntry): String = listOf(entry.time, entry.type.name, entry.level.name, entry.source, entry.message)
        .joinToString("\t")

    private fun sanitize(value: String, maxLength: Int): String =
        value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').take(maxLength)

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_CAPTURE_LEVEL = "capture_level"
        const val MAX_ENTRIES = 500
        const val MAX_FILE_BYTES = 256 * 1024L
    }
}
