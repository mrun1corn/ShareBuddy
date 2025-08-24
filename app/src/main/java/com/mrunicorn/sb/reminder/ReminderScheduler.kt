package com.mrunicorn.sb.reminder


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build


object ReminderScheduler {
fun schedule(
context: Context,
itemId: String,
title: String,
whenAt: Long,
deleteAfterReminder: Boolean,
label: String?
) {
val intent = Intent(context, ReminderReceiver::class.java).apply {
action = ReminderReceiver.ACTION_FIRE
putExtra(ReminderReceiver.EXTRA_ITEM_ID, itemId)
putExtra(ReminderReceiver.EXTRA_TITLE, title)
putExtra(ReminderReceiver.EXTRA_DELETE_AFTER, deleteAfterReminder)
putExtra(ReminderReceiver.EXTRA_LABEL, label)
}
val pi = PendingIntent.getBroadcast(
context,
itemId.hashCode(),
intent,
PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
val am = context.getSystemService(AlarmManager::class.java)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
} else {
am.setExact(AlarmManager.RTC_WAKEUP, whenAt, pi)
}
}


fun cancel(context: Context, itemId: String) {
val intent = Intent(context, ReminderReceiver::class.java).apply { action = ReminderReceiver.ACTION_FIRE }
val pi = PendingIntent.getBroadcast(
context,
itemId.hashCode(),
intent,
PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
val am = context.getSystemService(AlarmManager::class.java)
am.cancel(pi)
}
}