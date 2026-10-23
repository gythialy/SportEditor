package io.github.gythialy.sporteditor.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
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
import io.github.gythialy.sporteditor.ui.theme.cardContainerColor
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

    var menuOpen by remember { mutableStateOf(false) }
    val copiedMessage = stringResource(R.string.debug_copied)
    val clearedMessage = stringResource(R.string.debug_cleared)

    val report = { buildReport(state, entries) }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.debug_more_actions)
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.debug_copy)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    clipboard.setText(AnnotatedString(report()))
                                    scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.debug_share)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, report())
                                    }
                                    context.startActivity(Intent.createChooser(send, null))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.debug_clear)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Filled.DeleteSweep, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    store.clear()
                                    scope.launch { snackbarHostState.showSnackbar(clearedMessage) }
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionTitle(R.string.debug_environment) }
            item { EnvironmentCard(state) }

            item { SectionTitle(R.string.debug_targets) }
            item { TargetsCard(state) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.debug_logs),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.debug_log_count, filtered.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                LogFilter(
                    selected = selected,
                    onSelectedChange = { types ->
                        selectedRaw = types.joinToString(",") { it.name }
                    }
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = stringResource(
                            if (entries.isEmpty()) R.string.debug_empty else R.string.debug_empty_filtered
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
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

@Composable
private fun SectionTitle(@StringRes labelRes: Int) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun DiagnosticsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor()
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun EnvironmentCard(state: SettingsState) {
    DiagnosticsCard {
        InfoRow(R.string.debug_app_version, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        RowDivider()
        InfoRow(
            R.string.debug_build_type,
            if (BuildConfig.DEBUG) "debug" else "release"
        )
        RowDivider()
        InfoRow(
            R.string.debug_android,
            "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
        RowDivider()
        InfoRow(R.string.debug_device, "${Build.MANUFACTURER} ${Build.MODEL}")
        RowDivider()
        InfoRow(
            R.string.debug_framework,
            "${state.frameworkName ?: "-"} ${state.frameworkVersion ?: ""} · API ${state.apiVersion ?: 0}"
        )
        RowDivider()
        InfoRow(
            R.string.debug_service,
            stringResource(
                if (state.connected) R.string.debug_connected else R.string.debug_disconnected
            ),
            valueColor = if (state.connected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
private fun TargetsCard(state: SettingsState) {
    val context = LocalContext.current
    DiagnosticsCard {
        AppTarget.entries.forEachIndexed { index, target ->
            if (index > 0) RowDivider()
            val settings = state.settingsFor(target)
            val installed = remember(target.packageName) {
                runCatching {
                    context.packageManager.getApplicationInfo(target.packageName, 0)
                }.isSuccess
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(target.labelRes()),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(
                                if (installed) R.string.debug_installed else R.string.debug_not_installed
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.debug_target_state,
                            settings.enabled.toString(),
                            settings.mode.storageKey,
                            settings.value,
                            settings.maxSteps
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (settings.enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogFilter(selected: Set<DebugLogType>, onSelectedChange: (Set<DebugLogType>) -> Unit) {
    val options = listOf<DebugLogType?>(null) + DebugLogType.entries
    val active = selected.firstOrNull()

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, type ->
            SegmentedButton(
                selected = active == type,
                onClick = {
                    onSelectedChange(if (type == null) emptySet() else setOf(type))
                },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = {
                    Text(
                        text = if (type == null) {
                            stringResource(R.string.debug_filter_all)
                        } else {
                            typeLabel(type)
                        },
                        maxLines = 1
                    )
                }
            )
        }
    }
}

@Composable
private fun LogRow(entry: DebugLogEntry, timeFormat: SimpleDateFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timeFormat.format(Date(entry.time)),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
        TypeBadge(type = entry.type, error = entry.level == DebugLogLevel.ERROR)
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TypeBadge(type: DebugLogType, error: Boolean) {
    val container = when {
        error || type == DebugLogType.ERROR -> MaterialTheme.colorScheme.errorContainer
        type == DebugLogType.FRAMEWORK -> MaterialTheme.colorScheme.primaryContainer
        type == DebugLogType.HOOK -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when {
        error || type == DebugLogType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        type == DebugLogType.FRAMEWORK -> MaterialTheme.colorScheme.onPrimaryContainer
        type == DebugLogType.HOOK -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = typeLabel(type),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TipsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor()
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
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
private fun InfoRow(@StringRes label: Int, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
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
