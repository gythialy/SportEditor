package io.github.sporteditor.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sporteditor.SportEditorApp
import io.github.sporteditor.ui.theme.Motion
import io.github.sporteditor.ui.theme.SportEditorTheme
import io.github.sporteditor.ui.theme.animationsEnabled
import io.github.sporteditor.ui.theme.resolveDarkTheme

private const val THEME_DIP_OUT_MS = 110
private const val THEME_DIP_IN_MS = 190

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
            // Captured before the animated content scope takes over `this`, which is what the
            // settings callbacks below still need in order to persist a preference.
            val activity = this@MainActivity
            val animations = animationsEnabled()

            // The applied theme lags the requested one by one dip: content fades out, the scheme
            // swaps at the seam, content fades back in. A Crossfade over the theme would keep two
            // copies of the tree alive and reset non-saveable state (the diagnostics pager, open
            // menus); dipping keeps a single tree, so the current screen and scroll survive.
            var appliedKey by rememberSaveable { mutableStateOf(ThemeModeStore.load(activity).storageKey) }
            var pendingKey by rememberSaveable { mutableStateOf<String?>(null) }

            val dip by animateFloatAsState(
                targetValue = if (pendingKey != null) 0f else 1f,
                animationSpec = tween(
                    durationMillis = if (pendingKey != null) THEME_DIP_OUT_MS else THEME_DIP_IN_MS,
                    easing = FastOutSlowInEasing
                ),
                label = "theme-dip"
            )
            LaunchedEffect(dip, pendingKey) {
                val next = pendingKey ?: return@LaunchedEffect
                if (dip > 0.02f) return@LaunchedEffect
                ThemeModeStore.save(activity, ThemeMode.fromKey(next))
                appliedKey = next
                pendingKey = null
            }

            val themeMode = ThemeMode.fromKey(appliedKey)
            // What the settings row reports while a dip is in flight, so the user's choice shows up
            // immediately instead of after the fade.
            val requestedTheme = ThemeMode.fromKey(pendingKey ?: appliedKey)

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
                // The page colour switches instantly at the seam while the content is invisible:
                // animating it as well would overlap two transitions and flash the old surface
                // between them. The dip already hides the moment the scheme flips.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = dip }
                    ) {
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        val updateState by viewModel.updateState.collectAsStateWithLifecycle()
                        var screen by rememberSaveable { mutableStateOf("main") }

                        BackHandler(enabled = screen != "main") {
                            screen = if (screen == "diagnostics") "settings" else "main"
                        }

                        // Fade-through between the three screens. The specs are read out here because
                        // transitionSpec is not a composable scope. Deliberately a plain cross-fade rather
                        // than a scaled "fade through": these are three different tools, not one surface
                        // zooming, and the app's language is restraint. Exits run faster than entrances.
                        val enterSpec = Motion.enter<Float>()
                        val leaveSpec = Motion.leave<Float>()

                        AnimatedContent(
                            targetState = screen,
                            transitionSpec = {
                                fadeIn(animationSpec = enterSpec).togetherWith(fadeOut(animationSpec = leaveSpec))
                            },
                            label = "screen"
                        ) { current ->
                            when (current) {
                                "settings" -> SettingsScreen(
                                    themeMode = requestedTheme,
                                    onThemeModeChange = { selected ->
                                        if (animations) {
                                            pendingKey = selected.storageKey
                                        } else {
                                            ThemeModeStore.save(activity, selected)
                                            appliedKey = selected.storageKey
                                        }
                                    },
                                    language = language,
                                    onLanguageChange = { selected ->
                                        if (selected != language) {
                                            AppLanguageStore.save(activity, selected)
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
                                    captureLevel = state.captureLevel,
                                    onCaptureLevelChange = viewModel::setCaptureLevel,
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
        }
    }
}
