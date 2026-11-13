package com.waterhardness.note.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.waterhardness.note.R
import com.waterhardness.note.core.theme.*

enum class WaterTone { SPLASH, MENU, TEST, SAVED, REVIEW, INFO, PLAN, HISTORY, SETTINGS }

@Composable
fun WaterBackground(tone: WaterTone) {
    val colors = when (tone) {
        WaterTone.SPLASH -> listOf(OceanDeep.copy(alpha = 0.98f), OceanDeep.copy(alpha = 0.9f), OceanBright.copy(alpha = 0.85f), OceanPale.copy(alpha = 0.94f))
        WaterTone.MENU -> listOf(BackgroundBlush.copy(alpha = 0.97f), SurfaceWhite.copy(alpha = 0.95f), OceanMist.copy(alpha = 0.94f))
        WaterTone.TEST -> listOf(OceanMist.copy(alpha = 0.96f), SurfaceWhite.copy(alpha = 0.97f), BackgroundBlush.copy(alpha = 0.94f))
        WaterTone.SAVED -> listOf(OceanPale.copy(alpha = 0.95f), SurfaceWhite.copy(alpha = 0.97f), BackgroundBlush.copy(alpha = 0.96f))
        WaterTone.REVIEW -> listOf(BackgroundBlush.copy(alpha = 0.97f), SurfaceWhite.copy(alpha = 0.96f), MineralRose.copy(alpha = 0.93f))
        WaterTone.INFO -> listOf(SurfaceWhite.copy(alpha = 0.97f), OceanMist.copy(alpha = 0.96f), OceanPale.copy(alpha = 0.95f))
        else -> listOf(BackgroundBlush.copy(alpha = 0.97f), SurfaceWhite.copy(alpha = 0.95f), OceanPale.copy(alpha = 0.95f))
    }
    Image(painterResource(R.drawable.bg_game), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors)))
}

@Composable
fun NotebookPage(tone: WaterTone, footer: (@Composable ColumnScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        WaterBackground(tone)
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.weight(1f).widthIn(max = 680.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
            if (footer != null) Column(Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = footer)
        }
    }
}
