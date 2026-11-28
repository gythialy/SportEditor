package io.github.gythialy.sporteditor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonSize
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gythialy.sporteditor.BuildConfig
import io.github.gythialy.sporteditor.R
import io.github.gythialy.sporteditor.data.AppTarget
import io.github.gythialy.sporteditor.data.DailyLimit
import io.github.gythialy.sporteditor.data.SettingsState
import io.github.gythialy.sporteditor.data.StepMode
import io.github.gythialy.sporteditor.data.TargetSettings
import io.github.gythialy.sporteditor.ui.art.EffectiveLimitStrip
import io.github.gythialy.sporteditor.ui.art.LinkBrokenArt
import io.github.gythialy.sporteditor.ui.theme.Dimensions
import io.github.gythialy.sporteditor.ui.theme.Motion
import io.github.gythialy.sporteditor.ui.theme.alertWash
import io.github.gythialy.sporteditor.ui.theme.animationsEnabled
import io.github.gythialy.sporteditor.ui.theme.brandAccent
import io.github.gythialy.sporteditor.ui.theme.cardBorderColor
import io.github.gythialy.sporteditor.ui.theme.cardContainerColor
import io.github.gythialy.sporteditor.ui.theme.cardShape
import io.github.gythialy.sporteditor.ui.theme.contentInset
import io.github.gythialy.sporteditor.ui.theme.rememberMorphShape
import io.github.gythialy.sporteditor.ui.theme.windowWidthDp
import io.github.gythialy.sporteditor.ui.theme.withTabularNumerals
import java.text.NumberFormat
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
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
    // Tablets: keep the column at a readable measure instead of stretching cards edge to edge.
    val sideInset = contentInset(windowWidthDp())

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                // The expressive bar owns the two-line title now: a title slot and a subtitle
                // slot, instead of a hand-rolled Column that had to be re-measured on every
                // change to the bar's height or insets.
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subtitle = {
                    Text(
                        text = stringResource(
                            R.string.home_subtitle,
                            AppTarget.entries.size,
                            enabledCount
                        ),
                        style = MaterialTheme.typography.labelMediumEmphasized.withTabularNumerals(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    // One control in the app bar: the split button reports the connection and its
                    // menu carries both follow-up destinations, so the settings gear is gone.
                    ConnectionSplitButton(
                        connected = state.connected,
                        onClick = { showConnectionSheet = true },
                        onOpenSettings = onOpenSettings
                    )
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
                // Keeps the column at a readable measure on tablets instead of stretching cards to
                // the full width of a 1280 dp window.
                .padding(horizontal = sideInset)
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
                    // The switch owns the stored flag only; AppCard orchestrates the expansion so the
                    // thumb leads the layout change instead of both starting on the same frame.
                    onEnabledChange = { enabled -> onEnabledChange(target, enabled) },
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
private fun ConnectionSplitButton(connected: Boolean, onClick: () -> Unit, onOpenSettings: () -> Unit) {
    val container = if (connected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        alertWash()
    }
    val content = if (connected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.error
    }
    // Brand anchor: the one place the app's own green stays visible under dynamic colour.
    val dot = if (connected) brandAccent() else MaterialTheme.colorScheme.error
    val label = stringResource(
        if (connected) R.string.status_badge_connected else R.string.status_badge_disconnected
    )
    val detailLabel = stringResource(
        if (connected) R.string.action_view_connection else R.string.action_view_steps
    )
    // At 320 dp the app-bar title and this control were left about 18 dp between them; the dot
    // alone still carries the state, and the full label stays available to TalkBack.
    val compact = windowWidthDp() < 360.dp

    Box {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    // The sheet carries both the first-run steps and the live framework details,
                    // so the leading half stays actionable in both states. Only its label changes.
                    onClick = onClick,
                    modifier = Modifier.semantics {
                        if (compact) contentDescription = label
                        onClick(label = detailLabel) {
                            onClick()
                            true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = container,
                        contentColor = content
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dot)
                    )
                    if (!compact) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                }
            },
            // Split button, not a decorated pill: the leading half reports the state and opens the
            // connection sheet, the trailing half goes straight to the settings. No menu in
            // between: a "view steps" item would only repeat what the leading half already does,
            // and a one-item menu costs a tap for nothing.
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = container,
                        contentColor = content
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
    }
}

/**
 * Reveals one piece of card content slightly after the previous one, so the disconnected card
 * builds up instead of popping in as a single block.
 *
 * The Compose animation clock follows the system "remove animations" setting, so this collapses to
 * an instant reveal when the user turns animations off.
 */
@Composable
private fun StaggeredAppear(index: Int, content: @Composable () -> Unit) {
    val animations = animationsEnabled()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    // The stagger is a coroutine delay, which the system animation scale does not touch, so with
    // animations off the rows simply fade in together instead of arriving one after another.
    val delay = if (animations) index * Motion.STAGGER_STEP_MS.toInt() else 0
    val enter = if (animations) {
        fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = delay)) +
            slideInVertically(animationSpec = tween(durationMillis = 220, delayMillis = delay)) { it / 6 }
    } else {
        fadeIn(animationSpec = tween(durationMillis = 0))
    }

    AnimatedVisibility(visible = shown, enter = enter) {
        content()
    }
}

