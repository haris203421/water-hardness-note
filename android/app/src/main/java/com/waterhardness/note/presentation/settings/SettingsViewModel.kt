package com.waterhardness.note.presentation.settings

import androidx.lifecycle.viewModelScope
import com.waterhardness.note.domain.model.*
import com.waterhardness.note.domain.repository.*
import com.waterhardness.note.presentation.common.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(repository: CareRepository, private val reminders: ReminderGateway) : CareViewModel(repository) {
    private val _state = MutableStateFlow(SettingsUiState())
    override val state = _state.asStateFlow()
    private var retryAction: (() -> Unit)? = null
    init { viewModelScope.launch { repository.state.collect { store -> _state.update { it.copy(store = store) } } } }
    override fun activate() { super.activate(); permissionChanged() }
    fun permissionChanged() { _state.update { it.copy(permission = reminders.permissionAvailable()) } }
    private fun settings(change: (CareSettings) -> CareSettings) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true, error = false) }
        retryAction = { settings(change) }
        viewModelScope.launch {
            runCatching { repository.update { it.copy(settings = change(it.settings)) } }
                .onSuccess { _state.update { it.copy(saving = false) }; retryAction = null }
                .onFailure { _state.update { it.copy(saving = false, error = true) } }
        }
    }
    fun retryWrite() { retryAction?.invoke() ?: retry() }
    fun unit(value: WaterUnit) = settings { it.copy(unit = value) }
    fun haptics(value: Boolean) = settings { it.copy(haptics = value) }
    fun examples(value: Boolean) = settings { it.copy(examples = value) }
    fun reminders(value: Boolean) {
        if (value && !reminders.permissionAvailable()) emit(UiEvent.Permission)
        else settings { it.copy(reminders = value) }
    }
    fun permissionResult() { permissionChanged(); settings { it.copy(reminders = reminders.permissionAvailable()) } }
    fun openSettings() = emit(UiEvent.NotificationSettings)
    fun eraseConfirmation(show: Boolean) { if (!_state.value.saving) _state.update { it.copy(eraseConfirmation = show) } }
    fun erase() {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true, error = false) }
        retryAction = ::erase
        viewModelScope.launch {
            runCatching { repository.erase() }.onSuccess {
                _state.update { it.copy(saving = false, eraseConfirmation = false) }
                retryAction = null
                if (active) menu()
            }.onFailure { _state.update { it.copy(saving = false, error = true) } }
        }
    }
    fun openChart(label: String) { _state.update { it.copy(chartEditor = ChartEditing.open(it.store.data.activeChart, label)) } }
    fun closeChart() { if (_state.value.chartEditor?.saving != true) _state.update { it.copy(chartEditor = null) } }
    fun chartValue(row: Int, value: String) { _state.update { it.copy(chartEditor = it.chartEditor?.let { e -> e.copy(values = e.values.mapIndexed { i, v -> if (i == row) value.take(16) else v }, errorRow = null) }) } }
    fun chartStep(row: Int, direction: Int) { _state.value.chartEditor?.values?.getOrNull(row)?.let { com.waterhardness.note.domain.usecase.HardnessCalculator.step(it, WaterUnit.MG_L, direction)?.let { next -> chartValue(row, next) } } }
    fun chartSwatch(row: Int) { _state.update { it.copy(chartEditor = it.chartEditor?.let { e -> e.copy(swatches = e.swatches.mapIndexed { i, v -> if (i == row) (v + 1) % 6 else v }) }) } }
    fun chartConfirmed(value: Boolean) { _state.update { it.copy(chartEditor = it.chartEditor?.copy(confirmed = value)) } }
    fun saveChart() {
        val editor = _state.value.chartEditor ?: return
        if (!editor.confirmed || editor.saving) return
        val conflict = ChartEditing.conflict(editor)
        if (conflict != null) { _state.update { it.copy(chartEditor = editor.copy(errorRow = conflict)) }; return }
        _state.update { it.copy(chartEditor = editor.copy(saving = true)) }
        retryAction = ::saveChart
        viewModelScope.launch {
            val profile = ChartEditing.profile(editor)
            runCatching { repository.update { it.copy(charts = it.charts + profile, settings = it.settings.copy(chartId = profile.id)) } }
                .onSuccess { _state.update { it.copy(chartEditor = null) }; retryAction = null }
                .onFailure { _state.update { it.copy(chartEditor = editor.copy(storageError = true), error = true) } }
        }
    }
}
