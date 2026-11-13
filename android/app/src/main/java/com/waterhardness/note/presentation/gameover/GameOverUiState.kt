package com.waterhardness.note.presentation.gameover

import com.waterhardness.note.domain.model.*

data class GameOverUiState(val loading: Boolean = false, val needsReview: Boolean = true, val record: WaterTest? = null, val appliance: Appliance? = null, val plan: DescalePlan? = null, val unavailable: Boolean = false, val unit: WaterUnit = WaterUnit.MG_L, val today: java.time.LocalDate = java.time.LocalDate.now())
