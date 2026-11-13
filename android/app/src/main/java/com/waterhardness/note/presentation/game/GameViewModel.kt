package com.waterhardness.note.presentation.game

import androidx.lifecycle.viewModelScope
import com.waterhardness.note.core.navigation.Screen
import com.waterhardness.note.domain.model.*
import com.waterhardness.note.domain.repository.CareRepository
import com.waterhardness.note.domain.usecase.HardnessCalculator
import com.waterhardness.note.presentation.common.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class GameViewModel(repository: CareRepository) : CareViewModel(repository) {
    private val _state = MutableStateFlow(GameUiState())
    override val state = _state.asStateFlow()
    init {
        viewModelScope.launch { repository.state.collect { store ->
            _state.update { old ->
                val chart = store.data.activeChart
                val changed = old.chart?.id != chart?.id && old.savedRecordId == null
                val unit = store.data.settings.unit
                old.copy(appliance = store.data.appliances.find { it.id == old.appliance?.id } ?: old.appliance, chart = if (old.savedRecordId == null) chart else old.chart, unit = if (old.savedRecordId == null) unit else old.unit,
                    input = if (unit != old.unit && old.canonical != null && old.savedRecordId == null) HardnessCalculator.format(old.canonical, unit) else old.input,
                    swatch = if (changed) null else old.swatch, canonical = if (changed && old.source == EntrySource.CHART) null else old.canonical, source = if (changed && old.source == EntrySource.CHART) null else old.source)
            }
        } }
    }
    fun enter(route: Screen.Game) {
        if (_state.value.draftId != route.draftId) {
            val data = repository.state.value.data
            _state.value = GameUiState(draftId = route.draftId, appliance = data.activeAppliances.find { it.id == route.applianceId }, unit = data.settings.unit, chart = data.activeChart)
        }
        activate()
    }
    override fun dateChanged(today: LocalDate) { _state.update { it.copy(today = today) } }
    private fun editable() = !_state.value.isSaving && _state.value.savedRecordId == null
    fun select(index: Int) {
        if (!editable()) return
        val value = (_state.value.chart?.values ?: listOf(0, 500, 1000, 1500, 2500, 4000)).getOrNull(index) ?: return
        _state.update { it.copy(swatch = index, source = EntrySource.CHART, canonical = value, input = HardnessCalculator.format(value, it.unit), originalInput = HardnessCalculator.format(value, WaterUnit.MG_L), originalUnit = WaterUnit.MG_L, dirty = true, inputError = false) }
    }
    fun printed() { if (editable()) _state.update { it.copy(source = EntrySource.PRINTED, swatch = null, canonical = null, dirty = true, numericEditor = false) } }
    fun input(value: String) {
        if (!editable()) return
        val canonical = HardnessCalculator.parse(value, _state.value.unit)
        _state.update { it.copy(input = value, canonical = canonical, originalInput = value, originalUnit = it.unit, source = EntrySource.PRINTED, swatch = null, inputError = canonical == null, dirty = true) }
    }
    fun step(direction: Int) { HardnessCalculator.step(_state.value.input, _state.value.unit, direction)?.let(::input) }
    fun numericEditor(show: Boolean) { _state.update { it.copy(numericEditor = show) } }
    fun note(value: String) { if (editable()) _state.update { it.copy(note = value.take(160), dirty = true) } }
    fun date(value: LocalDate) { if (editable()) _state.update { it.copy(date = value, dirty = true) } }
    fun notSure() {
        val s = _state.value
        if (!s.isSaving) emit(UiEvent.Navigate(Screen.GameOver(s.draftId, s.appliance?.id ?: "")))
    }
    fun replay() = emit(UiEvent.Replay)
    override fun back() {
        val s = _state.value
        if (s.isSaving) return
        if (s.dirty && s.savedRecordId == null) _state.update { it.copy(discard = true) } else super.back()
    }
    fun keep() { _state.update { it.copy(discard = false) } }
    fun discard() { _state.update { it.copy(discard = false, dirty = false) }; super.back() }
    fun save() {
        val s = _state.value
        if (!s.canSave) return
        val appliance = s.appliance ?: return
        val canonical = s.canonical ?: return
        _state.update { it.copy(isSaving = true, writeError = false) }
        viewModelScope.launch {
            val record = WaterTest(draftId = s.draftId, applianceId = appliance.id, name = appliance.name, date = s.date, canonical = canonical, originalInput = s.originalInput, unit = s.originalUnit, category = HardnessCalculator.category(canonical), source = s.source ?: EntrySource.PRINTED, chart = if (s.source == EntrySource.CHART) s.chart else null, note = s.note)
            runCatching { repository.update { data ->
                require(data.activeAppliances.any { it.id == appliance.id })
                if (data.tests.any { it.draftId == s.draftId }) data else data.copy(tests = data.tests + record)
            } }.onSuccess { data ->
                val saved = data.tests.first { it.draftId == s.draftId }
                if (_state.value.draftId == s.draftId) _state.update { it.copy(isSaving = false, dirty = false, savedRecordId = saved.id) }
                if (active && _state.value.draftId == s.draftId) {
                    if (data.settings.haptics) emit(UiEvent.Haptic)
                    emit(UiEvent.Navigate(Screen.GameOver(s.draftId, appliance.id, saved.id)))
                }
            }.onFailure { if (_state.value.draftId == s.draftId) _state.update { it.copy(isSaving = false, writeError = true) } }
        }
    }
    fun openChart(label: String) { if (editable()) _state.update { it.copy(chartEditor = ChartEditing.open(it.chart, label)) } }
    fun closeChart() { if (_state.value.chartEditor?.saving != true) _state.update { it.copy(chartEditor = null) } }
    fun chartValue(row: Int, value: String) { _state.update { it.copy(chartEditor = it.chartEditor?.let { e -> e.copy(values = e.values.mapIndexed { i, v -> if (i == row) value.take(16) else v }, errorRow = null) }) } }
    fun chartStep(row: Int, direction: Int) { _state.value.chartEditor?.values?.getOrNull(row)?.let { HardnessCalculator.step(it, WaterUnit.MG_L, direction)?.let { next -> chartValue(row, next) } } }
    fun chartSwatch(row: Int) { _state.update { it.copy(chartEditor = it.chartEditor?.let { e -> e.copy(swatches = e.swatches.mapIndexed { i, v -> if (i == row) (v + 1) % 6 else v }) }) } }
    fun chartConfirmed(value: Boolean) { _state.update { it.copy(chartEditor = it.chartEditor?.copy(confirmed = value)) } }
    fun saveChart() {
        val e = _state.value.chartEditor ?: return
        if (e.saving || !e.confirmed) return
        val conflict = ChartEditing.conflict(e)
        if (conflict != null) { _state.update { it.copy(chartEditor = e.copy(errorRow = conflict)) }; return }
        _state.update { it.copy(chartEditor = e.copy(saving = true)) }
        viewModelScope.launch {
            val profile = ChartEditing.profile(e)
            runCatching { repository.update { it.copy(charts = it.charts + profile, settings = it.settings.copy(chartId = profile.id)) } }
                .onSuccess { _state.update { it.copy(chartEditor = null, swatch = null, canonical = null, source = null) } }
                .onFailure { _state.update { it.copy(chartEditor = e.copy(storageError = true), writeError = true) } }
        }
    }
}
