package io.github.gythialy.sporteditor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.gythialy.sporteditor.BuildConfig
import io.github.gythialy.sporteditor.data.AppTarget
import io.github.gythialy.sporteditor.data.SettingsRepository
import io.github.gythialy.sporteditor.data.StepMode
import io.github.gythialy.sporteditor.data.UpdateChecker
import io.github.gythialy.sporteditor.data.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateUiState(
    val checking: Boolean = false,
    val upToDate: Boolean = false,
    val update: UpdateInfo? = null,
    val error: String? = null
)

class MainViewModel(private val repository: SettingsRepository) : ViewModel() {

    val state = repository.state

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    fun checkForUpdates() {
        if (_updateState.value.checking) return
        _updateState.value = UpdateUiState(checking = true)
        viewModelScope.launch {
            runCatching { UpdateChecker.checkLatest(BuildConfig.VERSION_NAME) }
                .onSuccess { update ->
                    _updateState.value = if (update == null) {
                        UpdateUiState(upToDate = true)
                    } else {
                        UpdateUiState(update = update)
                    }
                }
                .onFailure { error ->
                    _updateState.value = UpdateUiState(
                        error = error.message ?: error.javaClass.simpleName
                    )
                }
        }
    }

    fun setEnabled(target: AppTarget, enabled: Boolean) = repository.update(target) { it.copy(enabled = enabled) }

    fun setMode(target: AppTarget, mode: StepMode) = repository.update(target) { it.copy(mode = mode) }

    fun setValue(target: AppTarget, value: Int) = repository.update(target) { it.copy(value = value) }

    fun setMaxSteps(target: AppTarget, maxSteps: Int) = repository.update(target) { it.copy(maxSteps = maxSteps) }

    fun resetToDefaults() = repository.resetToDefaults()

    companion object {
        fun factory(repository: SettingsRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(repository) }
        }
    }
}
