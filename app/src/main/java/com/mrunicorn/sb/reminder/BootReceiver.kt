package com.mrunicorn.sb.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules existing reminders after device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Reschedule reminders in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as com.mrunicorn.sb.App
                    val items = app.repo.dao.observeAllOnce() // small helper to get current items snapshot
                    items.filter { it.reminderAt != null && it.reminderAt!! > System.currentTimeMillis() }
                        .forEach { item ->
                            ReminderScheduler.schedule(context, item.id, item.cleanedText?.take(80) ?: item.text?.take(80) ?: "Reminder", item.reminderAt!!, false, item.label)
                        }
                } catch (_: Exception) {
                }
            }
        }
    }
}
