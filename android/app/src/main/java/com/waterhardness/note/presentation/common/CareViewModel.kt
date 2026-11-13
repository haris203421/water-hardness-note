package com.waterhardness.note.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waterhardness.note.core.navigation.Screen
import com.waterhardness.note.domain.model.*
import com.waterhardness.note.domain.repository.CareRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

sealed interface UiEvent {
    data class Navigate(val screen: Screen) : UiEvent
    data class StartTest(val applianceId: String) : UiEvent
    data object Back : UiEvent
    data object Menu : UiEvent
    data object Replay : UiEvent
    data object Haptic : UiEvent
    data object Permission : UiEvent
    data object NotificationSettings : UiEvent
}

abstract class CareViewModel(protected val repository: CareRepository) : ViewModel() {
    abstract val state: StateFlow<*>
    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()
    protected var active = false
    private var ownerToken: Any? = null
    fun claim(token: Any) { ownerToken = token }
    fun resume(token: Any) { if (ownerToken === token) activate() }
    fun release(token: Any) { if (ownerToken === token) pause() }
    private var dateJob: Job? = null
    protected fun emit(event: UiEvent) { viewModelScope.launch { eventChannel.send(event) } }
    fun navigate(screen: Screen) = emit(UiEvent.Navigate(screen))
    open fun back() = emit(UiEvent.Back)
    fun menu() = emit(UiEvent.Menu)
    fun retry() { viewModelScope.launch { repository.load() } }
    open fun activate() {
        active = true
        dateChanged(LocalDate.now())
        dateJob?.cancel()
        dateJob = viewModelScope.launch {
            while (isActive) {
                val now = ZonedDateTime.now()
                delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).toMillis().coerceIn(1000, 60000))
                dateChanged(LocalDate.now())
            }
        }
    }
    protected open fun dateChanged(today: LocalDate) = Unit
    open fun pause() { active = false; dateJob?.cancel(); dateJob = null }
}

data class ApplianceEditor(val id: String? = null, val name: String = "", val type: ApplianceType = ApplianceType.KETTLE, val launchTest: Boolean = false, val error: Boolean = false, val saving: Boolean = false, val confirmArchive: Boolean = false, val storageError: Boolean = false)
data class ChartEditor(val label: String, val values: List<String>, val swatches: List<Int>, val confirmed: Boolean = false, val errorRow: Int? = null, val saving: Boolean = false, val storageError: Boolean = false) {
    fun canStep(row: Int, direction: Int): Boolean = com.waterhardness.note.domain.usecase.HardnessCalculator.step(values[row], WaterUnit.MG_L, direction) != null
}
