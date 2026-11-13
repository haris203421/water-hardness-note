package com.waterhardness.note.presentation.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.waterhardness.note.R
import com.waterhardness.note.core.theme.*
import com.waterhardness.note.core.ui.*
import com.waterhardness.note.domain.model.Appliance

@Composable
fun MenuScreen(state: MenuUiState, inactivePlan: Boolean, onPlay: () -> Unit, onAdd: () -> Unit, onSelect: (String) -> Unit, onEdit: (Appliance) -> Unit, onPlan: (String) -> Unit, onHistory: () -> Unit, onInfo: () -> Unit, onSettings: () -> Unit, onExample: () -> Unit, onRetry: () -> Unit) {
    val data = state.store.data
    NotebookPage(WaterTone.MENU, footer = {
        Text(stringResource(if (data.activeAppliances.isEmpty()) R.string.play_empty_hint else R.string.play_hint), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        PrimaryButton(R.string.play, onPlay, enabled = !state.store.loading && !state.store.error)
    }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.your_appliances), Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, color = Ink)
            TextButton(onClick = onSettings, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.settings)) }
        }
        Surface(shape = RoundedCornerShape(20.dp), shadowElevation = 5.dp, color = PrimaryBurgundy) {
            Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(PrimaryBurgundy, BurgundyDeep))).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.hero_title), style = MaterialTheme.typography.titleLarge, color = SurfaceWhite)
                    Text(stringResource(R.string.hero_caption), style = MaterialTheme.typography.bodyMedium, color = SurfaceWhite.copy(alpha = 0.88f))
                }
                Image(painterResource(R.drawable.logo), null, Modifier.size(80.dp), contentScale = ContentScale.Fit)
            }
        }
        if (inactivePlan) InlineStatus(R.string.inactive_plan)
        if (state.store.loading) LoadingCards()
        if (state.store.error) { InlineStatus(R.string.storage_error, true); SecondaryButton(R.string.retry, onRetry) }
        if (state.writeError) InlineStatus(R.string.write_error, true)
        if (!state.store.loading && !state.store.error && data.activeAppliances.isEmpty()) {
            CareCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Illustration(com.waterhardness.note.domain.model.ApplianceType.KETTLE, 72)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.start_kettle), style = MaterialTheme.typography.titleLarge, color = BurgundyDeep, modifier = Modifier.weight(1f))
                }
                Text(stringResource(R.string.empty_appliances))
                SecondaryButton(R.string.add_appliance, onAdd)
            }
        } else if (data.activeAppliances.isNotEmpty()) {
            val dueCount = data.plans.count { it.dueDate <= state.today && data.activeAppliances.any { a -> a.id == it.applianceId } }
            if (dueCount > 0) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { StatCard(R.string.appliances, data.activeAppliances.size, Modifier.weight(1f)); StatCard(R.string.plans_due, dueCount, Modifier.weight(1f)) }
            SectionTitle(stringResource(R.string.appliances))
            SecondaryButton(R.string.add_appliance, onAdd, enabled = !state.store.error)
            data.activeAppliances.forEach { appliance ->
                val selected = state.selectedId == appliance.id
                val status = stringResource(if (selected) R.string.selected else R.string.not_selected)
                val latest = data.tests.filter { it.applianceId == appliance.id }.maxWithOrNull(compareBy<com.waterhardness.note.domain.model.WaterTest> { it.date }.thenBy { it.createdAt })
                CareCard(Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(appliance.id) }).semantics { stateDescription = status }, selected) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Illustration(appliance.type)
                        Column(Modifier.weight(1f)) {
                            Text(appliance.name, style = MaterialTheme.typography.titleMedium)
                            Text(status, style = MaterialTheme.typography.labelSmall, color = PrimaryBurgundy)
                            Text(if (latest == null) stringResource(R.string.no_test) else categoryText(latest.category), style = MaterialTheme.typography.bodyMedium)
                        }
                        RadioButton(selected, null)
                    }
                    DueStatus(data.plans.find { it.applianceId == appliance.id }, state.today)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onPlan(appliance.id) }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.open_plan)) }
                        TextButton(onClick = { onEdit(appliance) }, Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.edit)) }
                    }
                }
            }
        }
        SecondaryButton(R.string.history, onHistory, Modifier.fillMaxWidth())
        SecondaryButton(R.string.hardness_info, onInfo, Modifier.fillMaxWidth())
        if (data.settings.examples && !state.store.error) CareCard {
            SectionTitle(stringResource(R.string.example_label))
            TextButton(onClick = onExample, Modifier.heightIn(min = 48.dp)) { Text(stringResource(if (state.exampleExpanded) R.string.hide_example else R.string.see_example)) }
            if (state.exampleExpanded) {
                state.examples.take(2).forEach { example ->
                    Text(typeText(example.type), style = MaterialTheme.typography.titleMedium)
                    Text(measurement(example.value, data.settings.unit))
                    Text(categoryText(example.category))
                    Text(stringResource(R.string.example_dates, dateText(example.testDate), dateText(example.cleaned), dateText(example.dueDate)), style = MaterialTheme.typography.bodyMedium)
                }
                Text(stringResource(R.string.example_note), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
        }
    }
}
