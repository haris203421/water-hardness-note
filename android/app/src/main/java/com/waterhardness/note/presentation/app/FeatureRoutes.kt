package com.waterhardness.note.presentation.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.waterhardness.note.R
import com.waterhardness.note.core.di.AppViewModelFactory
import com.waterhardness.note.core.navigation.Screen
import com.waterhardness.note.presentation.common.*
import com.waterhardness.note.presentation.dialog.*
import com.waterhardness.note.presentation.game.*
import com.waterhardness.note.presentation.gameover.*
import com.waterhardness.note.presentation.hardness.*
import com.waterhardness.note.presentation.history.*
import com.waterhardness.note.presentation.menu.*
import com.waterhardness.note.presentation.plan.*
import com.waterhardness.note.presentation.settings.*

@Composable
private fun RouteEvents(model: CareViewModel, origin: Screen, app: AppViewModel, permission: (() -> Unit)? = null) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val current by app.screen.collectAsStateWithLifecycle()
    if (current == origin) LaunchedEffect(Unit) {
        model.events.collect { event ->
            when (event) {
                UiEvent.Haptic -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                UiEvent.NotificationSettings -> openNotificationSettings(context)
                UiEvent.Permission -> permission?.invoke()
                else -> app.handle(event, origin)
            }
        }
    }
}

@Composable
private fun RouteLifecycle(model: CareViewModel, key: Any, enter: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val token = remember(model, key) { Any() }
    DisposableEffect(model, key, owner) {
        model.claim(token)
        enter()
        val observer = LifecycleEventObserver { _, event ->
            when (event) { Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> model.resume(token); Lifecycle.Event.ON_STOP -> model.release(token); else -> Unit }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); model.release(token) }
    }
}

@Composable
fun MenuRoute(factory: AppViewModelFactory, app: AppViewModel, inactivePlan: Boolean) {
    val model: MenuViewModel = viewModel(factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    val defaultName = stringResource(R.string.kettle)
    RouteEvents(model, Screen.Menu, app)
    RouteLifecycle(model, Screen.Menu) { model.activate() }
    MenuScreen(state, inactivePlan, { model.play(defaultName) }, { model.add(defaultName) }, model::select, model::edit, { model.navigate(Screen.DescalePlan(it)) }, { model.navigate(Screen.History()) }, { model.navigate(Screen.HardnessInfo()) }, { model.navigate(Screen.Settings) }, model::toggleExample, model::retry)
    state.editor?.let { editor -> ApplianceDialog(editor, model::name, model::type, model::save, { if (editor.confirmArchive) model.archiveConfirmation(false) else model.dismissEditor() }, { if (editor.confirmArchive) model.archive() else model.archiveConfirmation(true) }) }
}

@Composable
fun GameRoute(route: Screen.Game, factory: AppViewModelFactory, app: AppViewModel) {
    val model: GameViewModel = viewModel(factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    val label = stringResource(R.string.my_chart)
    RouteEvents(model, route, app)
    RouteLifecycle(model, route) { model.enter(route) }
    BackHandler { model.back() }
    GameScreen(state, model::back, { model.navigate(Screen.HardnessInfo(state.canonical, true)) }, { model.openChart(label) }, model::select, model::printed, model::input, model::step, { model.numericEditor(true) }, model::note, model::date, model::save, model::notSure, model::replay)
    if (state.discard) ConfirmationDialog(stringResource(R.string.discard_title), R.string.discard_body, R.string.discard, R.string.keep_editing, onConfirm = model::discard, onDismiss = model::keep)
    state.chartEditor?.let { ChartProfileDialog(it, model::chartValue, model::chartStep, model::chartSwatch, model::chartConfirmed, model::saveChart, model::closeChart) }
}

@Composable
fun GameOverRoute(route: Screen.GameOver, factory: AppViewModelFactory, app: AppViewModel) {
    val model: GameOverViewModel = viewModel(factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    RouteEvents(model, route, app)
    RouteLifecycle(model, route) { model.enter(route) }
    GameOverScreen(state, model::replay, model::menu, { model.navigate(Screen.DescalePlan(route.applianceId)) }, { model.navigate(Screen.History(route.applianceId)) }, { model.navigate(Screen.HardnessInfo(state.record?.canonical)) }, model::retry)
}

@Composable
fun HardnessRoute(route: Screen.HardnessInfo, factory: AppViewModelFactory, app: AppViewModel) {
    val model: HardnessInfoViewModel = viewModel(factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    RouteEvents(model, route, app)
    RouteLifecycle(model, route) { model.enter(route) }
    HardnessInfoScreen(state, model::back, model::expand)
}

@Composable
fun PlanRoute(route: Screen.DescalePlan, factory: AppViewModelFactory, app: AppViewModel) {
    val model: DescalePlanViewModel = viewModel(factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    RouteEvents(model, route, app)
    RouteLifecycle(model, route) { model.enter(route.applianceId) }
    BackHandler { model.back() }
    DescalePlanScreen(state, model::back, model::interval, model::step, model::anchor, model::due, model::clearDue, model::suggestion, model::reminder, model::save, model::cleaningDialog, model::markDescaled, model::snooze, model::openSettings, model::retryLast)
    if (state.discard) ConfirmationDialog(stringResource(R.string.discard_title), R.string.discard_body, R.string.discard, R.string.keep_editing, onConfirm = model::discard, onDismiss = model::keep)
}

@Composable
fun HistoryRoute(route: Screen.History, factory: AppViewModelFactory, app: AppViewModel) {
    val model: HistoryViewModel = viewModel(factory = factory)
    val menuModel: MenuViewModel = viewModel(factory = factory)
    val defaultName = stringResource(R.string.kettle)
    val menuState by menuModel.state.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    val state by model.state.collectAsStateWithLifecycle()
    RouteEvents(model, route, app)
    RouteLifecycle(model, route) { model.enter(route) }
    RouteEvents(menuModel, route, app)
    RouteLifecycle(menuModel, route) { menuModel.activate() }
    HistoryScreen(state, model::back, model::filter, model::appliance, model::clear, model::detail, model::confirm, model::delete, { if (state.store.data.activeAppliances.isEmpty()) { adding = true; menuModel.add(defaultName, true) } else model.record() }, model::retry)
    if (adding) menuState.editor?.let { editor -> ApplianceDialog(editor, menuModel::name, menuModel::type, menuModel::save, { menuModel.dismissEditor(); adding = false }, { menuModel.archiveConfirmation(true) }) }
}

@Composable
fun SettingsRoute(factory: AppViewModelFactory, app: AppViewModel) {
    val model: SettingsViewModel = viewModel(factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    val label = stringResource(R.string.my_chart)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { model.permissionResult() }
    RouteEvents(model, Screen.Settings, app, permission = { if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else model.openSettings() })
    RouteLifecycle(model, Screen.Settings) { model.activate() }
    SettingsScreen(state, model::back, model::unit, model::reminders, model::haptics, model::examples, { model.openChart(label) }, model::openSettings, model::eraseConfirmation, model::erase, model::retryWrite)
    state.chartEditor?.let { ChartProfileDialog(it, model::chartValue, model::chartStep, model::chartSwatch, model::chartConfirmed, model::saveChart, model::closeChart) }
}
