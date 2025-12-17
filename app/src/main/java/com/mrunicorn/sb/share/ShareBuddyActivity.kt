package com.mrunicorn.sb.share

import android.content.Intent
import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.lifecycle.lifecycleScope
import com.mrunicorn.sb.App
import com.mrunicorn.sb.reminder.ReminderScheduler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.mrunicorn.sb.ui.theme.ShareBuddyTheme
import androidx.compose.foundation.layout.FlowRow
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.mrunicorn.sb.ui.components.ReminderDialog
// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Link

@OptIn(ExperimentalLayoutApi::class)
class ShareBuddyActivity : ComponentActivity() {
    private val requestNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showReminderDialog = true
        }
    }
    private val repo by lazy { (application as App).repo }
    private var sharedText: String? = null
    private var sharedImages: List<Uri> = emptyList()
    private var showReminderDialog by mutableStateOf(false)
    private var labelText by mutableStateOf("")
    private var lastSavedItemId: String? = null

    // ✅ Prevent duplicate saves (debounce)
    private var isSaving by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseShare(intent)
        setContent {
            ShareBuddyTheme {
                Surface {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "App icon")
                        Spacer(Modifier.width(8.dp))
                        Text("Share Buddy", style = MaterialTheme.typography.headlineSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (sharedImages.isNotEmpty()) {
                            Column {
                                for (uri in sharedImages) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Shared image",
                                        modifier = Modifier.fillMaxWidth().height(200.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        } else {
                            val preview = sharedText ?: ""
                            Text(preview, maxLines = 6, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = labelText,
                            onValueChange = { labelText = it },
                            label = { Text("Label (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { onSave() }, enabled = !isSaving) {
                                Icon(Icons.Filled.Save, contentDescription = "Save")
                                Spacer(Modifier.width(6.dp))
                                Text("Save")
                            }
                            Button(
                                onClick = { onCleanAndReshare() },
                                enabled = sharedText?.startsWith("http", ignoreCase = true) == true && !isSaving
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = "Clean and re-share")
                                Spacer(Modifier.width(6.dp))
                                Text("Clean + Re-share")
                            }
                            OutlinedButton(onClick = { onRemind() }, enabled = !isSaving) {
                                Icon(Icons.Filled.Alarm, contentDescription = "Remind")
                                Spacer(Modifier.width(6.dp))
                                Text("Remind")
                            }
                            OutlinedButton(onClick = { onReshare() }) {
                                Icon(Icons.Filled.Share, contentDescription = "Re-share")
                                Spacer(Modifier.width(6.dp))
                                Text("Re-share")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tip: You can find saved items in the Share Buddy app.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (showReminderDialog) {
                        ReminderDialog(
                            onDismiss = { showReminderDialog = false },
                            onConfirm = { millis, deleteAfterReminder ->
                                scheduleReminder(millis, deleteAfterReminder)
                                showReminderDialog = false
                            }
                        )
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun parseShare(i: Intent?) {
        when (i?.action) {
            Intent.ACTION_SEND -> {
                val type = i.type ?: ""
                if (type.startsWith("text")) {
                    sharedText = i.getStringExtra(Intent.EXTRA_TEXT)
                } else if (type.startsWith("image")) {
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        i.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    (uri as? Uri)?.let { sharedImages = listOf(it) }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    i.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    i.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) sharedImages = uris
            }
        }
    }

    // ✅ Single source of truth for saving; prevents duplicate items
    private suspend fun ensureSaved(): String? {
        if (lastSavedItemId != null) return lastSavedItemId
        isSaving = true
        return try {
            val currentLabel = labelText.ifBlank { null }
            val savedItem = when {
                !sharedText.isNullOrBlank() ->
                    repo.saveTextOrLink(sharedText!!.trim(), sourcePkg = callingPackage, label = currentLabel)
                sharedImages.isNotEmpty() ->
                    repo.saveImages(sharedImages, sourcePkg = callingPackage, label = currentLabel)
                else -> null
            }
            lastSavedItemId = savedItem?.id
            lastSavedItemId
        } finally {
            isSaving = false
        }
    }

    private fun onSave() {
        if (isSaving) return
        lifecycleScope.launch {
            val id = ensureSaved()
            if (id != null) {
                Toast.makeText(this@ShareBuddyActivity, "Saved", Toast.LENGTH_SHORT).show()
                // Keep screen open so user can set a reminder
            } else {
                Toast.makeText(this@ShareBuddyActivity, "Nothing to save", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun onCleanAndReshare() {
        lifecycleScope.launch {
            val t = sharedText ?: return@launch
            val cleaned = com.mrunicorn.sb.util.LinkCleaner.clean(t.trim())
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, cleaned)
            }
            startActivity(Intent.createChooser(share, "Share cleaned link"))
            finish()
        }
    }

    private fun onRemind() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showReminderDialog = true
        }
    }

    private fun scheduleReminder(timeInMillis: Long, deleteAfterReminder: Boolean) {
        lifecycleScope.launch {
            val id = ensureSaved()
            if (id == null) {
                Toast.makeText(this@ShareBuddyActivity, "Nothing to save for reminder", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val now = System.currentTimeMillis()
            val whenAt = now + timeInMillis
            val title = sharedText?.take(80) ?: "New reminder"
            val itemLabel = labelText.ifBlank { null }

            repo.setReminder(id, whenAt)
            ReminderScheduler.schedule(
                this@ShareBuddyActivity,
                itemId = id,
                title = title,
                whenAt = whenAt,
                deleteAfterReminder = deleteAfterReminder,
                label = itemLabel
            )
            Toast.makeText(this@ShareBuddyActivity, "Reminder set!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun onReshare() {
        if (!sharedText.isNullOrBlank()) {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sharedText!!.trim())
            }
            startActivity(Intent.createChooser(share, "Share"))
            finish()
        } else if (sharedImages.isNotEmpty()) {
            val share = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(sharedImages))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "Share images"))
            finish()
        } else {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
        }
    }
}
