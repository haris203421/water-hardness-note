package com.waterhardness.note.presentation.hardness

import androidx.lifecycle.viewModelScope
import com.waterhardness.note.core.navigation.Screen
import com.waterhardness.note.domain.model.Hardness
import com.waterhardness.note.domain.repository.CareRepository
import com.waterhardness.note.presentation.common.CareViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HardnessInfoViewModel(repository: CareRepository) : CareViewModel(repository) {
    private val _state = MutableStateFlow(HardnessInfoUiState())
    override val state = _state.asStateFlow()
    init { viewModelScope.launch { repository.state.collect { store -> _state.update { it.copy(unit = store.data.settings.unit) } } } }
    fun enter(route: Screen.HardnessInfo) { _state.update { it.copy(canonical = route.canonical, fromTest = route.fromTest) }; activate() }
    fun expand(category: Hardness) { _state.update { it.copy(expanded = if (it.expanded == category) null else category) } }
}
