package com.waterhardness.note.presentation.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.waterhardness.note.R
import com.waterhardness.note.core.theme.ChartSwatches
import com.waterhardness.note.core.ui.*
import com.waterhardness.note.domain.model.ApplianceType
import com.waterhardness.note.presentation.common.ApplianceEditor
import com.waterhardness.note.presentation.common.ChartEditor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun ApplianceDialog(state: ApplianceEditor, onName: (String) -> Unit, onType: (ApplianceType, String) -> Unit, onSave: () -> Unit, onCancel: () -> Unit, onArchive: () -> Unit) {
    if (state.confirmArchive) {
        ConfirmationDialog(stringResource(R.string.archive_title, state.name), R.string.archive_body, R.string.archive, R.string.keep, state.saving, onArchive, onCancel)
        return
    }
    val names = ApplianceType.entries.associateWith { typeText(it) }
    AlertDialog(onDismissRequest = onCancel, title = { Text(stringResource(if (state.id == null) R.string.add_appliance else R.string.edit_appliance)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(state.name, onName, Modifier.fillMaxWidth(), enabled = !state.saving, label = { Text(stringResource(R.string.appliance_name)) }, singleLine = true, isError = state.error)
                if (state.error) InlineStatus(R.string.name_error, true)
                if (state.storageError) InlineStatus(R.string.write_error, true)
                Text(stringResource(R.string.appliance_type), style = MaterialTheme.typography.titleMedium)
                ApplianceType.entries.forEach { type ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Illustration(type, 48)
                        Box(Modifier.weight(1f)) { RadioChoice(names.getValue(type), state.type == type, { onType(type, names.getValue(type)) }, !state.saving) }
                    }
                }
                if (state.id != null) SecondaryButton(R.string.archive, onArchive, enabled = !state.saving)
                if (state.saving) CircularProgressIndicator(Modifier.size(24.dp))
            }
        }, confirmButton = { TextButton(onClick = onSave, enabled = !state.saving) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton(onClick = onCancel, enabled = !state.saving) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun ConfirmationDialog(title: String, body: Int, confirmLabel: Int, dismissLabel: Int, busy: Boolean = false, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(title) }, text = { Column { Text(stringResource(body)); if (busy) CircularProgressIndicator(Modifier.padding(top = 12.dp).size(24.dp)) } },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !busy) { Text(stringResource(confirmLabel)) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(dismissLabel)) } })
}

@Composable
fun ChartProfileDialog(state: ChartEditor, onValue: (Int, String) -> Unit, onStep: (Int, Int) -> Unit, onSwatch: (Int) -> Unit, onConfirmed: (Boolean) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel, title = { Text(stringResource(R.string.chart_title)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.chart_setup_hint))
                state.values.forEachIndexed { index, value ->
                    val colorState = stringResource(R.string.colour_number, state.swatches[index] + 1)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value, { onValue(index, it) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.chart_row, index + 1)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, enabled = !state.saving, isError = state.errorRow == index)
                        LargeStepper(value, !state.saving && state.canStep(index, -1), !state.saving && state.canStep(index, 1), { onStep(index, -1) }, { onStep(index, 1) })
                        OutlinedButton(onClick = { onSwatch(index) }, enabled = !state.saving, modifier = Modifier.heightIn(min = 48.dp).semantics { stateDescription = colorState }) {
                            Box(Modifier.size(24.dp).background(ChartSwatches[state.swatches[index]], RoundedCornerShape(6.dp)))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.change_colour))
                        }
                    }
                }
                if (state.errorRow != null) Text(stringResource(R.string.chart_error, state.errorRow + 1), color = MaterialTheme.colorScheme.error)
                if (state.storageError) InlineStatus(R.string.write_error, true)
                PreferenceToggle(R.string.chart_verified, state.confirmed, !state.saving, onConfirmed)
                if (state.saving) CircularProgressIndicator(Modifier.size(24.dp))
            }
        }, confirmButton = { TextButton(onClick = onSave, enabled = state.confirmed && !state.saving) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton(onClick = onCancel, enabled = !state.saving) { Text(stringResource(R.string.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionDialog(initial: LocalDate, earliest: LocalDate? = null, latest: LocalDate? = null, onDate: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
            return (earliest == null || date >= earliest) && (latest == null || date <= latest)
        }
    })
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = { dateState.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } }, enabled = dateState.selectedDateMillis != null) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }) {
        DatePicker(dateState, title = { Text(stringResource(R.string.date_title), Modifier.padding(20.dp)) }, showModeToggle = true)
    }
}
