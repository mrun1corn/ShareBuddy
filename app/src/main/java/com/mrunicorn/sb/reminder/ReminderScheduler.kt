package com.mrunicorn.sb.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    fun schedule(context: Context, itemId: String, title: String, whenAt: Long, deleteAfterReminder: Boolean, label: String?) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("itemId", itemId)
            putExtra("title", title)
            putExtra("deleteAfterReminder", deleteAfterReminder)
            putExtra("label", label)
        }
        val pi = PendingIntent.getBroadcast(
            context, itemId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, whenAt, pi)
        }
    }
}
