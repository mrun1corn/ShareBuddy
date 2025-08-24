package com.mrunicorn.sb.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mrunicorn.sb.R
import com.mrunicorn.sb.ui.MainActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra("itemId") ?: return
        val text = intent.getStringExtra("title")

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openItemId", itemId)
        }
        val pending = PendingIntent.getActivity(context, itemId.hashCode(), tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val copyIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_COPY
            putExtra("itemId", itemId)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(context, itemId.hashCode(), copyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val shareIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SHARE
            putExtra("itemId", itemId)
        }
        val sharePendingIntent = PendingIntent.getBroadcast(context, itemId.hashCode(), shareIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Reminder")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "Copy", copyPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Share", sharePendingIntent)
            .build()
        nm.notify(itemId.hashCode(), notif)
    }

    companion object {
        private const val CHANNEL_ID = "reminders"
    }
}
