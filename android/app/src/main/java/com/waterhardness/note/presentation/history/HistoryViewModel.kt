package com.waterhardness.note.presentation.history

import androidx.lifecycle.viewModelScope
import com.waterhardness.note.core.navigation.Screen
import com.waterhardness.note.domain.repository.CareRepository
import com.waterhardness.note.domain.usecase.PlanCalculator
import com.waterhardness.note.presentation.common.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(repository: CareRepository) : CareViewModel(repository) {
    private val _state = MutableStateFlow(HistoryUiState())
    override val state = _state.asStateFlow()
    private var route: Screen.History? = null
    init { viewModelScope.launch { repository.state.collect { refresh() } } }
    fun enter(value: Screen.History) { if (route != value) { route = value; _state.update { it.copy(applianceId = value.applianceId, filter = HistoryFilter.ALL) } }; refresh(); activate() }
    private fun refresh() {
        val store = repository.state.value
        val data = store.data
        val archived = data.appliances.filter { it.archived }.map { it.id }.toSet()
        val all = data.tests.map { HistoryEntry(it.id, it.applianceId, it.name, it.date, it.createdAt, test = it, archived = it.applianceId in archived) } + data.cleanings.map { HistoryEntry(it.id, it.applianceId, it.name, it.date, it.createdAt, cleaning = it, archived = it.applianceId in archived) }
        _state.update { old ->
            val selected = all.filter { old.applianceId == null || it.applianceId == old.applianceId }
            old.copy(store = store, testCount = selected.count { it.test != null }, cleaningCount = selected.count { it.cleaning != null }, entries = selected.filter { when (old.filter) { HistoryFilter.ALL -> true; HistoryFilter.TESTS -> it.test != null; HistoryFilter.DESCALING -> it.cleaning != null } }.sortedWith(compareByDescending<HistoryEntry> { it.date }.thenByDescending { it.createdAt }.thenByDescending { it.id }))
        }
    }
    fun filter(value: HistoryFilter) { _state.update { it.copy(filter = value) }; refresh() }
    fun appliance(id: String?) { _state.update { it.copy(applianceId = id) }; refresh() }
    fun clear() { _state.update { it.copy(applianceId = null, filter = HistoryFilter.ALL) }; refresh() }
    fun detail(value: HistoryEntry?) { if (!_state.value.saving) _state.update { it.copy(detail = value, confirmDelete = false) } }
    fun confirm(value: Boolean) { _state.update { it.copy(confirmDelete = value) } }
    fun delete() {
        val entry = _state.value.detail ?: return
        if (_state.value.saving) return
        _state.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            runCatching { repository.update { data ->
                if (entry.test != null) data.copy(tests = data.tests.filterNot { it.id == entry.id })
                else PlanCalculator.recompute(data.copy(cleanings = data.cleanings.filterNot { it.id == entry.id }), entry.applianceId)
            } }.onSuccess { _state.update { it.copy(saving = false, detail = null, confirmDelete = false) } }
                .onFailure { _state.update { it.copy(saving = false, error = true, confirmDelete = false) } }
        }
    }
    fun record() {
        val activeAppliances = _state.value.store.data.activeAppliances
        val id = activeAppliances.find { it.id == _state.value.applianceId }?.id ?: activeAppliances.firstOrNull()?.id
        if (id != null) emit(UiEvent.StartTest(id)) else emit(UiEvent.Menu)
    }
}
