package com.waterhardness.note.presentation.hardness

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.waterhardness.note.R
import com.waterhardness.note.core.ui.*
import com.waterhardness.note.domain.model.Hardness
import com.waterhardness.note.domain.model.WaterUnit

@Composable
fun HardnessInfoScreen(state: HardnessInfoUiState, onBack: () -> Unit, onExpand: (Hardness) -> Unit) {
    NotebookPage(WaterTone.INFO, footer = { PrimaryButton(if (state.fromTest) R.string.back_to_test else R.string.back, onBack) }) {
        ScreenHeader(R.string.hardness_title, onBack)
        Image(painterResource(R.drawable.sprite_5), null, Modifier.size(96.dp).align(Alignment.CenterHorizontally), contentScale = ContentScale.Fit)
        SectionTitle(stringResource(R.string.hardness_intro))
        val ranges = listOf(0 to 600, 600 to 1200, 1200 to 1800, 1800 to 10000)
        val descriptions = listOf(R.string.soft_range, R.string.moderate_range, R.string.hard_range, R.string.very_hard_range)
        Hardness.entries.forEachIndexed { index, category ->
            val expanded = state.expanded == category
            val status = stringResource(if (expanded) R.string.expanded else R.string.collapsed)
            val selected = state.selected == category
            CareCard(Modifier.animateContentSize(tween(180)).semantics { stateDescription = status }, selected) {
                SectionTitle(categoryText(category))
                Text(stringResource(descriptions[index]), style = MaterialTheme.typography.bodyMedium)
                if (selected) Text(stringResource(R.string.latest_estimate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(categoryHint(category))
                SecondaryButton(if (expanded) R.string.hide_details else R.string.show_details, { onExpand(category) })
                if (expanded) {
                    if (state.unit != WaterUnit.MG_L) Text(stringResource(R.string.approx_range, measurement(ranges[index].first, state.unit), measurement(ranges[index].second, state.unit)), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.hardness_explanation), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        CareCard { Text(stringResource(R.string.hardness_explanation)) }
    }
}
