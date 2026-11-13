package com.waterhardness.note.presentation.menu

import com.waterhardness.note.domain.model.StoreState
import com.waterhardness.note.presentation.common.ApplianceEditor
import java.time.LocalDate

data class MenuUiState(val store: StoreState = StoreState(), val selectedId: String? = null, val editor: ApplianceEditor? = null, val exampleExpanded: Boolean = false, val today: LocalDate = LocalDate.now(), val writeError: Boolean = false, val examples: List<com.waterhardness.note.domain.model.ExampleCare> = emptyList())
