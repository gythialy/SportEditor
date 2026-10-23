package io.github.gythialy.sporteditor.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gythialy.sporteditor.SportEditorApp
import io.github.gythialy.sporteditor.ui.theme.SportEditorTheme
import io.github.gythialy.sporteditor.ui.theme.resolveDarkTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as SportEditorApp).settingsRepository)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SportEditorApp
        val language = AppLanguageStore.load(this)
        val openUrl: (String) -> Unit = { url ->
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
        setContent {
            var themeModeKey by rememberSaveable { mutableStateOf(ThemeModeStore.load(this).storageKey) }
            val themeMode = ThemeMode.fromKey(themeModeKey)

            // A forced theme overrides the system light/dark state, so the system bar icons have
            // to follow the app theme instead of the device theme.
            val darkTheme = themeMode.resolveDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            SportEditorTheme(themeMode = themeMode) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val updateState by viewModel.updateState.collectAsStateWithLifecycle()
                var screen by rememberSaveable { mutableStateOf("main") }

                BackHandler(enabled = screen != "main") {
                    screen = if (screen == "diagnostics") "settings" else "main"
                }

                when (screen) {
                    "settings" -> SettingsScreen(
                        themeMode = themeMode,
                        onThemeModeChange = { selected ->
                            ThemeModeStore.save(this, selected)
                            themeModeKey = selected.storageKey
                        },
                        language = language,
                        onLanguageChange = { selected ->
                            if (selected != language) {
                                AppLanguageStore.save(this, selected)
                                recreate()
                            }
                        },
                        updateState = updateState,
                        onCheckUpdate = viewModel::checkForUpdates,
                        onOpenRelease = openUrl,
                        onOpenDiagnostics = { screen = "diagnostics" },
                        onOpenUrl = openUrl,
                        onBack = { screen = "main" }
                    )

                    "diagnostics" -> DiagnosticsScreen(
                        state = state,
                        store = app.debugLogStore,
                        onBack = { screen = "settings" }
                    )

                    else -> HomeScreen(
                        state = state,
                        onEnabledChange = viewModel::setEnabled,
                        onModeChange = viewModel::setMode,
                        onValueChange = viewModel::setValue,
                        onMaxStepsChange = viewModel::setMaxSteps,
                        onResetTarget = viewModel::resetTarget,
                        onOpenSettings = { screen = "settings" }
                    )
                }
            }
        }
    }
}
