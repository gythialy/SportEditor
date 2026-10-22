package io.github.gythialy.sporteditor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gythialy.sporteditor.BuildConfig
import io.github.gythialy.sporteditor.R
import io.github.gythialy.sporteditor.data.AppTarget
import io.github.gythialy.sporteditor.data.SettingsState
import io.github.gythialy.sporteditor.data.StepMode
import io.github.gythialy.sporteditor.data.TargetSettings
import java.text.NumberFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onEnabledChange: (AppTarget, Boolean) -> Unit,
    onModeChange: (AppTarget, StepMode) -> Unit,
    onValueChange: (AppTarget, Int) -> Unit,
    onMaxStepsChange: (AppTarget, Int) -> Unit,
    onReset: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val resetMessage = stringResource(R.string.action_reset_done)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenDiagnostics) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.debug_open)
                        )
                    }
                    TextButton(
                        onClick = {
                            // Commit any pending edit before defaults replace it.
                            focusManager.clearFocus()
                            onReset()
                            scope.launch { snackbarHostState.showSnackbar(resetMessage) }
                        }
                    ) {
                        Text(stringResource(R.string.action_reset))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ServiceStatusCard(state) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.section_apps),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.section_apps_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(AppTarget.entries.toList(), key = { it.key }) { target ->
                val iconState = rememberAppIcon(target)
                TargetCard(
                    target = target,
                    iconState = iconState,
                    settings = state.settingsFor(target),
                    onEnabledChange = { onEnabledChange(target, it) },
                    onModeChange = { onModeChange(target, it) },
                    onValueChange = { onValueChange(target, it) },
                    onMaxStepsChange = { onMaxStepsChange(target, it) }
                )
            }

            item { LanguageCard(language = language, onLanguageChange = onLanguageChange) }

            item { Footer() }
        }
    }
}

