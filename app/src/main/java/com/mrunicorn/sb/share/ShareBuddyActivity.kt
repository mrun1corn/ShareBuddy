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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.lifecycle.lifecycleScope
import androidx.activity.viewModels
import com.mrunicorn.sb.reminder.ReminderScheduler
import com.mrunicorn.sb.ui.theme.ShareBuddyTheme
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.mrunicorn.sb.ui.components.ReminderDialog
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Link
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@AndroidEntryPoint
class ShareBuddyActivity : ComponentActivity() {
    private val viewModel: ShareViewModel by viewModels()

    private val requestNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showReminderDialog = true
        }
    }
    
    private var showReminderDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.parseIntent(intent)

        setContent {
            ShareBuddyTheme {
                var showSheet by remember { mutableStateOf(true) }
                val sheetState = rememberModalBottomSheetState()

                if (showSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { 
                            showSheet = false
                            finish() 
                        },
                        sheetState = sheetState,
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        ShareSheetContent(
                            viewModel = viewModel,
                            onSave = { onSave() },
                            onCleanAndReshare = { onCleanAndReshare() },
                            onRemind = { onRemind() },
                            onReshare = { onReshare() }
                        )
                    }
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

    @Composable
    fun ShareSheetContent(
        viewModel: ShareViewModel,
        onSave: () -> Unit,
        onCleanAndReshare: () -> Unit,
        onRemind: () -> Unit,
        onReshare: () -> Unit
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Share Buddy", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(Modifier.height(16.dp))

            if (viewModel.sharedImages.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (uri in viewModel.sharedImages) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Shared image",
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                }
            } else {
                val preview = viewModel.sharedText ?: ""
                Text(
                    preview, 
                    maxLines = 4, 
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = viewModel.labelText,
                onValueChange = { viewModel.labelText = it },
                label = { Text("Label (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.height(24.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onSave, enabled = !viewModel.isSaving) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
                Button(
                    onClick = onCleanAndReshare,
                    enabled = viewModel.sharedText?.startsWith("http", ignoreCase = true) == true && !viewModel.isSaving
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clean + Re-share")
                }
                OutlinedButton(onClick = onRemind, enabled = !viewModel.isSaving) {
                    Icon(Icons.Filled.Alarm, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Remind")
                }
                OutlinedButton(onClick = onReshare) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Re-share")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Tip: You can find saved items in the Share Buddy app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    private fun getSafeCallingPackage(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                this.callingPackage
            } else {
                @Suppress("DEPRECATION")
                this.callingPackage
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun onSave() {
        lifecycleScope.launch {
            val id = viewModel.save(getSafeCallingPackage())
            if (id != null) {
                Toast.makeText(this@ShareBuddyActivity, "Saved", Toast.LENGTH_SHORT).show()
                // Auto-finish after short delay? Or let user stay to set reminder?
            } else {
                Toast.makeText(this@ShareBuddyActivity, "Nothing to save", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun onCleanAndReshare() {
        lifecycleScope.launch {
            val t = viewModel.sharedText ?: return@launch
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
            val id = viewModel.save(getSafeCallingPackage())
            if (id == null) {
                Toast.makeText(this@ShareBuddyActivity, "Nothing to save for reminder", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val now = System.currentTimeMillis()
            val whenAt = now + timeInMillis
            val title = viewModel.sharedText?.take(80) ?: "New reminder"

            viewModel.setReminder(id, whenAt, deleteAfterReminder)

            ReminderScheduler.schedule(
                this@ShareBuddyActivity,
                itemId = id,
                title = title,
                whenAt = whenAt,
                deleteAfterReminder = deleteAfterReminder,
                label = viewModel.labelText.ifBlank { null }
            )
            Toast.makeText(this@ShareBuddyActivity, "Reminder set!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun onReshare() {
        if (!viewModel.sharedText.isNullOrBlank()) {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, viewModel.sharedText!!.trim())
            }
            startActivity(Intent.createChooser(share, "Share"))
            finish()
        } else if (viewModel.sharedImages.isNotEmpty()) {
            val share = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(viewModel.sharedImages))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "Share images"))
            finish()
        } else {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
        }
    }
}
