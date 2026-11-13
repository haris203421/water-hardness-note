package com.waterhardness.note.domain.repository

import com.waterhardness.note.domain.model.CareData
import com.waterhardness.note.domain.model.StoreState
import kotlinx.coroutines.flow.StateFlow

interface CareRepository {
    val state: StateFlow<StoreState>
    suspend fun load()
    suspend fun update(transform: (CareData) -> CareData): CareData
    suspend fun erase()
    fun examples(today: java.time.LocalDate): List<com.waterhardness.note.domain.model.ExampleCare>
}

interface ReminderGateway {
    fun permissionAvailable(): Boolean
    suspend fun refresh()
}
