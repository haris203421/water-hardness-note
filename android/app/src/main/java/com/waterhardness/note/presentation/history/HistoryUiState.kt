package com.waterhardness.note.presentation.history

import com.waterhardness.note.domain.model.*
import java.time.Instant
import java.time.LocalDate

enum class HistoryFilter { ALL, TESTS, DESCALING }
data class HistoryEntry(val id: String, val applianceId: String, val name: String, val date: LocalDate, val createdAt: Instant, val test: WaterTest? = null, val cleaning: DescaleEvent? = null, val archived: Boolean = false)
data class HistoryUiState(val store: StoreState = StoreState(), val applianceId: String? = null, val filter: HistoryFilter = HistoryFilter.ALL, val entries: List<HistoryEntry> = emptyList(), val testCount: Int = 0, val cleaningCount: Int = 0, val detail: HistoryEntry? = null, val confirmDelete: Boolean = false, val saving: Boolean = false, val error: Boolean = false)
