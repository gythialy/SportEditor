package io.github.sporteditor.ui

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sporteditor.BuildConfig
import io.github.sporteditor.R
import io.github.sporteditor.data.AppTarget
import io.github.sporteditor.data.SettingsState
import io.github.sporteditor.data.StepMode
import io.github.sporteditor.data.TargetSettings
import io.github.sporteditor.debug.DebugLogEntry
import io.github.sporteditor.debug.DebugLogFilter
import io.github.sporteditor.debug.DebugLogLevel
import io.github.sporteditor.debug.DebugLogStore
import io.github.sporteditor.debug.DebugLogType
import io.github.sporteditor.debug.SensorEventLog
import io.github.sporteditor.ui.theme.cardContainerColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** Peer views of the diagnostics screen, shown as the top tabs of the pager. */
private enum class DiagnosticsTab { LOGS, ENVIRONMENT, APPS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    state: SettingsState,
    store: DebugLogStore,
    captureLevel: DebugLogLevel,
    onCaptureLevelChange: (DebugLogLevel) -> Unit,
    onBack: () -> Unit
) {
    val entries by store.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { DiagnosticsTab.entries.size })
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    var selectedRaw by rememberSaveable { mutableStateOf("") }
    val selectedTypes = selectedRaw.split(',')
        .mapNotNull { runCatching { DebugLogType.valueOf(it) }.getOrNull() }
        .toSet()
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var levelMenuOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val filtered = DebugLogFilter.apply(entries, selectedTypes, query)
    val copiedMessage = stringResource(R.string.debug_copied)
    val clearedMessage = stringResource(R.string.debug_cleared)

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DiagnosticsTopBar(
                tab = diagnosticsTab(pagerState),
                searchOpen = searchOpen,
                query = query,
                captureLevel = captureLevel,
                onQuery = { query = it },
                onToggleSearch = {
                    searchOpen = !searchOpen
                    if (!searchOpen) query = ""
                },
                onCaptureLevelChange = onCaptureLevelChange,
                levelMenuOpen = levelMenuOpen,
                onLevelMenuOpenChange = { levelMenuOpen = it },
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onCopy = {
                    menuOpen = false
                    clipboard.setText(AnnotatedString(buildReport(state, filtered)))
                    scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                },
                onShare = {
                    menuOpen = false
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, buildReport(state, filtered))
                    }
                    context.startActivity(Intent.createChooser(send, null))
                },
                onClear = {
                    menuOpen = false
                    store.clear()
                    scope.launch { snackbarHostState.showSnackbar(clearedMessage) }
                },
                onCloseSearch = {
                    query = ""
                    searchOpen = false
                },
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // 内容 pager 占满剩余高度，tabs 固定在底部（对应设计稿 01/01b/01c/02/03）。
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (DiagnosticsTab.entries[page]) {
                    DiagnosticsTab.LOGS -> LogsPage(
                        filtered = filtered,
                        allEntries = entries,
                        selected = selectedTypes,
                        query = query,
                        timeFormat = timeFormat,
                        onSelectedChange = { types -> selectedRaw = types.joinToString(",") { it.name } }
                    )

                    DiagnosticsTab.ENVIRONMENT -> EnvironmentPage(state = state, entriesSize = entries.size)

                    DiagnosticsTab.APPS -> AppsPage(state = state, entries = entries)
                }
            }

            DiagnosticsTabBar(pagerState)
        }
    }
}

private fun diagnosticsTab(pagerState: PagerState): DiagnosticsTab =
    DiagnosticsTab.entries[pagerState.currentPage.coerceIn(0, DiagnosticsTab.entries.lastIndex)]

