package com.waterhardness.note.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.waterhardness.note.R
import com.waterhardness.note.core.ui.*
import com.waterhardness.note.domain.model.WaterUnit
import com.waterhardness.note.presentation.dialog.ConfirmationDialog

@Composable
fun SettingsScreen(state: SettingsUiState, onBack: () -> Unit, onUnit: (WaterUnit) -> Unit, onReminders: (Boolean) -> Unit, onHaptics: (Boolean) -> Unit, onExamples: (Boolean) -> Unit, onChart: () -> Unit, onSettings: () -> Unit, onEraseConfirmation: (Boolean) -> Unit, onErase: () -> Unit, onRetry: () -> Unit) {
    val data = state.store.data
    val enabled = !state.saving && !state.store.loading && !state.store.error
    NotebookPage(WaterTone.SETTINGS) {
        ScreenHeader(R.string.settings_title, onBack)
        if (state.store.loading) LoadingCards()
        if (state.store.error) { InlineStatus(R.string.storage_error, true); SecondaryButton(R.string.retry, onRetry) }
        if (state.error) { InlineStatus(R.string.write_error, true); SecondaryButton(R.string.retry, onRetry, enabled = !state.saving) }
        CareCard {
            SectionTitle(stringResource(R.string.units_title))
            Text(stringResource(R.string.units_hint), style = MaterialTheme.typography.bodyMedium)
            WaterUnit.entries.forEach { unit -> RadioChoice(unitText(unit), data.settings.unit == unit, { onUnit(unit) }, enabled) }
            Text(stringResource(R.string.unit_preview, measurement(1500, data.settings.unit)), style = MaterialTheme.typography.bodyMedium)
        }
        CareCard {
            SectionTitle(stringResource(R.string.reminders_title))
            PreferenceToggle(R.string.global_reminders, data.settings.reminders && state.permission, enabled, onReminders)
            if (!state.permission) InlineStatus(R.string.permission_needed)
            else if (data.settings.reminders) Text(stringResource(R.string.reminders_on, data.plans.count { it.reminderEnabled && data.activeAppliances.any { a -> a.id == it.applianceId } }))
            else InlineStatus(R.string.off)
            Text(stringResource(R.string.reminder_disclosure), style = MaterialTheme.typography.bodyMedium)
            SecondaryButton(R.string.open_settings, onSettings)
        }
        CareCard {
            SectionTitle(stringResource(R.string.entry_preferences))
            PreferenceToggle(R.string.haptics, data.settings.haptics, enabled, onHaptics)
            PreferenceToggle(R.string.show_examples, data.settings.examples, enabled, onExamples)
            Text(data.activeChart?.label ?: stringResource(R.string.example_chart), style = MaterialTheme.typography.bodyMedium)
            SecondaryButton(R.string.edit_chart, onChart, enabled = enabled)
        }
        CareCard {
            SectionTitle(stringResource(R.string.on_device))
            Text(stringResource(R.string.local_promise))
            Text(stringResource(R.string.data_counts, data.tests.size, data.appliances.size, data.cleanings.size), style = MaterialTheme.typography.bodyMedium)
            SecondaryButton(R.string.erase_data, { onEraseConfirmation(true) }, Modifier.fillMaxWidth(), enabled = !state.saving)
            if (state.saving) CircularProgressIndicator()
        }
        SecondaryButton(R.string.back, onBack, Modifier.fillMaxWidth())
    }
    if (state.eraseConfirmation) ConfirmationDialog(stringResource(R.string.erase_title), R.string.erase_hint, R.string.erase, R.string.keep_data, state.saving, onErase, { onEraseConfirmation(false) })
}
