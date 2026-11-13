package com.waterhardness.note.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.waterhardness.note.domain.model.*
import com.waterhardness.note.domain.usecase.HardnessCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

fun dateText(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

@Composable
fun unitText(unit: WaterUnit): String = stringResource(when (unit) { WaterUnit.MG_L -> R.string.unit_mg; WaterUnit.DH -> R.string.unit_dh; WaterUnit.MMOL_L -> R.string.unit_mmol })
@Composable
fun categoryText(category: Hardness): String = stringResource(when (category) { Hardness.SOFT -> R.string.soft; Hardness.MODERATE -> R.string.moderate; Hardness.HARD -> R.string.hard; Hardness.VERY_HARD -> R.string.very_hard })
@Composable
fun categoryHint(category: Hardness): String = stringResource(when (category) { Hardness.SOFT -> R.string.soft_hint; Hardness.MODERATE -> R.string.moderate_hint; Hardness.HARD -> R.string.hard_hint; Hardness.VERY_HARD -> R.string.very_hard_hint })
@Composable
fun typeText(type: ApplianceType): String = stringResource(when (type) { ApplianceType.KETTLE -> R.string.kettle; ApplianceType.COFFEE -> R.string.coffee; ApplianceType.DISPENSER -> R.string.dispenser; ApplianceType.OTHER -> R.string.other })
@Composable
fun measurement(value: Int, unit: WaterUnit): String = stringResource(R.string.measurement, HardnessCalculator.format(value, unit), unitText(unit))

@Composable
fun CareCard(modifier: Modifier = Modifier, selected: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val borderColor by animateColorAsState(if (selected) PrimaryBurgundy else MineralRose.copy(alpha = 0.7f), tween(150), label = "cardSelectionColor")
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, tween(150), label = "cardSelectionWidth")
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), border = BorderStroke(borderWidth, borderColor), colors = CardDefaults.cardColors(containerColor = SurfaceWhite.copy(alpha = 0.94f)), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge, color = BurgundyDeep, modifier = Modifier.semantics { heading() }) }

@Composable
fun ScreenHeader(title: Int, onBack: () -> Unit, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SecondaryButton(R.string.back, onBack)
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
    }
}

@Composable
fun Illustration(type: ApplianceType, size: Int = 56) {
    val asset = when (type) { ApplianceType.KETTLE -> R.drawable.sprite_1; ApplianceType.COFFEE -> R.drawable.sprite_2; ApplianceType.DISPENSER -> R.drawable.sprite_3; ApplianceType.OTHER -> R.drawable.sprite_4 }
    Image(painterResource(asset), typeText(type), Modifier.size(size.dp), contentScale = ContentScale.Fit)
}

@Composable
fun HardnessBadge(category: Hardness, value: String? = null) {
    val label = categoryText(category)
    val status = if (value == null) label else stringResource(R.string.category_value, label, value)
    Surface(shape = RoundedCornerShape(14.dp), color = OceanMist, border = BorderStroke(1.dp, OceanLine), modifier = Modifier.fillMaxWidth().semantics { stateDescription = status; liveRegion = LiveRegionMode.Polite }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = BurgundyDeep)
            if (value != null) Text(value, style = MaterialTheme.typography.headlineMedium, color = Ink)
            Text(categoryHint(category), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        }
    }
}

@Composable
fun DueStatus(plan: DescalePlan?, today: LocalDate) {
    val status = if (plan == null) stringResource(R.string.no_plan) else {
        val days = ChronoUnit.DAYS.between(today, plan.dueDate).toInt()
        when { days > 0 -> androidx.compose.ui.res.pluralStringResource(R.plurals.due_in, days, days); days == 0 -> stringResource(R.string.due_today); else -> androidx.compose.ui.res.pluralStringResource(R.plurals.overdue, -days, -days) }
    }
    Column(Modifier.semantics { stateDescription = status; liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(status, color = PrimaryBurgundy, style = MaterialTheme.typography.titleMedium)
        if (plan != null) {
            Text(dateText(plan.dueDate), style = MaterialTheme.typography.bodyMedium)
            if (plan.snooze != null && plan.snooze > today) Text(stringResource(R.string.snoozed_until, dateText(plan.snooze)), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun StatCard(label: Int, value: Int, modifier: Modifier = Modifier) {
    if (value > 0) Surface(modifier, shape = RoundedCornerShape(16.dp), color = OceanPale.copy(alpha = 0.9f), border = BorderStroke(1.dp, OceanLine)) {
        Column(Modifier.padding(16.dp)) { Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = BurgundyDeep); Text(stringResource(label), style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
fun InlineStatus(message: Int, error: Boolean = false) {
    val text = stringResource(message)
    Text(text, color = if (error) PrimaryBurgundy else InkMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; if (error) this.error(text) })
}

@Composable
fun LoadingCards() {
    CircularProgressIndicator(Modifier.size(28.dp), color = OceanDeep)
    repeat(2) { Box(Modifier.fillMaxWidth().height(76.dp).background(Brush.horizontalGradient(listOf(OceanPale, OceanMist)), RoundedCornerShape(18.dp))) }
}

@Composable
fun PreferenceToggle(label: Int, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val status = stringResource(if (checked) R.string.on else R.string.off)
    val labelText = stringResource(label)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics(mergeDescendants = true) { stateDescription = status }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = labelText })
    }
}

@Composable
fun RadioChoice(label: String, selected: Boolean, onSelect: () -> Unit, enabled: Boolean = true) {
    val state = stringResource(if (selected) R.string.selected else R.string.not_selected)
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect).semantics { stateDescription = state }, verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, null, enabled = enabled)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) Ink else InkMuted)
    }
}
