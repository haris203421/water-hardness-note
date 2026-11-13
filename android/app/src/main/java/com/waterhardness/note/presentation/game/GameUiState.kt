package com.waterhardness.note.presentation.game

import com.waterhardness.note.domain.model.*
import com.waterhardness.note.presentation.common.ChartEditor
import java.time.LocalDate

data class GameUiState(
    val draftId: String = "", val appliance: Appliance? = null, val today: LocalDate = LocalDate.now(),
    val date: LocalDate = LocalDate.now(), val unit: WaterUnit = WaterUnit.MG_L, val chart: ChartProfile? = null,
    val swatch: Int? = null, val source: EntrySource? = null, val input: String = "", val canonical: Int? = null,
    val originalInput: String = "", val originalUnit: WaterUnit = WaterUnit.MG_L,
    val note: String = "", val dirty: Boolean = false, val isSaving: Boolean = false, val savedRecordId: String? = null,
    val inputError: Boolean = false, val writeError: Boolean = false, val discard: Boolean = false,
    val chartEditor: ChartEditor? = null, val numericEditor: Boolean = false
) {
    val canSave: Boolean get() = appliance != null && !appliance.archived && !date.isAfter(today) && canonical != null && (source == EntrySource.PRINTED || source == EntrySource.CHART && chart?.verified == true) && !isSaving && savedRecordId == null
    val category: Hardness? get() = canonical?.let(com.waterhardness.note.domain.usecase.HardnessCalculator::category)
    val canDecrease: Boolean get() = com.waterhardness.note.domain.usecase.HardnessCalculator.step(input, unit, -1) != null
    val canIncrease: Boolean get() = com.waterhardness.note.domain.usecase.HardnessCalculator.step(input, unit, 1) != null
}
