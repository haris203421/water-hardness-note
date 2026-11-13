package com.waterhardness.note.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.waterhardness.note.core.di.ServiceLocator
import java.util.concurrent.Executors

class CareReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        executor.execute {
            try {
                ServiceLocator.initialize(context.applicationContext)
                val reminders = ServiceLocator.reminders
                if (intent.action == LocalReminders.ACTION) {
                    val id = intent.getStringExtra("applianceId")
                    val cycle = intent.getStringExtra("cycle")
                    if (id != null && cycle != null) reminders.deliver(id, cycle)
                } else ServiceLocator.preferences.serialized { reminders.reconcile(ServiceLocator.preferences.read()) }
            } catch (_: Exception) {
                Unit
            } finally {
                pending.finish()
            }
        }
    }
    companion object {
        private val executor = Executors.newSingleThreadExecutor()
    }
}