/**
 * One line of state, one line of what to do. The full instructions live in the connection sheet
 * this card opens, so the card no longer restates them (it used to print the same two paragraphs
 * the sheet prints, in a panel that took a third of the first screen).
 */
@Composable
private fun DisconnectedCard(onViewSteps: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewSteps),
        shape = cardShape(),
        colors = CardDefaults.cardColors(
            containerColor = alertWash(),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Card),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // The illustration survives as a thin strip: it still says "the link is broken" at a
            // glance, without the 40 dp banner it used to occupy.
            StaggeredAppear(0) {
                LinkBrokenArt(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            StaggeredAppear(1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.Tight)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.status_disconnected),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.action_view_steps),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            StaggeredAppear(2) {
                Text(
                    text = stringResource(R.string.status_disconnected_hint_short),
                    style = MaterialTheme.typography.bodySmall
                )
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
    val animations = animationsEnabled()
    val scope = rememberCoroutineScope()
    var expandJob by remember { mutableStateOf<Job?>(null) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    // Draft typed into the daily-max field. It drives every displayed number immediately, while the
    // preference itself is written once the edit settles (debounce, IME done, focus loss or collapse).
    var draftMaxSteps by remember { mutableStateOf<Int?>(null) }
    val pendingMaxSteps = draftMaxSteps?.takeIf { it != settings.maxSteps }
    val effectiveLimit = DailyLimit.effectiveToday(pendingMaxSteps ?: settings.maxSteps)

    // The readout cascade. One animated value feeds the big number, the card summary and the
    // conversion preview, so a change reads as a single instrument settling instead of three
    // unrelated text swaps. It starts at the stored value, so nothing animates on first composition.
    val readout = remember { Animatable(settings.value.toFloat()) }
    var readoutMode by remember { mutableStateOf(settings.mode) }
    // Captured here: the motion scheme is a composable read, and the effect below is not.
    val settleSpec = Motion.settle()
    LaunchedEffect(settings.value, settings.mode) {
        if (settings.mode != readoutMode) {
            // Switching strategy changes what the number means (a multiplier became an amount), so
            // the readout jumps instead of counting through values it never held.
            readoutMode = settings.mode
            readout.snapTo(settings.value.toFloat())
        } else {
            readout.animateTo(settings.value.toFloat(), settleSpec)
        }
    }
    val readoutValue = readout.value.roundToInt()
    val summary = summaryText(settings, readoutValue)
    val expandedState = stringResource(R.string.state_expanded)
    val collapsedState = stringResource(R.string.state_collapsed)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = Motion.press(),
        label = "card-chevron"
    )

    // Drop a stale draft when the stored value changed elsewhere (reset action, remote edit).
    LaunchedEffect(settings.maxSteps) {
        if (draftMaxSteps != null && draftMaxSteps != settings.maxSteps) draftMaxSteps = null
    }

    Card(
        onClick = { onExpandChange(!expanded) },
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = if (expanded) expandedState else collapsedState
            },
        shape = cardShape(),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor(),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, cardBorderColor()),
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
                            // The "not installed" badge is measured first, so a long
                            // summary is ellipsized instead of pushing the badge out.
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // One arrow that turns, in both states. Previously the open card swapped it for a
                // MoreVert button, which put a third control in the header and moved the signal
                // sideways; the reset menu now lives with the other per-card meta action.
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )

                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { enabled ->
                        onEnabledChange(enabled)
                        expandJob?.cancel()
                        expandJob = scope.launch {
                            // Let the switch thumb travel before the card grows, so one change reads
                            // as cause then consequence. Collapsing happens immediately.
                            if (enabled && animations) delay(Motion.SWITCH_LEAD_MS)
                            onExpandChange(enabled)
                        }
                    },
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
                    ) {
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
                    }
                    StepEditor(
                        target = target,
                        settings = settings,
                        effectiveLimit = effectiveLimit,
                        readoutValue = readoutValue,
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

/**
 * What the card says under the app name. The rule stays visible when the switch is off — the
 * switch already says "off", while the rule is what the user came to check.
 */
@Composable
private fun summaryText(settings: TargetSettings, value: Int): String {
    val formatted = formatNumber(value)
    return when (settings.mode) {
        StepMode.MULTIPLY -> stringResource(R.string.app_summary_multiply, formatted)
        StepMode.ADD -> stringResource(R.string.app_summary_add, formatted)
        StepMode.AUTO -> stringResource(R.string.app_summary_auto, formatted)
        StepMode.REPLACE -> stringResource(R.string.app_summary_replace, formatted)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(
    target: AppTarget,
    mode: StepMode,
    onModeChange: (StepMode) -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    // WeRun syncs increments, so a constant fixed value can never work there: the option is not
    // offered at all instead of being shown disabled.
    val modes = StepMode.entries.filter { !(target == AppTarget.WECHAT && it == StepMode.REPLACE) }
    // Four labels do not fit at 320 dp once the row has to share the width equally, so the short
    // set takes over; with three segments the full words fit.
    // Four labels do not fit at 320 dp once the group adds its own item padding, so the short set
    // takes over; with three segments the full words fit. The recommendation marker is a text run
    // rather than an icon slot: an icon costs ~20 dp per item and that was what pushed the fourth
    // segment past the group's width.
    val abbrev = modes.size > 3
    val star = stringResource(R.string.mode_recommended)
    // The star is a badge drawn over the button, not part of its label: putting it in the string
    // cost width inside an equal quarter of the row and clipped the option's own name.
    val modeLabels = modes.associateWith { mode ->
        stringResource(if (abbrev) mode.shortLabelRes() else mode.labelRes())
    }

    // A stored REPLACE from an older version would leave no selected option; snap it back.
    LaunchedEffect(target, mode) {
        if (target == AppTarget.WECHAT && mode == StepMode.REPLACE) {
            onModeChange(StepMode.MULTIPLY)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_mode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            trailing()
        }

        // Connected toggle buttons rather than material3's ButtonGroup: 1.5.0-rc01's group
        // measures its overflow button with `constraints.copy(maxWidth = remainingSpace +
        // overflowWidth)`, which throws "maxWidth must be >= minWidth" whenever a full-width group
        // has to fold. Hand-rolling the row also gives each segment an equal quarter of the width,
        // and leaves room for the recommendation badge above the label.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            modes.forEachIndexed { index, item ->
                val shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        checked = item == mode,
                        onCheckedChange = { onModeChange(item) },
                        buttonSize = ToggleButtonSize.Medium,
                        shapes = shapes,
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = modeLabels.getValue(item),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    // The recommendation is a corner badge, not part of the label: a superscript
                    // star stays out of the text run, so the label keeps its centred position and
                    // the badge never competes with the label for width.
                    if (item == target.recommendedMode()) {
                        Text(
                            text = star,
                            style = MaterialTheme.typography.labelSmall,
                            // The badge inherits the button's own content colour when the option is
                            // checked (the pill is a light `primary` tint, so anything lighter
                            // vanishes), and takes the tertiary accent when it sits on the neutral
                            // surface instead.
                            color = if (item == mode) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-12).dp, y = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepEditor(
    target: AppTarget,
    settings: TargetSettings,
    effectiveLimit: Int,
    readoutValue: Int,
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

    // While the draft is dirty the field must show the literal text the user is typing; otherwise it
    // shows the animated readout, which is what makes the number, the preview and the card summary
    // settle together after a preset or a step.
    val editing = dirty
    val displayValue = if (editing) text else readoutValue.toString()

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
                value = displayValue,
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
                // The unit used to sit in the suffix slot, pinned to the far border away from the
                // number it describes. Naming it in the label keeps the value alone and centred.
                label = {
                    Text(
                        stringResource(
                            if (settings.mode == StepMode.MULTIPLY) {
                                R.string.mode_multiply
                            } else {
                                R.string.label_value
                            }
                        )
                    )
                },
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
                // Tabular figures keep the counting readout from shifting sideways as digits change.
                textStyle = MaterialTheme.typography.displaySmallEmphasized.copy(
                    textAlign = TextAlign.Center,
                    fontFeatureSettings = "tnum"
                ),
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
                    label = { Text(presetLabel(settings.mode, preset)) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        Text(
            // The preview counts with the readout, so "×50 → 50,000" is visibly derived from the
            // number above instead of appearing in the same frame as an unrelated claim.
            text = previewText(
                mode = settings.mode,
                value = if (editing) settings.value else readoutValue,
                effectiveLimit = effectiveLimit
            ),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onStep: () -> Unit) {
    val enabledState = stringResource(if (enabled) R.string.state_enabled else R.string.state_disabled)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
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

    // Neutral surface, not primaryContainer: the Pine Quota reserves the accent for the selected
    // mode, the current readout and the limit marker. Enabled/disabled still differ in tone.
    val container = if (enabled) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // A held button morphs from the circle it rests as into a 20 dp rounded square: the repeat has
    // a 400 ms lead-in, and without this the control looks dead for that whole beat. Shape rather
    // than scale so the acknowledgement reads as the control changing state, not shrinking.
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = Motion.press(),
        label = "step-press"
    )
    val shape = rememberMorphShape(MaterialShapes.Circle, MaterialShapes.Square, pressProgress)

    Surface(
        color = container,
        contentColor = content,
        shape = shape,
        modifier = Modifier
            .size(56.dp)
            .clickable(
                enabled = enabled,
                onClickLabel = description,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple()
            ) { onStep() }
            .semantics {
                contentDescription = description
                stateDescription = enabledState
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
    val expandedState = stringResource(R.string.state_expanded)
    val collapsedState = stringResource(R.string.state_collapsed)

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp)
                .heightIn(min = 48.dp)
                .semantics {
                    role = Role.Button
                    stateDescription = if (expanded) expandedState else collapsedState
                },
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
                color = MaterialTheme.colorScheme.onSurface
            )
            // One arrow that turns, instead of swapping glyphs: the rotation carries "this opened"
            // and costs no layout change.
            val arrowRotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = Motion.press(),
                label = "max-steps-arrow"
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(arrowRotation)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val offset = (effectiveLimit - maxSteps).coerceIn(0, DailyLimit.JITTER_LIMIT)
            Column {
                MaxStepsField(
                    maxSteps = maxSteps,
                    onDraftChange = onDraftMaxStepsChange,
                    onMaxStepsChange = onMaxStepsChange
                )
                // Shows where today's effective value sits inside the deterministic random range,
                // so the "上限 80,037" number above the section is no longer a surprise.
                EffectiveLimitStrip(
                    baseLabel = stringResource(R.string.max_steps_base_value, formatNumber(maxSteps)),
                    offsetLabel = stringResource(R.string.max_steps_today_offset, offset),
                    fraction = offset / DailyLimit.JITTER_LIMIT.toFloat(),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
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

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input.filter { it in '0'..'9' }.take(7)
                dirty = true
            },
            // A five- to seven-digit number does not need the full card width; right-aligned it also
            // lines its digits up with the effective value above.
            modifier = Modifier.widthIn(max = 220.dp)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionSheet(state: SettingsState, onDismiss: () -> Unit) {
    // The old rememberModalBottomSheetState(skipPartiallyExpanded = true) is deprecated: the
    // anchors are now declared explicitly instead of inferred from the sheet height.
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

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
        return stringResource(R.string.preview_auto, formatNumber(value))
    }

    // Plain arithmetic, no "capped" suffix: the daily-max section directly below owns the cap, and
    // a suffix here only repeated it. The clamp stays for REPLACE, where the value is absolute.
    val result = when (mode) {
        StepMode.MULTIPLY -> sampleSteps.toLong() * value
        StepMode.ADD -> sampleSteps.toLong() + value
        StepMode.REPLACE, StepMode.AUTO -> value.toLong().coerceAtMost(effectiveLimit.toLong())
    }
    return when (mode) {
        StepMode.MULTIPLY, StepMode.ADD -> stringResource(
            R.string.preview_target,
            formatNumber(sampleSteps),
            formatNumber(result)
        )

        StepMode.REPLACE, StepMode.AUTO -> stringResource(R.string.preview_replace, formatNumber(result))
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
