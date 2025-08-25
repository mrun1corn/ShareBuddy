package com.mrunicorn.sb.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.mrunicorn.sb.App
import com.mrunicorn.sb.data.Item
import com.mrunicorn.sb.data.ItemFilter
import com.mrunicorn.sb.data.ItemSort
import com.mrunicorn.sb.data.ItemType
import com.mrunicorn.sb.data.Repository
import com.mrunicorn.sb.ui.theme.ShareBuddyTheme
import com.mrunicorn.sb.share.ReminderDialog
import com.mrunicorn.sb.share.ReminderDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Alarm
import java.text.SimpleDateFormat
import java.util.Date

// Main activity for Share Buddy application
class MainActivity : ComponentActivity() {
    private val repo by lazy { (application as App).repo }

    private val requestNotif =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            ShareBuddyTheme {
                var query by remember { mutableStateOf("") }
                var filter by remember { mutableStateOf(ItemFilter.All) }
                var sortBy by remember { mutableStateOf(ItemSort.Date) }
                val items = remember { mutableStateListOf<Item>() }
                val lazyListState = rememberLazyListState()

                var showLabelDialog by remember { mutableStateOf(false) }
                var selectedItemForLabel by remember { mutableStateOf<Item?>(null) }

                // Delete confirmation state
                var showDeleteDialog by remember { mutableStateOf(false) }
                var itemToDelete by remember { mutableStateOf<Item?>(null) }

                // Image preview state
                var previewImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

                // Reminder details state
                var showReminderDetailsDialog by remember { mutableStateOf(false) }
                var selectedItemForReminderDetails by remember { mutableStateOf<Item?>(null) }
                // Add reminder (set later) state
                var showAddReminderDialog by remember { mutableStateOf(false) }
                var selectedItemForAddReminder by remember { mutableStateOf<Item?>(null) }

                LaunchedEffect(query, filter, sortBy) {
                    repo.inbox(query.ifBlank { null }).collectLatest { list ->
                        val processed = withContext(Dispatchers.Default) {
                            Repository.sortAndFilter(list, filter, sortBy)
                        }
                        items.clear()
                        items.addAll(processed)
                        val openItemId = intent.getStringExtra("openItemId")
                        if (openItemId != null) {
                            val index = items.indexOfFirst { it.id == openItemId }
                            if (index != -1) {
                                lazyListState.animateScrollToItem(index)
                            }
                        }
                    }
                }

                // 👇 App bar that hides on scroll down and re-shows on scroll up
                val topAppBarState = rememberTopAppBarState()
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Share Buddy") },
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { pad ->
                    // When preview is open, blur the underlying content a bit
                    val baseBlur = if (previewImageUri != null) 12.dp else 0.dp

                    Box(
                        Modifier
                            .padding(pad)
                            .fillMaxSize()
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .blur(baseBlur)
                        ) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text("Search saved items…") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = filter == ItemFilter.All,
                                    onClick = { filter = ItemFilter.All },
                                    label = { Text("All") }
                                )
                                FilterChip(
                                    selected = filter == ItemFilter.Links,
                                    onClick = { filter = ItemFilter.Links },
                                    label = { Text("Links") }
                                )
                                FilterChip(
                                    selected = filter == ItemFilter.Text,
                                    onClick = { filter = ItemFilter.Text },
                                    label = { Text("Text") }
                                )
                                FilterChip(
                                    selected = filter == ItemFilter.Images,
                                    onClick = { filter = ItemFilter.Images },
                                    label = { Text("Images") }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = sortBy == ItemSort.Date,
                                    onClick = { sortBy = ItemSort.Date },
                                    label = { Text("Date") }
                                )
                                FilterChip(
                                    selected = sortBy == ItemSort.Name,
                                    onClick = { sortBy = ItemSort.Name },
                                    label = { Text("Name") }
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            if (items.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Filled.Inbox,
                                            contentDescription = "Empty inbox icon",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text("No items yet — Share to Share Buddy from any app.")
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = lazyListState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(items, key = { it.id }) { item ->
                                        ItemCard(
                                            item = item,
                                            onCopy = {
                                                val text = item.cleanedText ?: item.text
                                                if (!text.isNullOrBlank()) {
                                                    // Copy text
                                                    repo.copyToClipboard(text)
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Copied to clipboard",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else if (item.type == ItemType.IMAGE && item.imageUris.isNotEmpty()) {
                                                    // Copy image
                                                    repo.copyImageToClipboard(item.imageUris.first())
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Image copied to clipboard",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onImageClick = { uri ->
                                                previewImageUri = uri
                                            },
                                            onPin = {
                                                lifecycleScope.launch { repo.pin(item.id, !item.pinned) }
                                            },
                                            onDelete = {
                                                itemToDelete = item
                                                showDeleteDialog = true
                                            },
                                            onLabelClick = { selectedItem ->
                                                selectedItemForLabel = selectedItem
                                                showLabelDialog = true
                                            },
                                            onReshare = { repo.reshare(it) },
                                            onShowReminderDetails = { itemToShow ->
                                                selectedItemForReminderDetails = itemToShow
                                                showReminderDetailsDialog = true
                                            }
                                            ,onAddReminder = { laterItem ->
                                                selectedItemForAddReminder = laterItem
                                                showAddReminderDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 🔍 Fullscreen preview overlay with animation
                        AnimatedVisibility(
                            visible = previewImageUri != null,
                            enter = fadeIn() + scaleIn(initialScale = 0.98f),
                            exit = fadeOut() + scaleOut(targetScale = 0.98f)
                        ) {
                            Box(
                                Modifier
                                    .padding(pad)
                                    .fillMaxSize()
                            ) {
                                // Scrim (tap to close)
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .clickable { previewImageUri = null }
                                )

                                // Fullscreen image
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = previewImageUri,
                                        contentDescription = "Preview image",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        contentScale = ContentScale.Fit
                                    )

                                    // Close button
                                    IconButton(
                                        onClick = { previewImageUri = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Close preview",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showLabelDialog && selectedItemForLabel != null) {
                    LabelDialog(
                        item = selectedItemForLabel!!,
                        onDismiss = { showLabelDialog = false },
                        onConfirm = { item, label ->
                            lifecycleScope.launch { repo.updateLabel(item.id, label) }
                            showLabelDialog = false
                        }
                    )
                }

                // Delete confirmation dialog
                if (showDeleteDialog && itemToDelete != null) {
                    AlertDialog(
                        onDismissRequest = {
                            showDeleteDialog = false
                            itemToDelete = null
                        },
                        title = { Text("Delete item?") },
                        text = { Text("Are you sure you want to delete this item? This action can't be undone.") },
                        confirmButton = {
                            TextButton(onClick = {
                                val id = itemToDelete!!.id
                                lifecycleScope.launch {
                                    repo.delete(id)
                                    Toast.makeText(this@MainActivity, "Deleted", Toast.LENGTH_SHORT).show()
                                }
                                showDeleteDialog = false
                                itemToDelete = null
                            }) { Text("Delete") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                itemToDelete = null
                            }) { Text("Cancel") }
                        }
                    )
                }

                if (showReminderDetailsDialog && selectedItemForReminderDetails != null) {
                    val itemForDialog = selectedItemForReminderDetails!! // Explicitly get the item
                    ReminderDetailsDialog(
                        item = itemForDialog,
                        onDismiss = { showReminderDetailsDialog = false },
                        onCancelReminder = { itemToCancel ->
                            lifecycleScope.launch {
                                com.mrunicorn.sb.reminder.ReminderScheduler.cancel(this@MainActivity, itemToCancel.id)
                                repo.setReminder(itemToCancel.id, null)
                                Toast.makeText(this@MainActivity, "Reminder cancelled", Toast.LENGTH_SHORT).show()
                                showReminderDetailsDialog = false // Dismiss dialog after cancellation
                            }
                        }
                    )
                }

                // Add reminder dialog (set a reminder later)
                if (showAddReminderDialog && selectedItemForAddReminder != null) {
                    val itemForDialog = selectedItemForAddReminder!!
                    ReminderDialog(
                        onDismiss = {
                            showAddReminderDialog = false
                            selectedItemForAddReminder = null
                        },
                        onConfirm = { millis, deleteAfterReminder ->
                            // Schedule reminder similar to ShareBuddyActivity
                            lifecycleScope.launch {
                                val now = System.currentTimeMillis()
                                val whenAt = now + millis
                                val title = itemForDialog.cleanedText?.take(80) ?: itemForDialog.text?.take(80) ?: "Reminder"
                                repo.setReminder(itemForDialog.id, whenAt)
                                com.mrunicorn.sb.reminder.ReminderScheduler.schedule(
                                    this@MainActivity,
                                    itemId = itemForDialog.id,
                                    title = title,
                                    whenAt = whenAt,
                                    deleteAfterReminder = deleteAfterReminder,
                                    label = itemForDialog.label
                                )
                                Toast.makeText(this@MainActivity, "Reminder set!", Toast.LENGTH_SHORT).show()
                                showAddReminderDialog = false
                                selectedItemForAddReminder = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LabelDialog(item: Item, onDismiss: () -> Unit, onConfirm: (Item, String?) -> Unit) {
    var labelText by remember { mutableStateOf(item.label ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Label") },
        text = {
            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                label = { Text("Label") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(item, labelText.ifBlank { null }) }
            ) { Text("Save") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCard(
    item: Item,
    onCopy: () -> Unit,
    onImageClick: (android.net.Uri) -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onLabelClick: (Item) -> Unit,
    onReshare: (Item) -> Unit,
    onShowReminderDetails: (Item) -> Unit
    ,onAddReminder: (Item) -> Unit
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            // Preview: Images or Link thumbnail
            if (item.type == ItemType.IMAGE && item.imageUris.isNotEmpty()) {
                AsyncImage(
                    model = item.imageUris.first(),
                    contentDescription = "Shared image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { onImageClick(item.imageUris.first()) }, // tap-to-preview
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
            } else if (item.type == ItemType.LINK && !item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = "Link thumbnail",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
            }

            // Title
            val title = when (item.type) {
                ItemType.LINK -> item.cleanedText ?: item.text ?: "(link)"
                ItemType.TEXT -> item.text ?: "(text)"
                ItemType.IMAGE -> ""
            }
            Text(
                title,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )

            if (!item.label.isNullOrBlank()) {
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(8.dp))

            // Enable Copy if there's text OR at least one image
            val hasTextToCopy = !((item.cleanedText ?: item.text).isNullOrBlank())
            val hasImageToCopy = item.type == ItemType.IMAGE && item.imageUris.isNotEmpty()
            val canCopy = hasTextToCopy || hasImageToCopy

            // Row 1: main actions (Copy / Pin / Delete / Label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onCopy, enabled = canCopy, label = { Text("Copy") })
                AssistChip(onClick = onPin, label = { Text(if (item.pinned) "Unpin" else "Pin") })
                AssistChip(onClick = onDelete, label = { Text("Delete") })
                AssistChip(onClick = { onLabelClick(item) }, label = { Text("Label") })
            }
            // Row 2: Re-share on its own line (looks cleaner)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { // Add horizontalArrangement
                AssistChip(onClick = { onReshare(item) }, label = { Text("Re-share") })
                val now = System.currentTimeMillis()
                if (item.reminderAt != null && item.reminderAt > now) {
                    IconButton(onClick = { onShowReminderDetails(item) }) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Reminder set"
                        )
                    }
                } else {
                    AssistChip(onClick = { onAddReminder(item) }, label = { Text("Reminder") })
                }
            }
        }
    }
}

@Composable
fun ReminderDetailsDialog(item: Item, onDismiss: () -> Unit, onCancelReminder: (Item) -> Unit) {
    val remainingTime by remember(item.reminderAt) {
        if (item.reminderAt == null) {
            mutableStateOf("No reminder set")
        } else {
            val diff = item.reminderAt - System.currentTimeMillis()
            if (diff <= 0) {
                mutableStateOf("Reminder overdue")
            } else {
                val minutes = diff / (1000 * 60)
                val hours = minutes / 60
                val days = hours / 24
                when {
                    days > 0 -> mutableStateOf("${days}d ${hours % 24}h remaining")
                    hours > 0 -> mutableStateOf("${hours}h ${minutes % 60}m remaining")
                    minutes > 0 -> mutableStateOf("${minutes}m remaining")
                    else -> mutableStateOf("Less than a minute remaining")
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder Details") },
        text = {
            Column {
                val displayText = item.text ?: item.cleanedText
                if (!displayText.isNullOrBlank()) {
                    Text("Item: $displayText")
                    Spacer(Modifier.height(8.dp))
                }
                Text("Time: ${SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(Date(item.reminderAt ?: 0L))}")
                Spacer(Modifier.height(8.dp))
                Text("Remaining: $remainingTime")
            }
        },
        confirmButton = {
            TextButton(onClick = { onCancelReminder(item) }) { Text("Cancel Reminder") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
