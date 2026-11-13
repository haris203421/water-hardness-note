package com.waterhardness.note.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.waterhardness.note.R
import com.waterhardness.note.core.ui.*
import com.waterhardness.note.presentation.dialog.ConfirmationDialog

@Composable
fun HistoryScreen(state: HistoryUiState, onBack: () -> Unit, onFilter: (HistoryFilter) -> Unit, onAppliance: (String?) -> Unit, onClear: () -> Unit, onOpen: (HistoryEntry?) -> Unit, onDeleteRequest: (Boolean) -> Unit, onDelete: () -> Unit, onRecord: () -> Unit, onRetry: () -> Unit) {
    var picker by rememberSaveable { mutableStateOf(false) }
    val data = state.store.data
    Box(Modifier.fillMaxSize()) {
        WaterBackground(WaterTone.HISTORY)
        LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().widthIn(max = 680.dp).align(Alignment.TopCenter), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { ScreenHeader(R.string.history_title, onBack) }
            item {
                CareCard {
                    Text(data.appliances.find { it.id == state.applianceId }?.name ?: stringResource(R.string.all_appliances), style = MaterialTheme.typography.titleMedium)
                    SecondaryButton(R.string.choose_appliance, { picker = true })
                    HistoryFilter.entries.forEach { filter ->
                        val label = stringResource(when (filter) { HistoryFilter.ALL -> R.string.all; HistoryFilter.TESTS -> R.string.tests; HistoryFilter.DESCALING -> R.string.descaling })
                        RadioChoice(label, state.filter == filter, { onFilter(filter) })
                    }
                }
            }
            if (state.store.loading) item { Column { LoadingCards() } }
            if (state.store.error) item { Column { InlineStatus(R.string.storage_error, true); SecondaryButton(R.string.retry, onRetry) } }
            if (state.error) item { InlineStatus(R.string.write_error, true) }
            if (state.testCount > 0 && state.cleaningCount > 0) item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { StatCard(R.string.test_count, state.testCount, Modifier.weight(1f)); StatCard(R.string.descale_count, state.cleaningCount, Modifier.weight(1f)) } }
            if (!state.store.loading && state.entries.isEmpty()) item {
                CareCard {
                    if (data.tests.isEmpty() && data.cleanings.isEmpty()) { SectionTitle(stringResource(R.string.history_empty)); Text(stringResource(R.string.history_empty_hint)); PrimaryButton(R.string.record_test, onRecord) }
                    else { Text(stringResource(R.string.filtered_empty)); SecondaryButton(R.string.clear_filters, onClear) }
                }
            }
            state.entries.groupBy { it.date }.forEach { (date, entries) ->
                item(key = date.toString()) { SectionTitle(dateText(date)) }
                items(entries, key = { it.id }) { entry ->
                    CareCard {
                        Text(entry.name, style = MaterialTheme.typography.titleMedium)
                        if (entry.archived) Text(stringResource(R.string.archived_appliance), style = MaterialTheme.typography.labelSmall)
                        val test = entry.test
                        if (test != null) { Text(stringResource(R.string.strip_estimate), style = MaterialTheme.typography.labelSmall); HardnessBadge(test.category, measurement(test.canonical, data.settings.unit)) }
                        else { SectionTitle(stringResource(R.string.descaling)); entry.cleaning?.let { Text(stringResource(R.string.next_due, dateText(it.nextDue))) } }
                        SecondaryButton(R.string.open, { onOpen(entry) })
                    }
                }
            }
        }
    }
    if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text(stringResource(R.string.appliances)) }, text = {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            RadioChoice(stringResource(R.string.all_appliances), state.applianceId == null, { onAppliance(null); picker = false })
            data.appliances.forEach { appliance -> RadioChoice(appliance.name, state.applianceId == appliance.id, { onAppliance(appliance.id); picker = false }) }
        }
    }, confirmButton = { TextButton(onClick = { picker = false }) { Text(stringResource(R.string.back)) } })
    val detail = state.detail
    if (detail != null) {
        if (state.confirmDelete) ConfirmationDialog(stringResource(R.string.delete_title), R.string.delete_hint, R.string.delete, R.string.keep, state.saving, onDelete, { onDeleteRequest(false) })
        else AlertDialog(onDismissRequest = { onOpen(null) }, title = { Text(stringResource(R.string.entry_details)) }, text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(detail.name, style = MaterialTheme.typography.titleMedium)
                Text(dateText(detail.date))
                detail.test?.let { test ->
                    Text(measurement(test.canonical, data.settings.unit))
                    Text(categoryText(test.category))
                    Text(stringResource(R.string.original_value, test.originalInput, unitText(test.unit)))
                    Text(if (test.chart == null) stringResource(R.string.direct_source) else stringResource(R.string.chart_source, test.chart.label))
                    if (test.note.isNotBlank()) Text(test.note)
                }
                detail.cleaning?.let { Text(stringResource(R.string.next_due, dateText(it.nextDue))) }
                if (state.error) InlineStatus(R.string.write_error, true)
            }
        }, confirmButton = { TextButton(onClick = { onDeleteRequest(true) }) { Text(stringResource(R.string.delete_entry)) } }, dismissButton = { TextButton(onClick = { onOpen(null) }) { Text(stringResource(R.string.back)) } })
    }
}
