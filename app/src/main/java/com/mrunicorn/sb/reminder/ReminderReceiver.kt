package com.mrunicorn.sb.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mrunicorn.sb.ui.MainActivity
import com.mrunicorn.sb.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Shared item"
        val itemId = intent.getStringExtra("itemId") ?: ""

        val channelId = "reminders"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openItemId", itemId)
        }
        val pi = PendingIntent.getActivity(context, itemId.hashCode(), open, PendingIntent.FLAG_IMMUTABLE)

        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("Reminder")
            .setContentText(title)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        nm.notify(itemId.hashCode(), n)
    }
}
