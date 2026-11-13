package com.waterhardness.note.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waterhardness.note.R
import com.waterhardness.note.core.di.AppViewModelFactory
import com.waterhardness.note.core.theme.*
import com.waterhardness.note.core.ui.*

@Composable
fun SplashRoute(factory: AppViewModelFactory, onFinished: () -> Unit) {
    val model: SplashViewModel = viewModel(factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    val finished by model.finished.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.events.collect { onFinished() } }
    SplashScreen(state.copy(finished = finished))
}

@Composable
fun SplashScreen(state: SplashUiState) {
    var entered by remember { mutableStateOf(false) }
    SideEffect { entered = true }
    val entry by animateFloatAsState(if (entered) 1f else 0f, tween(500, easing = FastOutSlowInEasing), label = "splashEntry")
    val loop = rememberInfiniteTransition(label = "waterOrbit")
    val float by loop.animateFloat(-6f, 6f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val halo by loop.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "halo")
    Box(Modifier.fillMaxSize()) {
        WaterBackground(WaterTone.SPLASH)
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(220.dp).scale(halo).background(SurfaceWhite.copy(alpha = 0.12f), CircleShape).border(1.dp, SurfaceWhite.copy(alpha = 0.24f), CircleShape))
                Box(Modifier.size(186.dp).border(1.dp, SurfaceWhite.copy(alpha = 0.18f), CircleShape))
                Image(painterResource(R.drawable.logo), stringResource(R.string.emblem_description), Modifier.size(176.dp).offset(y = float.dp).alpha(entry).scale(0.94f + 0.06f * entry), contentScale = ContentScale.Fit)
                Box(Modifier.align(Alignment.TopEnd).size(28.dp).border(2.dp, ActionAqua.copy(alpha = 0.65f), CircleShape))
                Box(Modifier.align(Alignment.BottomStart).size(20.dp).border(2.dp, SurfaceWhite.copy(alpha = 0.5f), CircleShape))
                Box(Modifier.align(Alignment.BottomEnd).size(36.dp).border(1.dp, SurfaceWhite.copy(alpha = 0.35f), CircleShape))
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.app_name), Modifier.widthIn(max = 300.dp), style = MaterialTheme.typography.displaySmall, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.splash_subtitle), style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))
            LinearProgressIndicator(modifier = Modifier.width(160.dp).height(4.dp), color = ActionAqua, trackColor = SurfaceWhite.copy(alpha = 0.24f))
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.opening), style = MaterialTheme.typography.labelSmall, color = OceanDeep, textAlign = TextAlign.Center)
        }
    }
}
