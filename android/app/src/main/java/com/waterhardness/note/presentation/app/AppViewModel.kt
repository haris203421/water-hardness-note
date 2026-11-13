package com.waterhardness.note.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waterhardness.note.core.navigation.Screen
import com.waterhardness.note.domain.repository.CareRepository
import com.waterhardness.note.presentation.common.UiEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AppUiState(val pendingPlan: String? = null, val inactivePlan: Boolean = false)

class AppViewModel(private val repository: CareRepository) : ViewModel() {
    private val _screen = MutableStateFlow<Screen>(Screen.Splash)
    val screen = _screen.asStateFlow()
    private val _state = MutableStateFlow(AppUiState())
    val state = _state.asStateFlow()
    private val backStack = mutableListOf<Screen>()
    private var navigationVersion = 0L
    fun foreground() { if (_screen.value != Screen.Splash) viewModelScope.launch { repository.load() } }
    fun finishSplash() {
        if (_screen.value != Screen.Splash) return
        navigationVersion++
        _screen.value = Screen.Menu
        val pending = _state.value.pendingPlan
        if (pending != null) openPlanIntent(pending)
    }
    fun navigate(target: Screen) {
        if (target == _screen.value) return
        navigationVersion++
        if (_screen.value != Screen.Splash) backStack.add(_screen.value)
        _screen.value = target
    }
    fun back() {
        navigationVersion++
        if (backStack.isNotEmpty()) _screen.value = backStack.removeAt(backStack.lastIndex)
        else home()
    }
    fun home() { navigationVersion++; backStack.clear(); _screen.value = Screen.Menu }
    fun handle(event: UiEvent, origin: Screen) {
        if (_screen.value != origin) return
        when (event) {
            is UiEvent.Navigate -> navigate(event.screen)
            is UiEvent.StartTest -> navigate(Screen.Game(UUID.randomUUID().toString(), event.applianceId))
            UiEvent.Back -> back()
            UiEvent.Menu -> home()
            UiEvent.Replay -> replay()
            else -> Unit
        }
    }
    private fun replay() {
        navigationVersion++
        val current = _screen.value
        if (current is Screen.GameOver && current.recordId == null) { back(); return }
        val applianceId = when (current) { is Screen.GameOver -> current.applianceId; is Screen.Game -> current.applianceId; else -> return }
        while (backStack.lastOrNull() is Screen.Game || backStack.lastOrNull() is Screen.GameOver) backStack.removeAt(backStack.lastIndex)
        _screen.value = Screen.Game(UUID.randomUUID().toString(), applianceId)
    }
    fun openPlanIntent(applianceId: String) {
        if (_screen.value == Screen.Splash) { _state.update { it.copy(pendingPlan = applianceId) }; return }
        val version = navigationVersion
        _state.update { it.copy(pendingPlan = null) }
        viewModelScope.launch {
            if (repository.state.value.loading) repository.load()
            if (navigationVersion != version) return@launch
            val valid = repository.state.value.data.activeAppliances.any { it.id == applianceId }
            home()
            _state.update { it.copy(pendingPlan = null, inactivePlan = !valid) }
            if (valid) navigate(Screen.DescalePlan(applianceId))
        }
    }
}
