package com.waterhardness.note.presentation.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.waterhardness.note.R
import com.waterhardness.note.core.theme.*
import com.waterhardness.note.core.ui.*
import com.waterhardness.note.domain.model.AnchorType
import com.waterhardness.note.presentation.dialog.DateSelectionDialog
import java.time.LocalDate

@Composable
fun DescalePlanScreen(state: DescalePlanUiState, onBack: () -> Unit, onInterval: (String) -> Unit, onStep: (Int) -> Unit, onAnchor: (LocalDate) -> Unit, onDue: (LocalDate) -> Unit, onClearDue: () -> Unit, onSuggestion: () -> Unit, onReminder: (Boolean) -> Unit, onSave: () -> Unit, onCleaningDate: (LocalDate?) -> Unit, onClean: () -> Unit, onSnooze: () -> Unit, onSettings: () -> Unit, onRetry: () -> Unit) {
    var picker by rememberSaveable { mutableStateOf<String?>(null) }
    val editable = !state.saving && !state.store.error
    NotebookPage(WaterTone.PLAN, footer = {
        Text(stringResource(R.string.plan_save_hint), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        PrimaryButton(R.string.save_plan, onSave, enabled = state.valid && editable, loading = state.saving)
    }) {
        ScreenHeader(R.string.plan_title, onBack)
        if (state.store.loading) LoadingCards()
        if (state.store.error) { InlineStatus(R.string.storage_error, true); SecondaryButton(R.string.retry, onRetry) }
        val appliance = state.appliance
        if (appliance != null) CareCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Illustration(appliance.type, 72); Text(appliance.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)) }
        } else InlineStatus(R.string.missing_appliance, true)
        CareCard {
            DueStatus(state.plan, state.today)
            if (state.draftDue != null) Text(dateText(requireNotNull(state.draftDue)), style = MaterialTheme.typography.displaySmall, color = OceanDeep, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        CareCard {
            SectionTitle(stringResource(if (state.plan?.anchorType == AnchorType.CLEANING) R.string.last_descaled else R.string.plan_start))
            Text(dateText(state.anchor))
            if (state.plan?.anchorType != AnchorType.CLEANING) {
                SecondaryButton(R.string.choose_date, { picker = "anchor" }, enabled = editable)
                SecondaryButton(R.string.start_today, { onAnchor(state.today) }, enabled = editable)
            }
            SectionTitle(stringResource(R.string.choose_interval))
            val status = stringResource(R.string.interval_days, state.interval)
            Column(Modifier.semantics { stateDescription = status }) {
                LargeStepper(status, editable && (state.interval.toIntOrNull() ?: 1) > 1, editable && (state.interval.toIntOrNull() ?: 365) < 365, { onStep(-1) }, { onStep(1) })
                OutlinedTextField(state.interval, onInterval, Modifier.fillMaxWidth(), enabled = editable, label = { Text(stringResource(R.string.choose_interval)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, isError = state.interval.toIntOrNull() !in 1..365)
            }
            if (state.interval.toIntOrNull() !in 1..365) InlineStatus(R.string.interval_error, true)
            if (state.suggestion != null) {
                Text(stringResource(R.string.suggestion, state.suggestion), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.suggestion_hint), style = MaterialTheme.typography.bodyMedium)
                SecondaryButton(R.string.use_suggestion, onSuggestion, enabled = editable)
            }
            SecondaryButton(R.string.change_due, { picker = "due" }, enabled = editable)
            if (state.dueOverride != null) { Text(stringResource(R.string.due_override, dateText(state.dueOverride))); SecondaryButton(R.string.clear_due, onClearDue, enabled = editable) }
            PreferenceToggle(R.string.plan_reminder, state.reminder, editable && state.store.data.settings.reminders && state.permission, onReminder)
            if (!state.store.data.settings.reminders) Text(stringResource(R.string.enable_global), style = MaterialTheme.typography.bodyMedium)
            if (!state.permission) { InlineStatus(R.string.permission_needed); SecondaryButton(R.string.open_settings, onSettings) }
            Text(stringResource(R.string.reminder_disclosure), style = MaterialTheme.typography.bodyMedium)
        }
        if (state.plan != null) {
            SecondaryButton(R.string.mark_descaled, { onCleaningDate(state.today) }, Modifier.fillMaxWidth(), enabled = editable && !state.dirty)
            SecondaryButton(R.string.snooze, onSnooze, Modifier.fillMaxWidth(), enabled = editable && !state.dirty)
        }
        if (state.error) { InlineStatus(R.string.write_error, true); SecondaryButton(R.string.retry, onRetry, enabled = !state.saving) }
        if (state.saved) InlineStatus(R.string.plan_saved)
        if (state.snoozed) InlineStatus(R.string.snooze_hint)
    }
    val cleaning = state.cleaningDate
    if (cleaning != null && picker != "cleaning") AlertDialog(onDismissRequest = { if (!state.saving) onCleaningDate(null) }, title = { Text(stringResource(R.string.cleaning_title)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.cleaning_hint, state.appliance?.name.orEmpty()))
            Text(dateText(cleaning))
            SecondaryButton(R.string.choose_date, { picker = "cleaning" }, enabled = !state.saving)
            if (state.error) InlineStatus(R.string.write_error, true)
            if (state.saving) CircularProgressIndicator(Modifier.size(24.dp))
        }
    }, confirmButton = { TextButton(onClick = onClean, enabled = !state.saving && cleaning <= state.today) { Text(stringResource(R.string.mark_descaled)) } }, dismissButton = { TextButton(onClick = { onCleaningDate(null) }, enabled = !state.saving) { Text(stringResource(R.string.cancel)) } })
    if (picker != null) DateSelectionDialog(initial = when (picker) { "due" -> state.dueOverride ?: state.draftDue ?: state.today; "cleaning" -> cleaning ?: state.today; else -> state.anchor }, earliest = if (picker == "due") state.today else null, latest = if (picker == "due") null else state.today, onDate = { date -> when (picker) { "due" -> onDue(date); "cleaning" -> onCleaningDate(date); else -> onAnchor(date) }; picker = null }, onDismiss = { picker = null })
}
