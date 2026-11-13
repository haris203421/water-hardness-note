package com.waterhardness.note.presentation.plan

import androidx.lifecycle.viewModelScope
import com.waterhardness.note.domain.model.*
import com.waterhardness.note.domain.repository.*
import com.waterhardness.note.domain.usecase.PlanCalculator
import com.waterhardness.note.presentation.common.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class DescalePlanViewModel(repository: CareRepository, private val reminders: ReminderGateway) : CareViewModel(repository) {
    private val _state = MutableStateFlow(DescalePlanUiState())
    override val state = _state.asStateFlow()
    private var applianceId = ""
    private var lastOperation = "save"
    fun retryLast() { if (!_state.value.error) retry() else when (lastOperation) { "clean" -> markDescaled(); "snooze" -> snooze(); else -> save() } }
    init { viewModelScope.launch { repository.state.collect { refresh(false) } } }
    fun enter(id: String) {
        val reset = applianceId != id
        applianceId = id
        if (reset) _state.value = DescalePlanUiState()
        refresh(reset)
        activate()
    }
    override fun activate() { super.activate(); _state.update { it.copy(permission = reminders.permissionAvailable()) } }
    override fun dateChanged(today: LocalDate) { _state.update { it.copy(today = today) } }
    private fun refresh(force: Boolean) {
        val store = repository.state.value
        val appliance = store.data.appliances.find { it.id == applianceId }
        val plan = store.data.plans.find { it.applianceId == applianceId }
        val latest = store.data.tests.filter { it.applianceId == applianceId }.maxWithOrNull(compareBy<WaterTest> { it.date }.thenBy { it.createdAt }.thenBy { it.id })
        _state.update { old ->
            val reset = force || !old.dirty && !old.saving
            old.copy(store = store, appliance = appliance, plan = plan, suggestion = appliance?.let { PlanCalculator.suggestion(it.type, latest?.category) },
                anchor = if (reset) plan?.anchorDate ?: LocalDate.now() else old.anchor,
                interval = if (reset) (plan?.interval ?: 30).toString() else old.interval,
                dueOverride = if (reset) plan?.dueOverride else old.dueOverride,
                reminder = if (reset) plan?.reminderEnabled ?: false else old.reminder)
        }
    }
    fun interval(value: String) { _state.update { it.copy(interval = value.take(3), dirty = true, saved = false) } }
    fun step(direction: Int) { interval(((_state.value.interval.toIntOrNull() ?: 30) + direction).coerceIn(1, 365).toString()) }
    fun suggestion() { _state.value.suggestion?.let { interval(it.toString()) } }
    fun anchor(date: LocalDate) { _state.update { it.copy(anchor = date, dirty = true, saved = false) } }
    fun due(date: LocalDate) { _state.update { it.copy(dueOverride = date, dirty = true, saved = false) } }
    fun clearDue() { _state.update { it.copy(dueOverride = null, dirty = true, saved = false) } }
    fun reminder(value: Boolean) { _state.update { it.copy(reminder = value, dirty = true, saved = false) } }
    override fun back() { if (_state.value.saving) return; if (_state.value.dirty) _state.update { it.copy(discard = true) } else super.back() }
    fun keep() { _state.update { it.copy(discard = false) } }
    fun discard() { _state.update { it.copy(dirty = false, discard = false) }; refresh(true); super.back() }
    fun save() {
        val s = _state.value
        val id = applianceId
        if (!s.valid || s.saving) return
        lastOperation = "save"
        _state.update { it.copy(saving = true, error = false, saved = false) }
        viewModelScope.launch {
            runCatching { repository.update { data ->
                require(data.activeAppliances.any { it.id == id })
                val existing = data.plans.find { it.applianceId == id }
                val latestCleaning = data.cleanings.filter { it.applianceId == id }.maxOfOrNull { it.date }
                val plan = DescalePlan(id, if (latestCleaning == null) AnchorType.PLAN_START else AnchorType.CLEANING, if (latestCleaning == null) s.anchor else existing?.startDate ?: s.anchor, latestCleaning ?: s.anchor, s.interval.toInt(), s.dueOverride, s.reminder && data.settings.reminders, cycle = UUID.randomUUID().toString())
                data.copy(plans = data.plans.filterNot { it.applianceId == id } + plan)
            } }.onSuccess { if (applianceId == id) { _state.update { it.copy(saving = false, dirty = false, saved = true) }; refresh(true) } }
                .onFailure { if (applianceId == id) _state.update { it.copy(saving = false, error = true) } }
        }
    }
    fun cleaningDialog(date: LocalDate?) { _state.update { it.copy(cleaningDate = date) } }
    fun markDescaled() {
        val s = _state.value
        val id = applianceId
        val date = s.cleaningDate ?: return
        val name = s.appliance?.name ?: return
        if (date > s.today || s.saving) return
        lastOperation = "clean"
        _state.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            runCatching { repository.update { data ->
                require(data.activeAppliances.any { it.id == id })
                val plan = requireNotNull(data.plans.find { it.applianceId == id })
                val newest = maxOf(date, data.cleanings.filter { it.applianceId == id }.maxOfOrNull { it.date } ?: date)
                val event = DescaleEvent(applianceId = id, name = name, date = date, nextDue = newest.plusDays(plan.interval.toLong()))
                PlanCalculator.recompute(data.copy(cleanings = data.cleanings + event), id)
            } }.onSuccess { if (applianceId == id) { _state.update { it.copy(saving = false, dirty = false, cleaningDate = null, saved = true) }; refresh(true) } }
                .onFailure { if (applianceId == id) _state.update { it.copy(saving = false, error = true) } }
        }
    }
    fun snooze() {
        val id = applianceId
        if (_state.value.saving) return
        lastOperation = "snooze"
        _state.update { it.copy(saving = true, error = false) }
        viewModelScope.launch {
            runCatching { repository.update { data -> data.copy(plans = data.plans.map { if (it.applianceId == id) it.copy(snooze = LocalDate.now().plusDays(7), cycle = UUID.randomUUID().toString(), lastDelivered = null) else it }) } }
                .onSuccess { if (applianceId == id) { _state.update { it.copy(saving = false, snoozed = true) }; refresh(false) } }
                .onFailure { if (applianceId == id) _state.update { it.copy(saving = false, error = true) } }
        }
    }
    fun openSettings() = emit(UiEvent.NotificationSettings)
}
