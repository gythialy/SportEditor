package io.github.gythialy.sporteditor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gythialy.sporteditor.BuildConfig
import io.github.gythialy.sporteditor.R
import io.github.gythialy.sporteditor.ui.theme.cardContainerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
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
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionLabel(R.string.section_general) }
            item {
                SettingsGroup {
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
                    RowDivider()
                    UpdateRow(
                        updateState = updateState,
                        onCheckUpdate = onCheckUpdate,
                        onOpenRelease = onOpenRelease
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Filled.BugReport,
                        title = stringResource(R.string.debug_title),
                        subtitle = stringResource(R.string.debug_desc),
                        onClick = onOpenDiagnostics
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
                SettingsGroup {
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
                    RowDivider()
                    ListItem(
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.settings_version)) },
                        supportingContent = {
                            Text(
                                "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
        modifier = modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor()
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
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
                tint = MaterialTheme.colorScheme.primary
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

@Composable
private fun UpdateRow(updateState: UpdateUiState, onCheckUpdate: () -> Unit, onOpenRelease: (String) -> Unit) {
    Column {
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            headlineContent = { Text(stringResource(R.string.update_title)) },
            supportingContent = {
                Text(stringResource(R.string.update_current, BuildConfig.VERSION_NAME))
            },
            trailingContent = {
                TextButton(
                    onClick = onCheckUpdate,
                    enabled = !updateState.checking
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(
                            if (updateState.checking) R.string.update_checking else R.string.update_check
                        )
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        val update = updateState.update
        when {
            update != null -> {
                Column(
                    modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.update_available, update.version),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(
                        onClick = { onOpenRelease(update.htmlUrl) },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.update_open))
                    }
                }
            }

            updateState.upToDate -> Text(
                text = stringResource(R.string.update_up_to_date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 12.dp)
            )

            updateState.error != null -> Text(
                text = stringResource(R.string.update_error, updateState.error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 56.dp, end = 16.dp, bottom = 12.dp)
            )
        }
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
                            .padding(vertical = 8.dp),
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
