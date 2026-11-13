package com.waterhardness.note.presentation.gameover

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.waterhardness.note.R
import com.waterhardness.note.core.theme.*
import com.waterhardness.note.core.ui.*
import java.time.LocalDate

@Composable
fun GameOverScreen(state: GameOverUiState, onReplay: () -> Unit, onMenu: () -> Unit, onPlan: () -> Unit, onHistory: () -> Unit, onInfo: () -> Unit, onRetry: () -> Unit) {
    NotebookPage(if (state.needsReview) WaterTone.REVIEW else WaterTone.SAVED, footer = {
        Text(stringResource(R.string.record_another), style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        PrimaryButton(R.string.play_again, onReplay)
        SecondaryButton(R.string.menu, onMenu, Modifier.fillMaxWidth(), icon = R.drawable.icon_home)
    }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(12.dp))
            Image(painterResource(R.drawable.sprite_5), null, Modifier.size(104.dp), contentScale = ContentScale.Fit)
            if (!state.unavailable && !state.loading) OutcomeMark(state.needsReview)
            Text(stringResource(if (state.needsReview) R.string.needs_review else if (state.unavailable) R.string.result_unavailable else R.string.result_saved), style = MaterialTheme.typography.displaySmall, color = BurgundyDeep, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite })
        }
        when {
            state.loading -> LoadingCards()
            state.unavailable -> { InlineStatus(R.string.result_unavailable, true); SecondaryButton(R.string.retry, onRetry) }
            state.needsReview -> CareCard {
                SectionTitle(stringResource(R.string.no_result_saved))
                Text(stringResource(R.string.review_hint))
                SecondaryButton(R.string.hardness_info, onInfo)
            }
            else -> {
                val record = state.record
                if (record != null) CareCard {
                    Text(record.name, style = MaterialTheme.typography.titleLarge)
                    Text(dateText(record.date), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.strip_estimate), style = MaterialTheme.typography.labelSmall)
                    HardnessBadge(record.category, measurement(record.canonical, state.unit))
                    if (record.note.isNotBlank()) Text(record.note, style = MaterialTheme.typography.bodyMedium)
                }
                CareCard {
                    if (state.plan == null) Text(stringResource(R.string.give_care_date)) else DueStatus(state.plan, state.today)
                    SecondaryButton(if (state.plan == null) R.string.set_plan else R.string.open_plan, onPlan, enabled = state.appliance?.archived == false)
                }
                SecondaryButton(R.string.view_history, onHistory, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun OutcomeMark(review: Boolean) {
    var appeared by remember(review) { mutableStateOf(false) }
    SideEffect { appeared = true }
    val progress by animateFloatAsState(if (appeared) 1f else 0f, tween(220), label = "outcomeMark")
    val description = stringResource(if (review) R.string.no_result_saved else R.string.result_saved)
    Canvas(Modifier.size(40.dp).semantics { contentDescription = description; stateDescription = description }) {
        val mark = Path().apply {
            if (review) {
                moveTo(size.width * 0.3f, size.height * 0.3f)
                cubicTo(size.width * 0.3f, size.height * 0.05f, size.width * 0.85f, size.height * 0.1f, size.width * 0.68f, size.height * 0.42f)
                lineTo(size.width * 0.5f, size.height * 0.6f)
            } else {
                moveTo(size.width * 0.16f, size.height * 0.5f)
                lineTo(size.width * 0.4f, size.height * 0.74f)
                lineTo(size.width * 0.86f, size.height * 0.23f)
            }
        }
        val measure = PathMeasure().apply { setPath(mark, false) }
        val visible = Path()
        measure.getSegment(0f, measure.length * progress, visible, true)
        drawPath(visible, PrimaryBurgundy, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (review) drawCircle(PrimaryBurgundy, 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.82f), alpha = progress)
    }
}
