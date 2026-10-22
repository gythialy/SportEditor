package io.github.gythialy.sporteditor.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gythialy.sporteditor.BuildConfig
import io.github.gythialy.sporteditor.R
import io.github.gythialy.sporteditor.data.AppTarget
import io.github.gythialy.sporteditor.data.SettingsState
import io.github.gythialy.sporteditor.debug.DebugLogEntry
import io.github.gythialy.sporteditor.debug.DebugLogFilter
import io.github.gythialy.sporteditor.debug.DebugLogLevel
import io.github.gythialy.sporteditor.debug.DebugLogStore
import io.github.gythialy.sporteditor.debug.DebugLogType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(state: SettingsState, store: DebugLogStore, onBack: () -> Unit) {
    val entries by store.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    var selectedRaw by rememberSaveable { mutableStateOf("") }
    val selected = selectedRaw.split(',')
        .mapNotNull { runCatching { DebugLogType.valueOf(it) }.getOrNull() }
        .toSet()
    val filtered = DebugLogFilter.apply(entries, selected).asReversed()

    val copiedMessage = stringResource(R.string.debug_copied)
    val clearedMessage = stringResource(R.string.debug_cleared)

    val report = {
        buildReport(state, entries)
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.debug_title)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(report()))
                        scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                    }
                ) {
                    Text(stringResource(R.string.debug_copy))
                }
                TextButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, report())
                        }
                        context.startActivity(Intent.createChooser(send, null))
                    }
                ) {
                    Text(stringResource(R.string.debug_share))
                }
                TextButton(
                    onClick = {
                        store.clear()
                        scope.launch { snackbarHostState.showSnackbar(clearedMessage) }
                    }
                ) {
                    Text(stringResource(R.string.debug_clear))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item { EnvironmentCard(state) }
                item { TargetsCard(state) }
                item {
                    Text(
                        text = stringResource(R.string.debug_logs),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    FilterRow(
                        entries = entries,
                        selected = selected,
                        onSelectedChange = { next ->
                            selectedRaw = next.joinToString(",") { it.name }
                        }
                    )
                }

                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(
                                if (entries.isEmpty()) {
                                    R.string.debug_empty
                                } else {
                                    R.string.debug_empty_filtered
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                        )
                    }
                } else {
                    items(filtered) { entry ->
                        LogRow(entry = entry, timeFormat = timeFormat)
                    }
                }

                item { TipsCard() }
            }
        }
    }
}

@Composable
private fun EnvironmentCard(state: SettingsState) {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_environment),
                style = MaterialTheme.typography.titleMedium
            )
            InfoRow(R.string.debug_app_version, BuildConfig.VERSION_NAME)
            InfoRow(
                R.string.debug_build_type,
                if (BuildConfig.DEBUG) "debug" else "release"
            )
            InfoRow(
                R.string.debug_android,
                Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"
            )
            InfoRow(R.string.debug_device, Build.MANUFACTURER + " " + Build.MODEL)
            InfoRow(
                R.string.debug_framework,
                (state.frameworkName ?: "-") + " " + (state.frameworkVersion ?: "") +
                    " · API " + (state.apiVersion ?: 0)
            )
            InfoRow(
                R.string.debug_service,
                stringResource(
                    if (state.connected) R.string.debug_connected else R.string.debug_disconnected
                )
            )
        }
    }
}

@Composable
private fun TargetsCard(state: SettingsState) {
    val context = LocalContext.current
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_targets),
                style = MaterialTheme.typography.titleMedium
            )
            AppTarget.entries.forEach { target ->
                val settings = state.settingsFor(target)
                val installed = remember(target.packageName) {
                    runCatching {
                        context.packageManager.getApplicationInfo(target.packageName, 0)
                    }.isSuccess
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(target.labelRes()) + " · " + stringResource(
                            if (installed) R.string.debug_installed else R.string.debug_not_installed
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = target.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.debug_target_state,
                            settings.enabled.toString(),
                            settings.mode.storageKey,
                            settings.value,
                            settings.maxSteps
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(entries: List<DebugLogEntry>, selected: Set<DebugLogType>, onSelectedChange: (Set<DebugLogType>) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selected.isEmpty(),
            onClick = { onSelectedChange(emptySet()) },
            label = {
                Text(stringResource(R.string.debug_filter_all) + " " + entries.size)
            }
        )
        DebugLogType.entries.forEach { type ->
            val count = entries.count { it.type == type }
            FilterChip(
                selected = type in selected,
                onClick = {
                    val next = if (type in selected) selected - type else selected + type
                    onSelectedChange(next)
                },
                label = { Text(typeLabel(type) + " " + count) }
            )
        }
    }
}

@Composable
private fun LogRow(entry: DebugLogEntry, timeFormat: SimpleDateFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = timeFormat.format(Date(entry.time)),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(58.dp)
        )
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = typeLabel(entry.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor(entry.type)
                    )
                    Text(
                        text = entry.level.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = levelColor(entry.level)
                    )
                    Text(
                        text = entry.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = entry.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun TipsCard() {
    Card(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_tip_logcat),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = stringResource(R.string.debug_tip_framework),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(@StringRes label: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun typeLabel(type: DebugLogType): String = stringResource(
    when (type) {
        DebugLogType.FRAMEWORK -> R.string.debug_type_framework
        DebugLogType.CONFIG -> R.string.debug_type_config
        DebugLogType.HOOK -> R.string.debug_type_hook
        DebugLogType.ERROR -> R.string.debug_type_error
    }
)

@Composable
private fun typeColor(type: DebugLogType): Color = when (type) {
    DebugLogType.FRAMEWORK -> MaterialTheme.colorScheme.primary
    DebugLogType.CONFIG -> MaterialTheme.colorScheme.tertiary
    DebugLogType.HOOK -> MaterialTheme.colorScheme.secondary
    DebugLogType.ERROR -> MaterialTheme.colorScheme.error
}

@Composable
private fun levelColor(level: DebugLogLevel): Color = when (level) {
    DebugLogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    DebugLogLevel.WARN -> MaterialTheme.colorScheme.tertiary
    DebugLogLevel.ERROR -> MaterialTheme.colorScheme.error
}

private fun buildReport(state: SettingsState, entries: List<DebugLogEntry>): String = buildString {
    appendLine("Sport Editor diagnostics")
    appendLine("app=" + BuildConfig.VERSION_NAME + " debug=" + BuildConfig.DEBUG)
    appendLine("android=" + Build.VERSION.RELEASE + " api=" + Build.VERSION.SDK_INT)
    appendLine("device=" + Build.MANUFACTURER + " " + Build.MODEL)
    appendLine(
        "framework=" + (state.frameworkName ?: "?") + " " + (state.frameworkVersion ?: "?") +
            " api=" + (state.apiVersion ?: 0)
    )
    appendLine("connected=" + state.connected)
    appendLine()
    AppTarget.entries.forEach { target ->
        val settings = state.settingsFor(target)
        appendLine(
            target.key + ": enabled=" + settings.enabled + " mode=" + settings.mode.storageKey +
                " value=" + settings.value + " max=" + settings.maxSteps
        )
    }
    appendLine()
    entries.forEach { entry ->
        appendLine(
            "[" + entry.type + "] " + entry.level + " " + entry.source + ": " + entry.message
        )
    }
}
