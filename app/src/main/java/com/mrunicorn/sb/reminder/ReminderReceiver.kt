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
import com.mrunicorn.sb.App
import com.mrunicorn.sb.data.ItemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Shared item"
        val itemId = intent.getStringExtra("itemId") ?: ""
        val deleteAfterReminder = intent.getBooleanExtra("deleteAfterReminder", false)
        val label = intent.getStringExtra("label")

        val pendingResult = goAsync()
        val repo = (context.applicationContext as App).repo
        CoroutineScope(Dispatchers.IO).launch {
            try {
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

                val notificationBuilder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notify)
                    .setContentTitle(label ?: "Reminder")
                    .setContentText(title)
                    .setAutoCancel(true)
                    .setContentIntent(pi)

                val item = repo.dao.getItemById(itemId)
                if (item != null && item.type == ItemType.IMAGE && item.imageUris.isNotEmpty()) {
                    try {
                        val imageUri = item.imageUris.first()
                        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageUri))
                        notificationBuilder.setLargeIcon(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                nm.notify(itemId.hashCode(), notificationBuilder.build())

                if (deleteAfterReminder) {
                    repo.delete(itemId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
