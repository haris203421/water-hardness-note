package com.waterhardness.note.presentation.settings

import com.waterhardness.note.domain.model.StoreState
import com.waterhardness.note.presentation.common.ChartEditor

data class SettingsUiState(val store: StoreState = StoreState(), val permission: Boolean = false, val saving: Boolean = false, val error: Boolean = false, val eraseConfirmation: Boolean = false, val chartEditor: ChartEditor? = null)
