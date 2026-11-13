package com.waterhardness.note.core.di

import android.content.Context
import com.waterhardness.note.data.local.CarePreferences
import com.waterhardness.note.data.reminder.LocalReminders
import com.waterhardness.note.data.repository.CareRepositoryImpl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

object ServiceLocator {
    private data class Graph(val preferences: CarePreferences, val reminders: LocalReminders, val factory: AppViewModelFactory)
    @Volatile private var graph: Graph? = null
    private val planChannel = Channel<String>(Channel.BUFFERED)
    val planRequests = planChannel.receiveAsFlow()
    val preferences: CarePreferences get() = checkNotNull(graph).preferences
    val reminders: LocalReminders get() = checkNotNull(graph).reminders
    val factory: AppViewModelFactory get() = checkNotNull(graph).factory
    @Synchronized fun initialize(context: Context) {
        if (graph != null) return
        val local = CarePreferences(context.applicationContext)
        val reminders = LocalReminders(context.applicationContext, local)
        graph = Graph(local, reminders, AppViewModelFactory(CareRepositoryImpl(local, reminders), reminders))
    }
    fun openPlan(id: String) { planChannel.trySend(id) }
}
