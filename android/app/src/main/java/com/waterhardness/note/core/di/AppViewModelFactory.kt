package com.waterhardness.note.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.waterhardness.note.domain.repository.*
import com.waterhardness.note.presentation.app.AppViewModel
import com.waterhardness.note.presentation.game.GameViewModel
import com.waterhardness.note.presentation.gameover.GameOverViewModel
import com.waterhardness.note.presentation.hardness.HardnessInfoViewModel
import com.waterhardness.note.presentation.history.HistoryViewModel
import com.waterhardness.note.presentation.menu.MenuViewModel
import com.waterhardness.note.presentation.plan.DescalePlanViewModel
import com.waterhardness.note.presentation.settings.SettingsViewModel
import com.waterhardness.note.presentation.splash.SplashViewModel

class AppViewModelFactory(private val repository: CareRepository, private val reminders: ReminderGateway) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        AppViewModel::class.java -> AppViewModel(repository)
        SplashViewModel::class.java -> SplashViewModel(repository)
        MenuViewModel::class.java -> MenuViewModel(repository)
        GameViewModel::class.java -> GameViewModel(repository)
        GameOverViewModel::class.java -> GameOverViewModel(repository)
        HardnessInfoViewModel::class.java -> HardnessInfoViewModel(repository)
        DescalePlanViewModel::class.java -> DescalePlanViewModel(repository, reminders)
        HistoryViewModel::class.java -> HistoryViewModel(repository)
        SettingsViewModel::class.java -> SettingsViewModel(repository, reminders)
        else -> throw IllegalArgumentException(modelClass.name)
    } as T
}
