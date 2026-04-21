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
import com.mrunicorn.sb.reminder.ReminderScheduler

/**
 * Reschedules existing reminders after device reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var repo: Repository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val now = System.currentTimeMillis()
                    val items = repo.dao.getPendingReminders(now)
                    items.forEach { item ->
                        val title = item.cleanedText?.take(80) ?: item.text?.take(80) ?: "Reminder"
                        ReminderScheduler.schedule(
                            context,
                            item.id,
                            title,
                            item.reminderAt!!,
                            item.deleteAfterReminder,
                            item.label
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
