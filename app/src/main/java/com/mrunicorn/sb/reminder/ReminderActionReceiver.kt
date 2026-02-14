package com.mrunicorn.sb.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.mrunicorn.sb.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.util.Log

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra("itemId") ?: return
        val app = context.applicationContext as App
        val repo = app.repo

        when (intent.action) {
            ACTION_COPY -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val item = repo.dao.getItemById(itemId)
                    if (item != null) {
                        val textToCopy = item.cleanedText ?: item.text
                        if (textToCopy != null) {
                            repo.copyToClipboard(textToCopy)
                        }
                    }
                }
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            }
            ACTION_SHARE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val item = repo.dao.getItemById(itemId)
                    if (item != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        if (item.text != null) {
                            shareIntent.type = "text/plain"
                            shareIntent.putExtra(Intent.EXTRA_TEXT, item.cleanedText ?: item.text)
                        } else if (item.imageUris.isNotEmpty()) {
                            shareIntent.type = "image/*"
                            shareIntent.action = Intent.ACTION_SEND_MULTIPLE
                            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(item.imageUris))
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(shareIntent, "Share")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_COPY = "com.mrunicorn.sb.reminder.ACTION_COPY"
        const val ACTION_SHARE = "com.mrunicorn.sb.reminder.ACTION_SHARE"
    }
}
