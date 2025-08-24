package com.mrunicorn.sb.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mrunicorn.sb.R
import com.mrunicorn.sb.App
import com.mrunicorn.sb.data.ItemType
import com.mrunicorn.sb.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class ReminderReceiver : BroadcastReceiver() {
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
                        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
                        val deleteAfter = intent.getBooleanExtra(EXTRA_DELETE_AFTER, false)

                        // Try to load a preview (image item or link thumbnail)
                        val app = context.applicationContext as App
                        val item = withContext(Dispatchers.IO) { app.repo.dao.getItemById(itemId) }
                        val preview: Bitmap? = when {
                            item != null && item.type == ItemType.IMAGE && item.imageUris.isNotEmpty() ->
                                loadBitmapFromUri(context, item.imageUris.first(), 512)
                            item != null && item.type == ItemType.LINK && !item.thumbnailUrl.isNullOrBlank() ->
                                loadBitmapFromUrl(item.thumbnailUrl!!, 512)
                            else -> null
                        }

                        showNotification(context, itemId, title, deleteAfter, preview)
                    }
                    ACTION_DONE -> {
                        val app = context.applicationContext as App
                        app.repo.delete(itemId)
                        NotificationManagerCompat.from(context).cancel(notificationId(itemId))
                        ReminderScheduler.cancel(context, itemId)
                    }
                    ACTION_SNOOZE -> {
                        val inTen = System.currentTimeMillis() + 10 * 60 * 1000L
                        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
                        val deleteAfter = intent.getBooleanExtra(EXTRA_DELETE_AFTER, false)
                        ReminderScheduler.schedule(context, itemId, title, inTen, deleteAfter, null)
                        NotificationManagerCompat.from(context).cancel(notificationId(itemId))
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
        title: String,
        deleteAfter: Boolean,
        preview: Bitmap?
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

        val donePI = PendingIntent.getBroadcast(
            context,
            (itemId.hashCode() * 31) + 1,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_DONE
                putExtra(EXTRA_ITEM_ID, itemId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePI = PendingIntent.getBroadcast(
            context,
            (itemId.hashCode() * 31) + 2,
            Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_ITEM_ID, itemId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DELETE_AFTER, deleteAfter)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sb) // ✅ small monochrome status icon
            .setContentTitle("Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPI)
            .addAction(0, "Done", donePI)
            .addAction(0, "Snooze", snoozePI)

        if (deleteAfter) builder.setSubText("Auto-clean suggested")

        // Use preview as large icon and BigPicture when available
        if (preview != null) {
            builder.setLargeIcon(preview)
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(preview).bigLargeIcon(null as android.graphics.Bitmap?))
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(title))
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
    }
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
    }
}
