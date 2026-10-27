package io.github.sporteditor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.sporteditor.BuildConfig
import io.github.sporteditor.R
import io.github.sporteditor.data.AppTarget
import io.github.sporteditor.data.DailyLimit
import io.github.sporteditor.data.SettingsState
import io.github.sporteditor.data.StepMode
import io.github.sporteditor.data.TargetSettings
import io.github.sporteditor.ui.theme.cardContainerColor
import java.text.NumberFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: SettingsState,
    onEnabledChange: (AppTarget, Boolean) -> Unit,
    onModeChange: (AppTarget, StepMode) -> Unit,
    onValueChange: (AppTarget, Int) -> Unit,
    onMaxStepsChange: (AppTarget, Int) -> Unit,
    onResetTarget: (AppTarget) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    var expandedKeysRaw by rememberSaveable { mutableStateOf("") }
    val expandedKeys = remember(expandedKeysRaw) {
        expandedKeysRaw.split(',').filter { it.isNotEmpty() }.toSet()
    }
    val setExpanded: (AppTarget, Boolean) -> Unit = { target, expand ->
        val next = if (expand) expandedKeys + target.key else expandedKeys - target.key
        expandedKeysRaw = next.joinToString(",")
    }

    var showConnectionSheet by rememberSaveable { mutableStateOf(false) }
    val enabledCount = AppTarget.entries.count { state.settingsFor(it).enabled }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(
                                R.string.home_subtitle,
                                AppTarget.entries.size,
                                enabledCount
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    ConnectionPill(
                        connected = state.connected,
                        onClick = { showConnectionSheet = true }
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.connected) {
                item { DisconnectedCard(onViewSteps = { showConnectionSheet = true }) }
            }

            items(AppTarget.entries.toList(), key = { it.key }) { target ->
                val iconState = rememberAppIcon(target)
                val settings = state.settingsFor(target)
                val expanded = target.key in expandedKeys
                val label = stringResource(target.labelRes())
                val resetDone = stringResource(R.string.action_reset_target_done, label)
                AppCard(
                    target = target,
                    iconState = iconState,
                    settings = settings,
                    expanded = expanded,
                    onExpandChange = { setExpanded(target, it) },
                    onEnabledChange = { enabled ->
                        onEnabledChange(target, enabled)
                        setExpanded(target, enabled)
                    },
                    onModeChange = { onModeChange(target, it) },
                    onValueChange = { onValueChange(target, it) },
                    onMaxStepsChange = { onMaxStepsChange(target, it) },
                    onReset = {
                        onResetTarget(target)
                        scope.launch { snackbarHostState.showSnackbar(resetDone) }
                    }
                )
            }

            item { Footer() }
        }
    }

    if (showConnectionSheet) {
        ConnectionSheet(
            state = state,
            onDismiss = { showConnectionSheet = false }
        )
    }
}

@Composable
private fun ConnectionPill(connected: Boolean, onClick: () -> Unit) {
    val container = if (connected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (connected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    val dot = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val label = stringResource(
        if (connected) R.string.status_badge_connected else R.string.status_badge_disconnected
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content
        )
    }
}

@Composable
private fun DisconnectedCard(onViewSteps: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.status_disconnected),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = stringResource(R.string.status_disconnected_hint),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.status_not_synced),
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(
                onClick = onViewSteps,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(stringResource(R.string.action_view_steps))
            }
        }
    }
}