@Composable
private fun LanguageCard(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.language_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.language_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { option ->
                    FilterChip(
                        selected = option == language,
                        onClick = { onLanguageChange(option) },
                        label = { Text(stringResource(option.labelRes)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceStatusCard(state: SettingsState) {
    val connected = state.connected
    val containerColor = if (connected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (connected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (connected) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = contentColor
                )
                Text(
                    text = stringResource(
                        if (connected) R.string.status_connected else R.string.status_disconnected
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor
                )
            }

            if (connected && state.frameworkName != null) {
                Text(
                    text = stringResource(
                        R.string.status_connected_detail,
                        state.frameworkName,
                        state.frameworkVersion ?: "-",
                        state.apiVersion ?: 0
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            } else {
                Text(
                    text = stringResource(R.string.status_disconnected_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
                Text(
                    text = stringResource(R.string.status_not_synced),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun TargetCard(
    target: AppTarget,
    iconState: AppIconState,
    settings: TargetSettings,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (StepMode) -> Unit,
    onValueChange: (Int) -> Unit,
    onMaxStepsChange: (Int) -> Unit
) {
    val appLabel = stringResource(target.labelRes())
    val toggleDescription = stringResource(R.string.toggle_app, appLabel)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(target = target, state = iconState)

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.module_package_label, target.packageName),
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

                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.semantics { contentDescription = toggleDescription }
                )
            }

            if (settings.enabled) {
                ModeSelector(
                    target = target,
                    mode = settings.mode,
                    onModeChange = onModeChange
                )
                StepEditor(
                    settings = settings,
                    onValueChange = onValueChange,
                    onMaxStepsChange = onMaxStepsChange
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(target: AppTarget, mode: StepMode, onModeChange: (StepMode) -> Unit) {
    var showHelp by remember { mutableStateOf(false) }
    val helpDescriptions = StepMode.entries.map { item ->
        stringResource(item.labelRes()) + "：" + stringResource(item.descRes())
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            TextButton(onClick = { showHelp = true }) {
                Text(stringResource(R.string.mode_help))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StepMode.entries.forEach { item ->
                FilterChip(
                    selected = item == mode,
                    onClick = { onModeChange(item) },
                    // WeRun syncs increments; a constant fixed value cannot work there.
                    enabled = !(target == AppTarget.WECHAT && item == StepMode.REPLACE),
                    label = { Text(stringResource(item.labelRes())) }
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
private fun StepEditor(settings: TargetSettings, onValueChange: (Int) -> Unit, onMaxStepsChange: (Int) -> Unit) {
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = { applyValue(baseValue - step) },
                enabled = baseValue > TargetSettings.MIN_VALUE,
                modifier = Modifier.semantics { contentDescription = decreaseDescription }
            ) {
                Text(text = "\u2212", style = MaterialTheme.typography.titleLarge)
            }

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
                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
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

            FilledTonalIconButton(
                onClick = { applyValue(baseValue + step) },
                enabled = baseValue < maxValue,
                modifier = Modifier.semantics { contentDescription = increaseDescription }
            ) {
                Text(text = "+", style = MaterialTheme.typography.titleLarge)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetsFor(settings.mode).forEach { preset ->
                SuggestionChip(
                    onClick = { applyValue(preset) },
                    label = { Text(presetLabel(settings.mode, preset)) }
                )
            }
        }

        Text(
            text = previewText(settings.mode, settings.value, settings.maxSteps),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        MaxStepsField(
            maxSteps = settings.maxSteps,
            onMaxStepsChange = onMaxStepsChange
        )
    }
}

@Composable
private fun MaxStepsField(maxSteps: Int, onMaxStepsChange: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf(maxSteps.toString()) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val upperBound = TargetSettings.MAX_VALUE

    LaunchedEffect(maxSteps, focused) {
        if (!focused && !dirty) text = maxSteps.toString()
    }

    val parsed = text.toIntOrNull()
    val isError = parsed == null || parsed < TargetSettings.MIN_MAX_STEPS || parsed > upperBound

    val commit: () -> Unit = {
        val value = text.toIntOrNull()
        if (value != null && value in TargetSettings.MIN_MAX_STEPS..upperBound) {
            if (value != maxSteps) onMaxStepsChange(value)
            text = value.toString()
        } else {
            text = maxSteps.toString()
        }
        dirty = false
    }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter { it in '0'..'9' }.take(7)
            dirty = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (!focusState.isFocused && focused) {
                    if (dirty) commit() else text = maxSteps.toString()
                }
                focused = focusState.isFocused
            },
        label = { Text(stringResource(R.string.label_max_steps)) },
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
                    stringResource(R.string.max_steps_hint)
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
                commit()
                focusManager.clearFocus()
            }
        )
    )
}

@Composable
private fun previewText(mode: StepMode, value: Int, maxSteps: Int): String {
    val sampleSteps = 1_000
    val result = when (mode) {
        StepMode.MULTIPLY -> (sampleSteps.toLong() * value)
            .coerceAtMost(TargetSettings.MAX_VALUE.toLong())
            .toInt()

        StepMode.ADD -> (sampleSteps.toLong() + value)
            .coerceAtMost(TargetSettings.MAX_VALUE.toLong())
            .toInt()

        StepMode.REPLACE -> value

        StepMode.AUTO -> 0
    }.coerceAtMost(maxSteps)
    return when (mode) {
        StepMode.MULTIPLY, StepMode.ADD -> stringResource(
            R.string.preview_target,
            formatNumber(sampleSteps),
            formatNumber(result)
        )

        StepMode.AUTO -> stringResource(
            R.string.preview_auto,
            formatNumber(value),
            formatNumber(TargetSettings.AUTO_CAP)
        )

        StepMode.REPLACE -> stringResource(R.string.preview_replace, formatNumber(value))
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

private fun formatNumber(value: Int): String = NumberFormat.getIntegerInstance().format(value)

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
            text = stringResource(R.string.footer_scope),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
