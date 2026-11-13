package com.waterhardness.note.data.repository

import com.waterhardness.note.data.local.CarePreferences
import com.waterhardness.note.data.reminder.LocalReminders
import com.waterhardness.note.domain.model.*
import com.waterhardness.note.domain.repository.CareRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class CareRepositoryImpl(private val local: CarePreferences, private val reminders: LocalReminders) : CareRepository {
    private val _state = MutableStateFlow(StoreState())
    override val state = _state.asStateFlow()
    override suspend fun load() {
        _state.update { it.copy(loading = true, error = false) }
        withContext(Dispatchers.IO) {
            runCatching { local.serialized { local.read().also { data ->
                runCatching { reminders.reconcile(data) }
                _state.update { it.copy(data = data, loading = false, error = false) }
            } } }
                .onFailure { _state.update { it.copy(loading = false, error = true) } }
        }
    }
    override fun examples(today: java.time.LocalDate) = com.waterhardness.note.data.sample.SampleData.examples(today)
    override suspend fun update(transform: (CareData) -> CareData): CareData = withContext(Dispatchers.IO) {
        try {
            local.serialized {
                val previous = local.read()
                val next = transform(previous)
                local.write(next)
                _state.update { it.copy(data = next, loading = false, error = false) }
                runCatching { reminders.reconcile(next, previous) }
                next
            }
        } catch (failure: Exception) {
            _state.update { it.copy(loading = false, error = true) }
            throw failure
        }
    }
    override suspend fun erase() = withContext(Dispatchers.IO) {
        local.serialized {
            val previous = runCatching { local.read() }.getOrDefault(_state.value.data)
            local.write(CareData())
            runCatching { reminders.clearAll(previous) }
            _state.update { it.copy(data = CareData(), loading = false, error = false) }
        }
    }
}
