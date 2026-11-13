package com.waterhardness.note.presentation.gameover

import androidx.lifecycle.viewModelScope
import com.waterhardness.note.core.navigation.Screen
import com.waterhardness.note.domain.repository.CareRepository
import com.waterhardness.note.presentation.common.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameOverViewModel(repository: CareRepository) : CareViewModel(repository) {
    private val _state = MutableStateFlow(GameOverUiState())
    override val state = _state.asStateFlow()
    private var route: Screen.GameOver? = null
    init { viewModelScope.launch { repository.state.collect { refresh() } } }
    fun enter(value: Screen.GameOver) { route = value; refresh(); activate() }
    override fun dateChanged(today: java.time.LocalDate) { _state.update { it.copy(today = today) } }
    private fun refresh() {
        val r = route ?: return
        val store = repository.state.value
        val record = store.data.tests.find { it.id == r.recordId }
        _state.update { it.copy(loading = store.loading, needsReview = r.recordId == null, record = record, appliance = store.data.appliances.find { it.id == r.applianceId }, plan = store.data.plans.find { it.applianceId == r.applianceId }, unavailable = r.recordId != null && (record == null || store.error), unit = store.data.settings.unit) }
    }
    fun replay() = emit(UiEvent.Replay)
}
