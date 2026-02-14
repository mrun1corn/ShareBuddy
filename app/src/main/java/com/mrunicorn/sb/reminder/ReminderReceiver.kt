package com.mrunicorn.sb.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mrunicorn.sb.R
import com.mrunicorn.sb.App
import com.mrunicorn.sb.data.ItemType
import com.mrunicorn.sb.data.Repository
import com.mrunicorn.sb.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var repo: Repository

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val action = intent.action
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
                if (itemId == null) {
                    pending.finish(); return@launch
                }

                when (action) {
                    ACTION_FIRE -> {
                        val shortText = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
                        val deleteAfter = intent.getBooleanExtra(EXTRA_DELETE_AFTER, false)
                        val itemLabel = intent.getStringExtra(EXTRA_LABEL)

                        // Try to load a preview (image item or link thumbnail)
                        val item = withContext(Dispatchers.IO) { repo.dao.getItemById(itemId) }
                        val fullText = item?.cleanedText ?: item?.text
                        val preview: Bitmap? = when {
                            item != null && item.type == ItemType.IMAGE && item.imageUris.isNotEmpty() ->
                                loadBitmapFromUri(context, item.imageUris.first(), 512)
                            item != null && item.type == ItemType.LINK && !item.thumbnailUrl.isNullOrBlank() ->
                                loadBitmapFromUrl(item.thumbnailUrl!!, 512)
                            else -> null
                        }

                        showNotification(context, itemId, shortText, fullText, deleteAfter, preview, itemLabel)
                    }
                    ACTION_DONE -> {
                        val app = context.applicationContext as App
                        app.repo.delete(itemId)
                        NotificationManagerCompat.from(context).cancel(notificationId(itemId))
                        ReminderScheduler.cancel(context, itemId)
                    }
                    ACTION_SNOOZE -> {
                        val minutes = DEFAULT_SNOOZE_MINUTES
                        val inTen = System.currentTimeMillis() + minutes * 60 * 1000L
                        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
                        val deleteAfter = intent.getBooleanExtra(EXTRA_DELETE_AFTER, false)
                        val label = intent.getStringExtra(EXTRA_LABEL)

                        // Persist the snoozed time so the app's UI (item card) shows the upcoming reminder
                        val app = context.applicationContext as App
                        withContext(Dispatchers.IO) {
                            try {
                                app.repo.setReminder(itemId, inTen)
                                // Read back to verify persistence
                                // read-back intentionally omitted from release build
                            } catch (_: Exception) {
                                // ignore DB write errors here
                            }
                        }

                        ReminderScheduler.schedule(context, itemId, title, inTen, deleteAfter, label)
                        NotificationManagerCompat.from(context).cancel(notificationId(itemId))

                        // Reliable confirmation: post a small notification and a debug log so we can verify the DB write
                        // snooze confirmed (no logging in release)
                        val time = java.text.SimpleDateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(inTen))
                        val snoozeConfirm = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_stat_sb)
                            .setContentTitle("Reminder snoozed")
                            .setContentText("Will remind at $time")
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setAutoCancel(true)
                            .build()
                        NotificationManagerCompat.from(context).notify(notificationId(itemId) + 1, snoozeConfirm)

                        // Also show a short toast when possible
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Snoozed until $time", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        itemId: String,
        shortText: String,
        fullText: String?,
        deleteAfter: Boolean,
        preview: Bitmap?,
        itemLabel: String?
    ) {
        // Tap content: open inbox and scroll to the item
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openItemId", itemId)
        }
        val contentPI = PendingIntent.getActivity(
            context,
            itemId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sharePI = PendingIntent.getBroadcast(
            context,
            (itemId.hashCode() * 31) + 1,
            Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_SHARE
                putExtra("itemId", itemId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePI = PendingIntent.getBroadcast(
            context,
            (itemId.hashCode() * 31) + 2,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_ITEM_ID, itemId)
                putExtra(EXTRA_TITLE, shortText)
                putExtra(EXTRA_DELETE_AFTER, deleteAfter)
                putExtra(EXTRA_LABEL, itemLabel)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sb) // ✅ small monochrome status icon
            .setContentTitle(itemLabel ?: "Share Buddy")
            .setContentText(shortText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPI)
            .addAction(0, "Share", sharePI)
            .addAction(0, "Snooze ${DEFAULT_SNOOZE_MINUTES}m", snoozePI)

        if (deleteAfter) builder.setSubText("Auto-clean suggested")

        // Use preview as large icon and BigPicture when available
        if (preview != null) {
            builder.setLargeIcon(preview)
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(preview).bigLargeIcon(null as android.graphics.Bitmap?))
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(fullText ?: shortText))
        }

        NotificationManagerCompat.from(context).notify(notificationId(itemId), builder.build())
    }

    private suspend fun loadBitmapFromUri(context: Context, uri: Uri, target: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                    val bmp = BitmapFactory.decodeStream(input, null, options)
                    bmp?.let { scaleDown(it, target) }
                }
            } catch (_: Exception) { null }
        }

    private suspend fun loadBitmapFromUrl(url: String, target: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                URL(url).openStream().use { input ->
                    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                    val bmp = BitmapFactory.decodeStream(input, null, options)
                    bmp?.let { scaleDown(it, target) }
                }
            } catch (_: Exception) { null }
        }

    private fun scaleDown(src: Bitmap, target: Int): Bitmap {
        val w = src.width
        val h = src.height
        val maxDim = maxOf(w, h)
        if (maxDim <= target) return src
        val scale = target.toFloat() / maxDim.toFloat()
        val nw = (w * scale).toInt()
        val nh = (h * scale).toInt()
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }

    private fun notificationId(itemId: String) = 0x10000000 or (itemId.hashCode() and 0x0FFFFFFF)

    companion object {
        const val CHANNEL_ID = "reminders"
        const val ACTION_FIRE = "com.mrunicorn.sb.reminder.ACTION_FIRE"
        const val ACTION_DONE = "com.mrunicorn.sb.reminder.ACTION_DONE"
        const val ACTION_SNOOZE = "com.mrunicorn.sb.reminder.ACTION_SNOOZE"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DELETE_AFTER = "extra_delete_after"
        const val EXTRA_LABEL = "extra_label"
    // Default snooze duration in minutes used for the snooze action label and scheduling
    const val DEFAULT_SNOOZE_MINUTES = 10
    }

}

