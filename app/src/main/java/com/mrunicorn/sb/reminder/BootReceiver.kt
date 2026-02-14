package com.mrunicorn.sb.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.mrunicorn.sb.data.Repository

/**
 * Reschedules existing reminders after device reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var repo: Repository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Reschedule reminders in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as com.mrunicorn.sb.App
                    val items = app.repo.dao.observeAllOnce() // small helper to get current items snapshot
                    items.filter { it.reminderAt != null && it.reminderAt!! > System.currentTimeMillis() }
                        .forEach { item ->
                            val title = item.cleanedText?.take(80) ?: item.text?.take(80) ?: "Reminder"
                            ReminderScheduler.schedule(context, item.id, title, item.reminderAt!!, false, item.label)
                        }
                } catch (_: Exception) {
                }
            }
        }
    }
}