@Composable
private fun AppCard(
    target: AppTarget,
    iconState: AppIconState,
    settings: TargetSettings,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (StepMode) -> Unit,
    onValueChange: (Int) -> Unit,
    onMaxStepsChange: (Int) -> Unit,
    onReset: () -> Unit
) {
    val label = stringResource(target.labelRes())
    val switchDescription = stringResource(R.string.toggle_app, label)
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    // Draft typed into the daily-max field. It drives every displayed number immediately, while the
    // preference itself is written once the edit settles (debounce, IME done, focus loss or collapse).
    var draftMaxSteps by remember { mutableStateOf<Int?>(null) }
    val pendingMaxSteps = draftMaxSteps?.takeIf { it != settings.maxSteps }
    val effectiveLimit = DailyLimit.effectiveToday(pendingMaxSteps ?: settings.maxSteps)
    val summary = summaryText(settings)

    // Drop a stale draft when the stored value changed elsewhere (reset action, remote edit).
    LaunchedEffect(settings.maxSteps) {
        if (draftMaxSteps != null && draftMaxSteps != settings.maxSteps) draftMaxSteps = null
    }

    Card(
        onClick = { onExpandChange(!expanded) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor(),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppIcon(
                    target = target,
                    state = iconState,
                    contentAlpha = if (settings.enabled) 1f else 0.72f
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = summary,
                            // The "not installed" badge is measured first, so a long summary is
                            // ellipsized instead of pushing the badge out of the row.
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!iconState.installed) {
                            Text(
                                text = stringResource(R.string.app_not_installed),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (expanded) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.action_more)
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_reset_default)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Filled.RestartAlt, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    showResetDialog = true
                                }
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.semantics { contentDescription = switchDescription }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModeSelector(
                        target = target,
                        mode = settings.mode,
                        onModeChange = onModeChange
                    )
                    StepEditor(
                        settings = settings,
                        effectiveLimit = effectiveLimit,
                        onValueChange = onValueChange,
                        onMaxStepsChange = onMaxStepsChange,
                        onDraftMaxStepsChange = { draftMaxSteps = it }
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.action_reset)) },
            text = { Text(stringResource(R.string.action_reset_confirm, label)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onReset()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_reset_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun summaryText(settings: TargetSettings): String {
    if (!settings.enabled) return stringResource(R.string.app_summary_disabled)
    val value = formatNumber(settings.value)
    return when (settings.mode) {
        StepMode.MULTIPLY -> stringResource(R.string.app_summary_multiply, value)
        StepMode.ADD -> stringResource(R.string.app_summary_add, value)
        StepMode.AUTO -> stringResource(R.string.app_summary_auto, value)
        StepMode.REPLACE -> stringResource(R.string.app_summary_replace, value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(target: AppTarget, mode: StepMode, onModeChange: (StepMode) -> Unit) {
    var showHelp by remember { mutableStateOf(false) }
    val helpDescriptions = StepMode.entries.map { item ->
        stringResource(item.labelRes()) + "：" + stringResource(item.descRes())
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_mode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { showHelp = true },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.mode_help),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            StepMode.entries.forEachIndexed { index, item ->
                // WeRun syncs increments; a constant fixed value cannot work there.
                val enabled = !(target == AppTarget.WECHAT && item == StepMode.REPLACE)
                SegmentedButton(
                    selected = item == mode,
                    onClick = { onModeChange(item) },
                    shape = SegmentedButtonDefaults.itemShape(index, StepMode.entries.size),
                    enabled = enabled,
                    label = {
                        Text(
                            text = stringResource(item.shortLabelRes()),
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Text(
            text = stringResource(mode.descRes()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.mode_help_title)) },
            text = { Text(helpDescriptions.joinToString("\n\n")) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun StepEditor(
    settings: TargetSettings,
    effectiveLimit: Int,
    onValueChange: (Int) -> Unit,
    onMaxStepsChange: (Int) -> Unit,
    onDraftMaxStepsChange: (Int?) -> Unit
) {
    val maxValue = TargetSettings.maxValueFor(settings.mode)
    val step = stepSize(settings.mode)
    var text by rememberSaveable { mutableStateOf(settings.value.toString()) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val decreaseDescription = stringResource(R.string.step_decrease)
    val increaseDescription = stringResource(R.string.step_increase)

    // Reflect external changes (reset, mode switch, remote edits) unless the user is editing.
    LaunchedEffect(settings.value, focused) {
        if (!focused && !dirty) text = settings.value.toString()
    }
    LaunchedEffect(settings.mode) {
        text = settings.value.toString()
        dirty = false
    }

    val parsed = text.toIntOrNull()
    val isError = parsed == null || parsed < TargetSettings.MIN_VALUE || parsed > maxValue

    val baseValue = text.toIntOrNull()
        ?.takeIf { it in TargetSettings.MIN_VALUE..maxValue }
        ?: settings.value

    val commit: () -> Unit = {
        val value = text.toIntOrNull()
        if (value != null && value in TargetSettings.MIN_VALUE..maxValue) {
            if (value != settings.value) onValueChange(value)
            text = value.toString()
        } else {
            text = settings.value.toString()
        }
        dirty = false
    }

    val applyValue: (Int) -> Unit = { candidate ->
        val clamped = TargetSettings.clamp(settings.mode, candidate)
        text = clamped.toString()
        dirty = false
        if (clamped != settings.value) onValueChange(clamped)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepButton(
                symbol = "\u2212",
                description = decreaseDescription,
                enabled = baseValue > TargetSettings.MIN_VALUE,
                onStep = { applyValue(baseValue - step) }
            )

            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input.filter { it in '0'..'9' }.take(7)
                    dirty = true
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && focused) {
                            if (dirty) commit() else text = settings.value.toString()
                        }
                        focused = focusState.isFocused
                    },
                label = { Text(stringResource(R.string.label_value)) },
                suffix = { Text(stringResource(settings.mode.unitRes())) },
                supportingText = if (isError) {
                    {
                        Text(
                            text = stringResource(
                                R.string.invalid_value,
                                TargetSettings.MIN_VALUE,
                                maxValue
                            )
                        )
                    }
                } else {
                    null
                },
                isError = isError,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        commit()
                        focusManager.clearFocus()
                    }
                )
            )

            StepButton(
                symbol = "+",
                description = increaseDescription,
                enabled = baseValue < maxValue,
                onStep = { applyValue(baseValue + step) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetsFor(settings.mode).forEach { preset ->
                FilterChip(
                    selected = settings.value == preset,
                    onClick = { applyValue(preset) },
                    label = { Text(presetLabel(settings.mode, preset)) }
                )
            }
        }

        Text(
            text = previewText(settings.mode, settings.value, effectiveLimit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        MaxStepsSection(
            maxSteps = settings.maxSteps,
            effectiveLimit = effectiveLimit,
            onDraftMaxStepsChange = onDraftMaxStepsChange,
            onMaxStepsChange = onMaxStepsChange
        )
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onStep: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val latestStep by rememberUpdatedState(onStep)

    // Single tap steps once; holding the button keeps stepping after a short delay.
    // [latestStep] always references the newest value, so the repeat loop keeps advancing.
    LaunchedEffect(pressed, enabled) {
        if (!pressed || !enabled) return@LaunchedEffect
        latestStep()
        delay(400)
        while (true) {
            latestStep()
            delay(80)
        }
    }

    val container = if (enabled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (enabled) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = container,
        contentColor = content,
        shape = CircleShape,
        modifier = Modifier
            .size(48.dp)
            .semantics {
                contentDescription = description
                role = Role.Button
                onClick(label = description) {
                    if (enabled) onStep()
                    enabled
                }
            }
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (!enabled) return@detectTapGestures
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = symbol, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun MaxStepsSection(maxSteps: Int, effectiveLimit: Int, onDraftMaxStepsChange: (Int?) -> Unit, onMaxStepsChange: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_max_steps),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatNumber(effectiveLimit) + " " + stringResource(R.string.unit_steps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            MaxStepsField(
                maxSteps = maxSteps,
                onDraftChange = onDraftMaxStepsChange,
                onMaxStepsChange = onMaxStepsChange
            )
        }
    }
}

@Composable
private fun MaxStepsField(maxSteps: Int, onDraftChange: (Int?) -> Unit, onMaxStepsChange: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf(maxSteps.toString()) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val upperBound = TargetSettings.MAX_VALUE

    // Reflect external changes (reset, remote edits) unless the user is editing.
    LaunchedEffect(maxSteps, focused) {
        if (!focused && !dirty) text = maxSteps.toString()
    }

    val draftValue = text.toIntOrNull()?.takeIf { it in TargetSettings.MIN_MAX_STEPS..upperBound }
    val isError = draftValue == null

    val commit: (String) -> Unit = { raw ->
        val value = raw.toIntOrNull()
        val next = if (value != null && value in TargetSettings.MIN_MAX_STEPS..upperBound) {
            value
        } else {
            maxSteps
        }
        if (next != maxSteps) onMaxStepsChange(next)
        text = next.toString()
        dirty = false
    }

    // The typed value is published as a draft immediately, so the card summary, the preview and the
    // daily-max row refresh while the user is still typing instead of after the edit is committed.
    LaunchedEffect(text) { onDraftChange(draftValue) }

    // Persist once the input settles, and flush an unfinished edit when the field leaves composition
    // (collapsed section, collapsed card or another screen).
    LaunchedEffect(text, dirty) {
        if (!dirty) return@LaunchedEffect
        delay(MAX_STEPS_COMMIT_DELAY_MS)
        commit(text)
    }
    val latestCommit by rememberUpdatedState(commit)
    val latestDraftChange by rememberUpdatedState(onDraftChange)
    DisposableEffect(Unit) {
        onDispose {
            latestDraftChange(null)
            if (dirty) latestCommit(text)
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter { it in '0'..'9' }.take(7)
            dirty = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .onFocusChanged { focusState ->
                if (!focusState.isFocused && focused && dirty) {
                    commit(text)
                }
                focused = focusState.isFocused
            },
        label = { Text(stringResource(R.string.label_max_steps_base)) },
        suffix = { Text(stringResource(R.string.unit_steps)) },
        supportingText = {
            Text(
                text = if (isError) {
                    stringResource(
                        R.string.invalid_value,
                        TargetSettings.MIN_MAX_STEPS,
                        upperBound
                    )
                } else {
                    stringResource(R.string.max_steps_effective_hint)
                }
            )
        },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                commit(text)
                focusManager.clearFocus()
            }
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionSheet(state: SettingsState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.status_sheet_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (state.connected) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (state.connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = stringResource(
                        if (state.connected) R.string.status_connected else R.string.status_disconnected
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (state.connected) {
                Text(
                    text = stringResource(
                        R.string.status_connected_detail,
                        state.frameworkName ?: "-",
                        state.frameworkVersion ?: "-",
                        state.apiVersion ?: 0
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.status_disconnected_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.status_not_synced),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    }
}

@Composable
private fun previewText(mode: StepMode, value: Int, effectiveLimit: Int): String {
    val sampleSteps = 1_000
    if (mode == StepMode.AUTO) {
        return stringResource(
            R.string.preview_auto,
            formatNumber(value),
            formatNumber(TargetSettings.AUTO_CAP)
        )
    }

    // Show what the strategy itself produces; the effective daily limit is displayed once, in the
    // daily-max section, so capping is only flagged here instead of repeating the number.
    val result = when (mode) {
        StepMode.MULTIPLY -> sampleSteps.toLong() * value
        StepMode.ADD -> sampleSteps.toLong() + value
        StepMode.REPLACE, StepMode.AUTO -> value.toLong()
    }
    val text = when (mode) {
        StepMode.MULTIPLY, StepMode.ADD -> stringResource(
            R.string.preview_target,
            formatNumber(sampleSteps),
            formatNumber(result)
        )

        StepMode.REPLACE, StepMode.AUTO -> stringResource(R.string.preview_replace, formatNumber(result))
    }
    return if (result > effectiveLimit) {
        text + " · " + stringResource(R.string.preview_capped)
    } else {
        text
    }
}

@Composable
private fun presetLabel(mode: StepMode, value: Int): String = if (mode == StepMode.MULTIPLY) {
    stringResource(R.string.preset_multiply, formatNumber(value))
} else {
    stringResource(R.string.preset_value, formatNumber(value))
}

private fun stepSize(mode: StepMode): Int = if (mode == StepMode.MULTIPLY) 10 else 1_000

private fun presetsFor(mode: StepMode): List<Int> = when (mode) {
    StepMode.MULTIPLY -> listOf(10, 50, 100, 500)
    StepMode.ADD -> listOf(5_000, 10_000, 20_000, 50_000)
    StepMode.AUTO -> listOf(5_000, 10_000, 20_000)
    StepMode.REPLACE -> listOf(20_000, 50_000, 80_000)
}

/** Delay after the last keystroke before the typed daily max is written to the preferences. */
private const val MAX_STEPS_COMMIT_DELAY_MS = 600L

private fun formatNumber(value: Int): String = formatNumber(value.toLong())

private fun formatNumber(value: Long): String = NumberFormat.getIntegerInstance().format(value)

@Composable
private fun Footer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.footer_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.footer_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