// ------------------------------ 顶部栏 ------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticsTopBar(
    tab: DiagnosticsTab,
    searchOpen: Boolean,
    query: String,
    captureLevel: DebugLogLevel,
    onQuery: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onCaptureLevelChange: (DebugLogLevel) -> Unit,
    levelMenuOpen: Boolean,
    onLevelMenuOpenChange: (Boolean) -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit,
    onCloseSearch: () -> Unit,
    onBack: () -> Unit
) {
    if (searchOpen) {
        // 搜索态（对应设计稿 01c）：顶栏整体变成搜索输入。状态栏 inset 必须补齐，
        // 否则输入框与关闭/清除按钮会被系统状态栏挡住而无法点击。
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCloseSearch) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.action_back))
            }
            Spacer(Modifier.width(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.debug_clear_search),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onQuery("") },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    TopAppBar(
        title = { Text(text = stringResource(R.string.debug_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        },
        actions = {
            if (tab == DiagnosticsTab.LOGS) {
                IconButton(onClick = onToggleSearch) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = stringResource(R.string.debug_search_acd))
                }
                Box {
                    IconButton(onClick = { onLevelMenuOpenChange(true) }) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = stringResource(R.string.debug_capture_level_acd)
                        )
                    }
                    DropdownMenu(expanded = levelMenuOpen, onDismissRequest = { onLevelMenuOpenChange(false) }) {
                        DebugLogLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(stringResource(levelLabel(level))) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = levelIcon(level),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (captureLevel == level) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    onCaptureLevelChange(level)
                                    onLevelMenuOpenChange(false)
                                }
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { onMenuOpenChange(true) }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.debug_more_actions)
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.debug_copy)) },
                            leadingIcon = { Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null) },
                            onClick = onCopy
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.debug_share)) },
                            leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                            onClick = onShare
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.debug_clear)) },
                            leadingIcon = { Icon(imageVector = Icons.Filled.DeleteSweep, contentDescription = null) },
                            onClick = onClear
                        )
                    }
                }
            }
        }
    )
}

// ------------------------------ Tabs ------------------------------

