package com.waterhardness.note.presentation.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waterhardness.note.core.di.ServiceLocator
import com.waterhardness.note.core.navigation.Screen
import com.waterhardness.note.presentation.splash.SplashRoute

@Composable
fun AppRoot() {
    val factory = ServiceLocator.factory
    val model: AppViewModel = viewModel(factory = factory)
    val screen by model.screen.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    SideEffect {
        context.activity()?.window?.let { window ->
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = screen != Screen.Splash
                isAppearanceLightNavigationBars = true
            }
        }
    }
    LaunchedEffect(Unit) { ServiceLocator.planRequests.collect(model::openPlanIntent) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) model.foreground() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = screen != Screen.Menu && screen != Screen.Splash) { model.back() }
    AnimatedContent(targetState = screen, transitionSpec = { fadeIn(tween(if (initialState == Screen.Splash) 400 else 180)) togetherWith fadeOut(tween(if (initialState == Screen.Splash) 400 else 180)) }, label = "notebookNavigation") { current ->
        when (current) {
            Screen.Splash -> SplashRoute(factory, model::finishSplash)
            Screen.Menu -> MenuRoute(factory, model, state.inactivePlan)
            is Screen.Game -> GameRoute(current, factory, model)
            is Screen.GameOver -> GameOverRoute(current, factory, model)
            is Screen.HardnessInfo -> HardnessRoute(current, factory, model)
            is Screen.DescalePlan -> PlanRoute(current, factory, model)
            is Screen.History -> HistoryRoute(current, factory, model)
            Screen.Settings -> SettingsRoute(factory, model)
        }
    }
}
