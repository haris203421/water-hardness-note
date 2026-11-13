package com.waterhardness.note.data.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.waterhardness.note.MainActivity
import com.waterhardness.note.R
import com.waterhardness.note.data.local.CarePreferences
import com.waterhardness.note.domain.model.*
import com.waterhardness.note.domain.repository.ReminderGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class LocalReminders(private val context: Context, private val local: CarePreferences) : ReminderGateway {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private fun channel() {
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT))
    }
    override fun permissionAvailable(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        return notifications.areNotificationsEnabled() && notifications.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    override suspend fun refresh() = withContext(Dispatchers.IO) { local.serialized { runCatching { reconcile(local.read()) }; Unit } }
    fun clearAll(previous: CareData) {
        if (Build.VERSION.SDK_INT >= 34) alarms.cancelAll()
        else previous.appliances.forEach { alarms.cancel(alarmIntent(it.id)) }
        notifications.cancelAll()
    }
    private fun alarmIntent(id: String, cycle: String = "") = PendingIntent.getBroadcast(context, 0, Intent(context, CareReminderReceiver::class.java).setAction(ACTION).setData(Uri.parse("waterhardness://reminder/$id")).putExtra("applianceId", id).putExtra("cycle", cycle), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun reconcile(data: CareData, previous: CareData = data) {
        channel()
        (data.appliances.map { it.id } + previous.appliances.map { it.id }).distinct().forEach { id ->
            alarms.cancel(alarmIntent(id))
            val plan = data.plans.find { it.applianceId == id }
            if (plan == null || !data.settings.reminders || !plan.reminderEnabled || data.activeAppliances.none { it.id == id } || previous.plans.find { it.applianceId == id }?.cycle != plan.cycle) notifications.cancel(id, 1)
        }
        if (!data.settings.reminders || !permissionAvailable()) return
        data.plans.filter { it.reminderEnabled && it.lastDelivered != it.cycle && data.activeAppliances.any { a -> a.id == it.applianceId } }.forEach { plan ->
            val eligible = maxOf(plan.dueDate, plan.snooze ?: plan.dueDate)
            val calculated = eligible.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val trigger = if (calculated <= System.currentTimeMillis()) System.currentTimeMillis() + 60000L else calculated
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, alarmIntent(plan.applianceId, plan.cycle))
        }
    }
    fun deliver(id: String, cycle: String) {
        local.serialized {
            val data = local.read()
            val appliance = data.activeAppliances.find { it.id == id } ?: return@serialized
            val plan = data.plans.find { it.applianceId == id } ?: return@serialized
            if (!data.settings.reminders || !plan.reminderEnabled || plan.cycle != cycle || plan.lastDelivered == cycle || !permissionAvailable()) return@serialized
            if (maxOf(plan.dueDate, plan.snooze ?: plan.dueDate) > LocalDate.now()) { reconcile(data); return@serialized }
            channel()
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).setAction("open_plan").setData(Uri.parse("waterhardness://plan/$id")).putExtra("applianceId", id).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val date = plan.dueDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
            val notification = Notification.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(context.getString(R.string.notification_title, appliance.name)).setContentText(context.getString(R.string.notification_due, date)).setContentIntent(open).addAction(Notification.Action.Builder(null, context.getString(R.string.open_plan), open).build()).setAutoCancel(true).build()
            notifications.notify(id, 1, notification)
            local.write(data.copy(plans = data.plans.map { if (it.applianceId == id && it.cycle == cycle) it.copy(lastDelivered = cycle) else it }))
        }
    }
    companion object {
        const val CHANNEL = "appliance_care"
        const val ACTION = "com.waterhardness.note.CARE_REMINDER"
    }
}