@Composable
private fun DiagnosticsTabBar(pagerState: PagerState) {
    val scope = rememberCoroutineScope()
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            // 接近设计稿 01/02/03 的 3px 底部短胶囊指示条。
            Box(
                Modifier
                    .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                    .fillMaxWidth()
                    .height(3.dp)
                    .padding(horizontal = 42.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    ) {
        DiagnosticsTab.entries.forEachIndexed { index, tab ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = {
                    // 双向联动：点击滚到页签，滑动 pager 同步 tab 选中态。
                    scope.launch { pagerState.animateScrollToPage(index) }
                },
                text = { Text(stringResource(tab.labelRes())) },
                icon = {
                    Icon(imageVector = tab.iconRes(), contentDescription = null, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@StringRes
private fun DiagnosticsTab.labelRes(): Int = when (this) {
    DiagnosticsTab.LOGS -> R.string.debug_logs
    DiagnosticsTab.ENVIRONMENT -> R.string.debug_environment
    DiagnosticsTab.APPS -> R.string.debug_targets
}

private fun DiagnosticsTab.iconRes(): ImageVector = when (this) {
    DiagnosticsTab.LOGS -> Icons.Filled.Article
    DiagnosticsTab.ENVIRONMENT -> Icons.Filled.PhoneAndroid
    DiagnosticsTab.APPS -> Icons.Filled.Apps
}

// ------------------------------ 日志页 ------------------------------

@Composable
private fun LogsPage(
    filtered: List<DebugLogEntry>,
    allEntries: List<DebugLogEntry>,
    selected: Set<DebugLogType>,
    query: String,
    timeFormat: SimpleDateFormat,
    onSelectedChange: (Set<DebugLogType>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = if (query.isNotBlank()) {
                    stringResource(R.string.debug_search_matches, filtered.size, allEntries.size)
                } else {
                    stringResource(R.string.debug_log_count, allEntries.size)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            // 类型 chips：全部 + 框架/配置/Hook，带实时计数；「错误」归等级，不再作为类型。
            // 计数达到 3 位时 4 个 chip 一行放不下，改为横向滚动而不是省略号截断。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypeChip(
                    label = stringResource(R.string.debug_filter_all),
                    count = allEntries.size,
                    selected = selected.isEmpty(),
                    onSelected = { onSelectedChange(emptySet()) }
                )
                DebugLogType.entries.filterNot { it == DebugLogType.ERROR }.forEach { type ->
                    TypeChip(
                        label = typeLabelText(type),
                        count = allEntries.count { it.type == type },
                        selected = selected.contains(type),
                        onSelected = { onSelectedChange(setOf(type)) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = stringResource(
                        if (query.isNotBlank() || selected.isNotEmpty()) {
                            R.string.debug_empty_filtered
                        } else {
                            R.string.debug_empty
                        }
                    )
                )
            }
        } else {
            itemsIndexed(filtered.asReversed()) { _, entry ->
                LogCard(entry = entry, timeFormat = timeFormat)
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, count: Int, selected: Boolean, modifier: Modifier = Modifier, onSelected: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.clickable(onClick = onSelected)
    ) {
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun LogCard(entry: DebugLogEntry, timeFormat: SimpleDateFormat) {
    var expanded by rememberSaveable(entry.time, entry.source, entry.message) { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = timeFormat.format(Date(entry.time)),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LevelBadge(level = entry.level)
                Text(
                    text = typeLabelText(entry.type) + " · " + entry.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = if (entry.level == DebugLogLevel.ERROR) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LevelBadge(level: DebugLogLevel) {
    val container = when (level) {
        DebugLogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer
        DebugLogLevel.WARN -> MaterialTheme.colorScheme.tertiaryContainer
        DebugLogLevel.INFO -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (level) {
        DebugLogLevel.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        DebugLogLevel.WARN -> MaterialTheme.colorScheme.onTertiaryContainer
        DebugLogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(7.dp)) {
        Text(
            text = stringResource(levelLabel(level)),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@StringRes
private fun levelLabel(level: DebugLogLevel): Int = when (level) {
    DebugLogLevel.INFO -> R.string.debug_level_info
    DebugLogLevel.WARN -> R.string.debug_level_warn
    DebugLogLevel.ERROR -> R.string.debug_level_error
}

/** 采集等级菜单的图标：信息 / 警告 / 错误。 */
private fun levelIcon(level: DebugLogLevel): ImageVector = when (level) {
    DebugLogLevel.INFO -> Icons.Filled.Info
    DebugLogLevel.WARN -> Icons.Filled.Warning
    DebugLogLevel.ERROR -> Icons.Filled.Error
}

@StringRes
private fun typeRes(type: DebugLogType): Int = when (type) {
    DebugLogType.FRAMEWORK -> R.string.debug_type_framework

    DebugLogType.CONFIG -> R.string.debug_type_config

    // ERROR 类型退役后并入 Hook 来源，徽章与 chips 均按来源展示。
    DebugLogType.HOOK, DebugLogType.ERROR -> R.string.debug_type_hook
}

@Composable
private fun typeLabelText(type: DebugLogType): String = stringResource(typeRes(type))

// ------------------------------ 环境页 ------------------------------

@Composable
private fun EnvironmentPage(state: SettingsState, entriesSize: Int) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // 主卡：框架连接状态是本页唯一焦点。
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PulseDot()
                        Text(
                            text = stringResource(
                                if (state.connected) R.string.debug_connected else R.string.debug_disconnected
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = stringResource(
                            if (state.connected) R.string.debug_env_connected_desc else R.string.debug_env_disconnected_desc
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.debug_framework),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = (state.frameworkName ?: "—") + " " + (state.frameworkVersion ?: "") +
                                " · API " + (state.apiVersion ?: 0),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item { InfoTitle(stringResource(R.string.debug_environment)) }
        item {
            InfoCard {
                InfoRow(stringResource(R.string.debug_app_version), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                InfoDivider()
                InfoRow(stringResource(R.string.debug_build_type), if (BuildConfig.DEBUG) "debug" else "release")
                InfoDivider()
                InfoRow(stringResource(R.string.debug_android), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                InfoDivider()
                InfoRow(stringResource(R.string.debug_device), "${Build.MANUFACTURER} ${Build.MODEL}")
            }
        }

        item { InfoTitle(stringResource(R.string.debug_stats)) }
        item {
            InfoCard {
                val enabledCount = state.targets.count { it.value.enabled }
                val installedCount = AppTarget.entries.count { target ->
                    runCatching { context.packageManager.getApplicationInfo(target.packageName, 0) }.isSuccess
                }
                InfoRow(
                    label = stringResource(R.string.debug_targets),
                    value = enabledCount.toString() + " / " + AppTarget.entries.size + " · " +
                        stringResource(R.string.debug_installed) + " " + installedCount
                )
                InfoDivider()
                InfoRow(
                    label = stringResource(R.string.debug_logs),
                    value = entriesSize.toString() + " / 500"
                )
            }
        }

        item { TipsCard() }
    }
}

@Composable
private fun PulseDot() {
    val transition = rememberInfiniteTransition(label = "connect-pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = .45f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "connect-pulse-alpha"
    )
    Box(
        modifier = Modifier
            .size(11.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
private fun InfoTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun InfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun TipsCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_tip_logcat),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
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
private fun EmptyState(icon: ImageVector, title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ------------------------------ 应用页 ------------------------------

private data class TargetSnapshot(val target: AppTarget, val installed: Boolean, val settings: TargetSettings)

@Composable
private fun AppsPage(state: SettingsState, entries: List<DebugLogEntry>) {
    val context = LocalContext.current
    var quickFilter by rememberSaveable { mutableStateOf("") } // "", "enabled", "uninstalled"
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }

    val targets = AppTarget.entries.map { target ->
        TargetSnapshot(
            target = target,
            installed = runCatching {
                context.packageManager.getApplicationInfo(target.packageName, 0)
            }.isSuccess,
            settings = state.settingsFor(target)
        )
    }

    // 最近一条步数传感器日志（Hook 以包名为 source 广播），用于展示目标 App 实际收到的值。
    val latestSensors = entries
        .filter { it.type == DebugLogType.HOOK && it.message.startsWith("step ") }
        .groupBy { it.source }
        .mapValues { (_, list) -> list.maxBy { it.time } }
    val visible = when (quickFilter) {
        "enabled" -> targets.filter { it.settings.enabled }
        "uninstalled" -> targets.filterNot { it.installed }
        else -> targets
    }
    val enabledCount = targets.count { it.settings.enabled }
    val installedCount = targets.count { it.installed }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.debug_apps_summary, enabledCount, AppTarget.entries.size, installedCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            // counts 始终按全部目标统计，不随快捷筛选变化，避免「全部 4」变成「全部 2」。
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypeChip(
                    label = stringResource(R.string.debug_filter_all),
                    count = targets.size,
                    selected = quickFilter.isEmpty(),
                    modifier = Modifier.weight(1f),
                    onSelected = { quickFilter = "" }
                )
                TypeChip(
                    label = stringResource(R.string.debug_filter_enabled),
                    count = enabledCount,
                    selected = quickFilter == "enabled",
                    modifier = Modifier.weight(1f),
                    onSelected = { quickFilter = "enabled" }
                )
                TypeChip(
                    label = stringResource(R.string.debug_filter_uninstalled),
                    count = targets.count { !it.installed },
                    selected = quickFilter == "uninstalled",
                    modifier = Modifier.weight(1f),
                    onSelected = { quickFilter = "uninstalled" }
                )
            }
        }

        items(visible, key = { it.target.packageName }) { snapshot ->
            AppRow(
                snapshot = snapshot,
                detail = expandedKey == snapshot.target.key,
                lastSensor = latestSensors[snapshot.target.packageName],
                onClick = {
                    expandedKey = if (expandedKey == snapshot.target.key) null else snapshot.target.key
                }
            )
        }
    }
}

@Composable
private fun AppRow(snapshot: TargetSnapshot, detail: Boolean, lastSensor: DebugLogEntry?, onClick: () -> Unit) {
    val target = snapshot.target
    val settings = snapshot.settings
    val iconState = rememberAppIcon(target)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 与首页一致：读取目标 App 的真实启动器图标，未安装时回退为字母徽标。
                AppIcon(
                    target = target,
                    state = iconState,
                    size = 40.dp,
                    contentAlpha = if (snapshot.installed) 1f else 0.4f
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                                if (snapshot.installed) R.string.debug_installed else R.string.debug_not_installed
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = modeSummary(settings),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (settings.enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }

            if (detail) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = target.packageName,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
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
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val sensorEvent = lastSensor?.let { SensorEventLog.parse(it.message) }
                    Text(
                        text = if (sensorEvent != null && lastSensor != null) {
                            stringResource(
                                R.string.debug_sensor_line,
                                sensorEvent.sensor,
                                NumberFormat.getIntegerInstance().format(sensorEvent.raw.toInt()),
                                NumberFormat.getIntegerInstance().format(sensorEvent.reported.toInt()),
                                relativeTime(lastSensor.time)
                            )
                        } else {
                            stringResource(R.string.debug_sensor_none)
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 「刚刚 / N 秒前 / N 分钟前 / N 小时前」的粗粒度相对时间，用于最近一次传感器事件。 */
@Composable
private fun relativeTime(time: Long): String {
    val seconds = ((System.currentTimeMillis() - time) / 1000).coerceAtLeast(0)
    return when {
        seconds < 5 -> stringResource(R.string.debug_time_just_now)
        seconds < 60 -> stringResource(R.string.debug_time_seconds_ago, seconds.toInt())
        seconds < 3600 -> stringResource(R.string.debug_time_minutes_ago, (seconds / 60).toInt())
        else -> stringResource(R.string.debug_time_hours_ago, (seconds / 3600).toInt())
    }
}

@Composable
private fun modeSummary(settings: TargetSettings): String { // 与首页一致：数值按当前语言格式化（中文「4,000」/英文「4,000」），避免两处显示不同。
    val value = NumberFormat.getIntegerInstance().format(settings.value)
    return when (settings.mode) {
        StepMode.MULTIPLY -> stringResource(R.string.app_summary_multiply, value)
        StepMode.ADD -> stringResource(R.string.app_summary_add, value)
        StepMode.AUTO -> stringResource(R.string.app_summary_auto, value)
        StepMode.REPLACE -> stringResource(R.string.app_summary_replace, value)
    }
}

// ------------------------------ 报告 ------------------------------

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
    appendLine("capture_level=" + state.captureLevel.name)
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
        appendLine("[${entry.type}] ${entry.level} ${entry.source}: ${entry.message}")
    }
}
