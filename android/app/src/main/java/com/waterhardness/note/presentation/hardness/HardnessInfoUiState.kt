package com.waterhardness.note.presentation.hardness

import com.waterhardness.note.domain.model.*

data class HardnessInfoUiState(val unit: WaterUnit = WaterUnit.MG_L, val canonical: Int? = null, val expanded: Hardness? = null, val fromTest: Boolean = false) {
    val selected: Hardness? get() = canonical?.let(com.waterhardness.note.domain.usecase.HardnessCalculator::category)
}
