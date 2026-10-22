package io.github.gythialy.sporteditor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.gythialy.sporteditor.data.AppTarget
import io.github.gythialy.sporteditor.data.SettingsRepository
import io.github.gythialy.sporteditor.data.StepMode

class MainViewModel(private val repository: SettingsRepository) : ViewModel() {

    val state = repository.state

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
