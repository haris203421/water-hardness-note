package com.waterhardness.note.presentation.plan

import com.waterhardness.note.domain.model.*
import java.time.LocalDate

data class DescalePlanUiState(val store: StoreState = StoreState(), val appliance: Appliance? = null, val plan: DescalePlan? = null, val today: LocalDate = LocalDate.now(), val anchor: LocalDate = LocalDate.now(), val interval: String = "30", val dueOverride: LocalDate? = null, val reminder: Boolean = false, val permission: Boolean = false, val suggestion: Int? = null, val dirty: Boolean = false, val saving: Boolean = false, val error: Boolean = false, val saved: Boolean = false, val discard: Boolean = false, val cleaningDate: LocalDate? = null, val snoozed: Boolean = false) {
    val valid: Boolean get() = interval.toIntOrNull() in 1..365 && !anchor.isAfter(today) && (dueOverride == null || !dueOverride.isBefore(today)) && appliance?.archived == false
    val draftDue: LocalDate? get() = interval.toIntOrNull()?.takeIf { it in 1..365 }?.let { dueOverride ?: anchor.plusDays(it.toLong()) }
}
