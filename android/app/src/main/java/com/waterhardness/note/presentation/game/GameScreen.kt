package com.waterhardness.note.presentation.game

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.waterhardness.note.R
import com.waterhardness.note.core.theme.*
import com.waterhardness.note.core.ui.*
import com.waterhardness.note.domain.model.EntrySource
import com.waterhardness.note.domain.model.WaterUnit
import com.waterhardness.note.presentation.dialog.DateSelectionDialog
import java.time.LocalDate

@Composable
fun GameScreen(state: GameUiState, onBack: () -> Unit, onHelp: () -> Unit, onChart: () -> Unit, onSwatch: (Int) -> Unit, onPrinted: () -> Unit, onInput: (String) -> Unit, onStep: (Int) -> Unit, onEditValue: () -> Unit, onNote: (String) -> Unit, onDate: (LocalDate) -> Unit, onSave: () -> Unit, onNotSure: () -> Unit, onReplay: () -> Unit) {
    var datePicker by rememberSaveable { mutableStateOf(false) }
    val editable = !state.isSaving && state.savedRecordId == null
    NotebookPage(WaterTone.TEST, footer = {
        Text(stringResource(if (state.savedRecordId != null) R.string.saved_draft_hint else R.string.save_hint), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        if (state.savedRecordId != null) PrimaryButton(R.string.play_again, onReplay)
        else {
            PrimaryButton(R.string.save_result, onSave, enabled = state.canSave, loading = state.isSaving)
            TextButton(onClick = onNotSure, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Image(painterResource(R.drawable.icon_pause), null, Modifier.size(24.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.not_sure))
            }
        }
    }) {
        ScreenHeader(R.string.test_title, onBack, state.appliance?.name)
        SecondaryButton(R.string.help, onHelp)
        if (state.appliance == null || state.appliance.archived) InlineStatus(R.string.missing_appliance, true)
        CareCard {
            SectionTitle(stringResource(R.string.match_chart))
            Text(stringResource(R.string.chart_hint), style = MaterialTheme.typography.bodyMedium)
            Text(state.chart?.takeIf { it.verified }?.label ?: stringResource(R.string.example_chart), style = MaterialTheme.typography.labelSmall, color = PrimaryBurgundy)
            SecondaryButton(R.string.edit_chart, onChart, enabled = editable)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 520.dp) 3 else 2
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    (0..5).toList().chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { index ->
                                val value = state.chart?.values?.get(index) ?: listOf(0, 500, 1000, 1500, 2500, 4000)[index]
                                val chosen = state.swatch == index && state.source == EntrySource.CHART
                                val selectedText = stringResource(if (chosen) R.string.selected else R.string.not_selected)
                                val display = measurement(value, WaterUnit.MG_L)
                                CareCard(Modifier.weight(1f).selectable(chosen, enabled = editable, role = Role.RadioButton, onClick = { onSwatch(index) }).semantics { stateDescription = selectedText }, selected = chosen) {
                                    Box(Modifier.size(40.dp).background(ChartSwatches[state.chart?.swatches?.get(index) ?: index], RoundedCornerShape(10.dp)))
                                    Text(display, style = MaterialTheme.typography.bodyMedium)
                                    Text(selectedText, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            if (state.source == EntrySource.CHART && state.chart?.verified != true) InlineStatus(R.string.unverified_hint)
            SecondaryButton(R.string.use_printed, onPrinted, enabled = editable)
            if (state.source == EntrySource.PRINTED) {
                Text(stringResource(R.string.printed_hint), style = MaterialTheme.typography.bodyMedium)
                LargeStepper(state.input.ifEmpty { stringResource(R.string.choose_value) }, editable && state.canDecrease, editable && state.canIncrease, { onStep(-1) }, { onStep(1) })
                Text(unitText(state.unit), style = MaterialTheme.typography.labelSmall)
                if (!state.numericEditor) SecondaryButton(R.string.edit_value, onEditValue, enabled = editable)
                if (state.numericEditor) OutlinedTextField(state.input, onInput, Modifier.fillMaxWidth(), enabled = editable, label = { Text(stringResource(R.string.printed_value)) }, suffix = { Text(unitText(state.unit)) }, isError = state.inputError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                if (state.numericEditor && state.input.isNotBlank() && state.canonical == null && !state.inputError) SecondaryButton(R.string.use_value, { onInput(state.input) }, enabled = editable)
                if (state.inputError) InlineStatus(R.string.value_error, true)
            }
        }
        val category = state.category
        val canonical = state.canonical
        if (category != null && canonical != null) HardnessBadge(category, measurement(canonical, state.unit)) else CareCard { Text(stringResource(R.string.choose_value)) }
        CareCard {
            SectionTitle(stringResource(R.string.test_date))
            Text(dateText(state.date))
            SecondaryButton(R.string.choose_date, { datePicker = true }, enabled = editable)
            if (state.date > state.today) InlineStatus(R.string.future_date_error, true)
            OutlinedTextField(state.note, onNote, Modifier.fillMaxWidth(), enabled = editable, minLines = 2, label = { Text(stringResource(R.string.note)) }, supportingText = { Text(stringResource(R.string.note_count, state.note.length)) })
        }
        if (state.writeError) { InlineStatus(R.string.write_error, true); SecondaryButton(R.string.retry, onSave, enabled = state.canSave) }
    }
    if (datePicker) DateSelectionDialog(state.date, latest = state.today, onDate = { onDate(it); datePicker = false }, onDismiss = { datePicker = false })
}
