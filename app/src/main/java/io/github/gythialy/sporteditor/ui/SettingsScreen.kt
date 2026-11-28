package io.github.gythialy.sporteditor.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gythialy.sporteditor.BuildConfig
import io.github.gythialy.sporteditor.R
import io.github.gythialy.sporteditor.ui.theme.Dimensions
import io.github.gythialy.sporteditor.ui.theme.Motion
import io.github.gythialy.sporteditor.ui.theme.Signal
import io.github.gythialy.sporteditor.ui.theme.animationsEnabled
import io.github.gythialy.sporteditor.ui.theme.brandAccent
import io.github.gythialy.sporteditor.ui.theme.cardBorderColor
import io.github.gythialy.sporteditor.ui.theme.cardContainerColor
import io.github.gythialy.sporteditor.ui.theme.cardShape
import io.github.gythialy.sporteditor.ui.theme.contentInset
import io.github.gythialy.sporteditor.ui.theme.windowWidthDp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    updateState: UpdateUiState,
    onCheckUpdate: () -> Unit,
    onOpenRelease: (String) -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    // The intro is a one-shot: once it has run, re-composition and navigation must not replay it.
    var groupsEntered by remember { mutableStateOf(false) }

    val sideInset = contentInset(windowWidthDp())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = sideInset),
            // Same inset and group rhythm as the home screen; the two lists used to breathe
            // differently for no reason a reader could see.
            contentPadding = PaddingValues(
                start = Dimensions.Page,
                end = Dimensions.Page,
                top = 4.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Dimensions.Gap)
        ) {
            item { SectionLabel(R.string.section_general) }
            item {
                SettingsGroup(
                    index = 0,
                    introPlayed = groupsEntered,
                    onIntroPlayed = { groupsEntered = true }
                ) {
                    SettingsRow(
                        icon = Icons.Filled.Language,
                        title = stringResource(R.string.language_title),
                        subtitle = stringResource(language.labelRes),
                        onClick = { showLanguageDialog = true }
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.theme_title),
                        subtitle = stringResource(themeMode.labelRes),
                        onClick = { showThemeDialog = true }
                    )
                    // Dynamic colour exists only from Android 12: below that the system has no
                    // palette to hand out, so the row would be a switch that does nothing.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        RowDivider()
                        SettingsSwitchRow(
                            icon = Icons.Filled.Wallpaper,
                            title = stringResource(R.string.dynamic_color_title),
                            subtitle = stringResource(R.string.dynamic_color_desc),
                            checked = dynamicColor,
                            onCheckedChange = onDynamicColorChange,
                            showSwatches = true
                        )
                    }
                    RowDivider()
                    // Diagnostics sits above the update row: it is the everyday troubleshooting
                    // entry, while "check for updates" is a rarely used network action.
                    SettingsRow(
                        icon = Icons.Filled.BugReport,
                        title = stringResource(R.string.debug_title),
                        subtitle = stringResource(R.string.debug_desc),
                        onClick = onOpenDiagnostics
                    )
                    RowDivider()
                    UpdateRow(
                        updateState = updateState,
                        onCheckUpdate = onCheckUpdate,
                        onOpenRelease = onOpenRelease
                    )
                }
            }

            item {
                SectionLabel(
                    labelRes = R.string.about_title,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            item {
                SettingsGroup(
                    index = 1,
                    introPlayed = groupsEntered,
                    onIntroPlayed = { groupsEntered = true }
                ) {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        title = stringResource(R.string.about_homepage),
                        subtitle = HOMEPAGE_URL,
                        onClick = { onOpenUrl(HOMEPAGE_URL) }
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Filled.Description,
                        title = stringResource(R.string.about_license),
                        subtitle = "MIT",
                        onClick = { onOpenUrl(LICENSE_URL) }
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ChoiceDialog(
            title = stringResource(R.string.theme_title),
            options = ThemeMode.entries,
            selected = themeMode,
            labelFor = { stringResource(it.labelRes) },
            onDismiss = { showThemeDialog = false },
            onSelect = { option ->
                showThemeDialog = false
                if (option != themeMode) onThemeModeChange(option)
            }
        )
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title = stringResource(R.string.language_title),
            options = AppLanguage.entries,
            selected = language,
            labelFor = { stringResource(it.labelRes) },
            onDismiss = { showLanguageDialog = false },
            onSelect = { option ->
                showLanguageDialog = false
                if (option != language) onLanguageChange(option)
            }
        )
    }
}

@Composable
private fun SectionLabel(labelRes: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // Flush with the card below it. The 4 dp nudge put the label on no axis at all: not the
        // card edge, not the row icon, not the row title.
        modifier = modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun SettingsGroup(index: Int, introPlayed: Boolean, onIntroPlayed: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val animations = animationsEnabled()
    // Captured once so a later group state change cannot restart an already finished intro.
    val animateEntry = remember { animations && !introPlayed }
    var visible by remember { mutableStateOf(!animateEntry) }
    LaunchedEffect(animateEntry) {
        if (animateEntry) {
            // A coroutine delay: the system duration scale does not reach it, so animations off
            // has to skip it entirely rather than scaling it to zero.
            delay(index * Motion.ENERGY_STAGGER_MS)
            visible = true
            onIntroPlayed()
        }
    }
    val enter = if (animateEntry) {
        fadeIn(animationSpec = Motion.enter()) +
            slideInVertically(animationSpec = Motion.enter()) { it / 8 }
    } else {
        EnterTransition.None
    }
    AnimatedVisibility(visible = visible, enter = enter, exit = ExitTransition.None) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape(),
            colors = CardDefaults.cardColors(
                containerColor = cardContainerColor()
            ),
            border = BorderStroke(1.dp, cardBorderColor()),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** A settings row whose trailing control is a switch instead of a disclosure chevron. */
@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showSwatches: Boolean = false
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = { Text(title) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = subtitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (showSwatches) PaletteSwatch()
            }
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        // The row is clickable as well: tapping the title toggles, the switch consumes its own tap.
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

/** Three role dots: the scheme itself changes with the switch, so there is no second palette. */
@Composable
private fun PaletteSwatch() {
    val animations = animationsEnabled()
    val spec: FiniteAnimationSpec<Color> = if (animations) Motion.glide() else tween(durationMillis = 0)
    val primary by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = spec,
        label = "swatch-primary"
    )
    val secondary by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.secondary,
        animationSpec = spec,
        label = "swatch-secondary"
    )
    val tertiary by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.tertiary,
        animationSpec = spec,
        label = "swatch-tertiary"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(primary, secondary, tertiary).forEach { color ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Mutually exclusive update states; drives the marker and the trailing action of [UpdateRow]. */
private enum class UpdateStatus { Idle, Checking, UpToDate, Available, Error }

private fun UpdateUiState.status(): UpdateStatus = when {
    checking -> UpdateStatus.Checking
    update != null -> UpdateStatus.Available
    upToDate -> UpdateStatus.UpToDate
    error != null -> UpdateStatus.Error
    else -> UpdateStatus.Idle
}

/**
 * One row, one line: title, current version, result marker, and the action on the right where the
 * other rows put their chevron. There is no supporting line and nothing is appended below, so the
 * version text can never be squeezed by the action and checking never pushes the card under it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateRow(updateState: UpdateUiState, onCheckUpdate: () -> Unit, onOpenRelease: (String) -> Unit) {
    val status = updateState.status()
    val update = updateState.update
    val checkingLabel = stringResource(R.string.update_checking)
    val animations = animationsEnabled()
    var previousStatus by remember { mutableStateOf(status) }
    var pulseKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(status) {
        // Only a completed check earns light; an error keeps its plain red icon.
        if (animations && previousStatus == UpdateStatus.Checking &&
            (status == UpdateStatus.UpToDate || status == UpdateStatus.Available)
        ) {
            pulseKey++
        }
        previousStatus = status
    }

    ListItem(
        leadingContent = {
            // Brand anchor: the update row is the one place the version lives now, so it carries
            // the brand hue instead of the neutral row icon tint.
            Icon(
                imageVector = Icons.Filled.SystemUpdate,
                contentDescription = null,
                tint = brandAccent()
            )
        },
        headlineContent = { Text(stringResource(R.string.update_title)) },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Text(
                    text = stringResource(R.string.update_current, BuildConfig.VERSION_NAME),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                when (status) {
                    UpdateStatus.UpToDate -> ResultMarker(
                        icon = Icons.Filled.CheckCircle,
                        description = stringResource(R.string.update_up_to_date),
                        tint = MaterialTheme.colorScheme.primary,
                        pulseKey = pulseKey,
                        animations = animations
                    )

                    UpdateStatus.Available -> {
                        ResultMarker(
                            icon = Icons.Filled.ArrowCircleUp,
                            description = stringResource(
                                R.string.update_available,
                                update?.version.orEmpty()
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                            pulseKey = pulseKey,
                            animations = animations
                        )
                        if (update != null) {
                            Text(
                                text = update.version,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    UpdateStatus.Error -> Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = stringResource(
                            R.string.update_error,
                            updateState.error.orEmpty()
                        ),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )

                    UpdateStatus.Idle, UpdateStatus.Checking -> Unit
                }
            }
        },
        trailingContent = {
            when (status) {
                UpdateStatus.Checking -> ContainedLoadingIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .semantics { contentDescription = checkingLabel }
                )

                UpdateStatus.Available -> if (update != null) {
                    TextButton(
                        onClick = { onOpenRelease(update.htmlUrl) },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(stringResource(R.string.update_action_open))
                    }
                }

                UpdateStatus.Error -> TextButton(
                    onClick = onCheckUpdate,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(stringResource(R.string.update_action_retry))
                }

                UpdateStatus.Idle, UpdateStatus.UpToDate -> TextButton(
                    onClick = onCheckUpdate,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(stringResource(R.string.update_action_check))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

/** Holds the result icon in a fixed frame so the halo has room without pushing the text. */
@Composable
private fun ResultMarker(
    icon: ImageVector,
    description: String,
    tint: Color,
    pulseKey: Int,
    animations: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(22.dp),
        contentAlignment = Alignment.Center
    ) {
        if (animations && pulseKey > 0) SuccessHalo(pulseKey)
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
    }
}

/** One converging ring, then a static rest frame. No blur, no glow over the version text. */
@Composable
private fun SuccessHalo(pulseKey: Int) {
    val convergence = remember { Animatable(0f) }
    val opacity = remember { Animatable(0f) }
    LaunchedEffect(pulseKey) {
        convergence.snapTo(0f)
        opacity.snapTo(1f)
        convergence.animateTo(1f, animationSpec = Motion.pulse())
        delay(Motion.PULSE_DECAY_MS)
        opacity.animateTo(0f, animationSpec = Motion.pulse())
    }
    val halo = Signal.halo()
    Canvas(modifier = Modifier.fillMaxSize()) {
        val alpha = opacity.value
        if (alpha <= 0f) return@Canvas
        val outer = size.minDimension / 2f - 1.dp.toPx()
        val radius = outer * (1f - 0.35f * convergence.value)
        drawCircle(
            color = halo.copy(alpha = halo.alpha * alpha),
            radius = radius,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = halo.copy(alpha = halo.alpha * alpha * 0.45f),
            radius = radius * 0.72f,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp)
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(labelFor(option))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

private const val HOMEPAGE_URL = "https://github.com/gythialy/SportEditor"
private const val LICENSE_URL = "https://github.com/gythialy/SportEditor/blob/main/LICENSE"
