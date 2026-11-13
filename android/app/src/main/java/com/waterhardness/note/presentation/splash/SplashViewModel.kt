package com.waterhardness.note.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waterhardness.note.core.config.GameConfig
import com.waterhardness.note.domain.repository.CareRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel(repository: CareRepository) : ViewModel() {
    private val _state = MutableStateFlow(SplashUiState())
    val state = _state.asStateFlow()
    private val _finished = MutableStateFlow(false)
    val finished = _finished.asStateFlow()
    private val completion = Channel<Unit>(Channel.BUFFERED)
    val events = completion.receiveAsFlow()
    init {
        viewModelScope.launch {
            repository.load()
            _state.update { it.copy(dataReady = !repository.state.value.error, startupError = repository.state.value.error) }
        }
        viewModelScope.launch {
            delay(GameConfig.LOADER_DURATION_MS)
            _finished.value = true
            _state.update { it.copy(finished = true) }
            completion.send(Unit)
        }
    }
}
