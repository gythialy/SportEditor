package io.github.gythialy.sporteditor.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gythialy.sporteditor.SportEditorApp
import io.github.gythialy.sporteditor.ui.theme.SportEditorTheme

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
        setContent {
            SportEditorTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                var showDiagnostics by rememberSaveable { mutableStateOf(false) }
                if (showDiagnostics) {
                    DiagnosticsScreen(
                        state = state,
                        store = app.debugLogStore,
                        onBack = { showDiagnostics = false }
                    )
                } else {
                    SettingsScreen(
                        state = state,
                        language = language,
                        onLanguageChange = { selected ->
                            if (selected != language) {
                                AppLanguageStore.save(this, selected)
                                recreate()
                            }
                        },
                        onEnabledChange = viewModel::setEnabled,
                        onModeChange = viewModel::setMode,
                        onValueChange = viewModel::setValue,
                        onMaxStepsChange = viewModel::setMaxSteps,
                        onReset = viewModel::resetToDefaults,
                        onOpenDiagnostics = { showDiagnostics = true }
                    )
                }
            }
        }
    }
}
