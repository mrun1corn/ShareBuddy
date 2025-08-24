package com.mrunicorn.sb.share

import android.content.Intent
import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.mrunicorn.sb.App
import com.mrunicorn.sb.reminder.ReminderScheduler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.mrunicorn.sb.ui.theme.ShareBuddyTheme
import androidx.compose.foundation.layout.FlowRow
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseShare(intent)
        setContent {
            ShareBuddyTheme {
                Surface {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Share Buddy", style = MaterialTheme.typography.headlineSmall)
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
                            Button(onClick = { onSave() }) { Text("Save") }
                            Button(
                                onClick = { onCleanAndReshare() },
                                enabled = sharedText?.startsWith("http", ignoreCase = true) == true
                            ) { Text("Clean + Re‑share") }
                            OutlinedButton(onClick = { onRemind() }) { Text("Remind") }
                            OutlinedButton(onClick = { onReshare() }) { Text("Re‑share") }
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
                            onConfirm = { hours, deleteAfterReminder ->
                                scheduleReminder(hours, deleteAfterReminder)
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

    private fun onSave() {
        lifecycleScope.launch {
            val currentLabel = labelText.ifBlank { null }
            if (!sharedText.isNullOrBlank()) {
                val savedItem = repo.saveTextOrLink(sharedText!!.trim(), sourcePkg = callingPackage, label = currentLabel)
                lastSavedItemId = savedItem.id
                Toast.makeText(this@ShareBuddyActivity, "Saved", Toast.LENGTH_SHORT).show()
                // Do not finish here, allow user to set reminder
            } else if (sharedImages.isNotEmpty()) {
                val savedItem = repo.saveImages(sharedImages, sourcePkg = callingPackage, label = currentLabel)
                lastSavedItemId = savedItem.id
                Toast.makeText(this@ShareBuddyActivity, "Saved images", Toast.LENGTH_SHORT).show()
                // Do not finish here, allow user to set reminder
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
            repo.saveTextOrLink(cleaned, sourcePkg = callingPackage)
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
            val now = System.currentTimeMillis()
            val whenAt = now + timeInMillis
            val itemLabel = labelText.ifBlank { null }

            if (lastSavedItemId == null) {
                if (!sharedText.isNullOrBlank()) {
                    val savedItem = repo.saveTextOrLink(sharedText!!.trim(), sourcePkg = callingPackage, label = itemLabel)
                    lastSavedItemId = savedItem.id
                } else if (sharedImages.isNotEmpty()) {
                    val savedItem = repo.saveImages(sharedImages, sourcePkg = callingPackage, label = itemLabel)
                    lastSavedItemId = savedItem.id
                } else {
                    Toast.makeText(this@ShareBuddyActivity, "Nothing to save for reminder", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
            }

            repo.setReminder(lastSavedItemId!!, whenAt)
            val title = (sharedText ?: "[${sharedImages.size} image(s)]").take(80)
            ReminderScheduler.schedule(this@ShareBuddyActivity, itemId = lastSavedItemId!!, title = title, whenAt = whenAt, deleteAfterReminder = deleteAfterReminder, label = itemLabel)
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

@Composable
fun ReminderDialog(onDismiss: () -> Unit, onConfirm: (Long, Boolean) -> Unit) {
    var inputValue by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(ReminderUnit.HOURS) }
    var deleteAfterReminder by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a reminder") },
        text = {
            Column {
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { newValue ->
                        inputValue = newValue.filter { it.isDigit() }
                    },
                    label = { Text("Time") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ReminderUnit.values().forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = { selectedUnit = unit },
                            label = { Text(unit.label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteAfterReminder,
                        onCheckedChange = { deleteAfterReminder = it }
                    )
                    Text("Delete after reminder closes")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = inputValue.toLongOrNull()
                    if (value != null && value > 0) {
                        val timeInMillis = when (selectedUnit) {
                            ReminderUnit.MINUTES -> value * 60 * 1000L
                            ReminderUnit.HOURS -> value * 60 * 60 * 1000L
                            ReminderUnit.DAYS -> value * 24 * 60 * 60 * 1000L
                        }
                        onConfirm(timeInMillis, deleteAfterReminder)
                    } else {
                        // Optionally show an error message
                    }
                }
            ) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

enum class ReminderUnit(val label: String) {
    MINUTES("Minutes"),
    HOURS("Hours"),
    DAYS("Days")
}
