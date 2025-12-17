package com.mrunicorn.sb.ui.inbox

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mrunicorn.sb.data.Item
import com.mrunicorn.sb.data.ItemFilter
import com.mrunicorn.sb.data.ItemSort
import com.mrunicorn.sb.data.ItemType
import com.mrunicorn.sb.ui.components.ReminderDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.format.DateUtils

@Composable
fun InboxRoute(
    viewModel: InboxViewModel,
    modifier: Modifier = Modifier,
    initialScrollItemId: String? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current

    var previewImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var labelItem by remember { mutableStateOf<Item?>(null) }
    var deleteItem by remember { mutableStateOf<Item?>(null) }
    var reminderDetailsItem by remember { mutableStateOf<Item?>(null) }
    var reminderTarget by remember { mutableStateOf<Item?>(null) }

    var pendingScrollItemId by rememberSaveable { mutableStateOf(initialScrollItemId) }
    val pinnedItems = remember(state.items) { state.items.filter { it.pinned } }
    val otherItems = remember(state.items) { state.items.filterNot { it.pinned } }
    LaunchedEffect(state.items, pendingScrollItemId) {
        val target = pendingScrollItemId ?: return@LaunchedEffect
        val pinnedIndex = pinnedItems.indexOfFirst { it.id == target }
        if (pinnedIndex >= 0) {
            val headerOffset = 1 // pinned section header
            lazyListState.animateScrollToItem(headerOffset + pinnedIndex)
            pendingScrollItemId = null
            return@LaunchedEffect
        }
        val regularIndex = otherItems.indexOfFirst { it.id == target }
        if (regularIndex >= 0) {
            var offset = 0
            if (pinnedItems.isNotEmpty()) {
                offset += 1 + pinnedItems.size + 1 // header + pinned + spacer
            }
            offset += 1 // recent header
            lazyListState.animateScrollToItem(offset + regularIndex)
            pendingScrollItemId = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is InboxEvent.Toast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    InboxScreen(
        state = state,
        lazyListState = lazyListState,
        previewImageUri = previewImageUri,
        onDismissPreview = { previewImageUri = null },
        onImageSelected = { previewImageUri = it },
        onQueryChange = viewModel::onQueryChange,
        onFilterSelected = viewModel::onFilterSelected,
        onSortSelected = viewModel::onSortSelected,
        onCopy = viewModel::copy,
        onPinToggle = viewModel::togglePin,
        onDeleteRequested = { deleteItem = it },
        onLabelRequested = { labelItem = it },
        onReshare = viewModel::reshare,
        onReminderDetailsRequested = { reminderDetailsItem = it },
        onReminderScheduleRequested = { reminderTarget = it },
        modifier = modifier
    )

    if (labelItem != null) {
        LabelDialog(
            item = labelItem!!,
            onDismiss = { labelItem = null },
            onConfirm = { item, label ->
                viewModel.updateLabel(item, label)
                labelItem = null
            }
        )
    }

    if (deleteItem != null) {
        ConfirmDeleteDialog(
            onDismiss = { deleteItem = null },
            onConfirm = { item ->
                viewModel.delete(item)
                deleteItem = null
            },
            item = deleteItem!!
        )
    }

    if (reminderDetailsItem != null) {
        ReminderDetailsDialog(
            item = reminderDetailsItem!!,
            onDismiss = { reminderDetailsItem = null },
            onCancelReminder = { item ->
                viewModel.cancelReminder(item)
                reminderDetailsItem = null
            }
        )
    }

    if (reminderTarget != null) {
        ReminderDialog(
            onDismiss = { reminderTarget = null },
            onConfirm = { millis, deleteAfter ->
                reminderTarget?.let { viewModel.scheduleReminder(it, millis, deleteAfter) }
                reminderTarget = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    state: InboxUiState,
    lazyListState: LazyListState,
    previewImageUri: Uri?,
    onDismissPreview: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterSelected: (ItemFilter) -> Unit,
    onSortSelected: (ItemSort) -> Unit,
    onCopy: (Item) -> Unit,
    onPinToggle: (Item) -> Unit,
    onDeleteRequested: (Item) -> Unit,
    onLabelRequested: (Item) -> Unit,
    onReshare: (Item) -> Unit,
    onReminderDetailsRequested: (Item) -> Unit,
    onReminderScheduleRequested: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val baseBlur = if (previewImageUri != null) 12.dp else 0.dp
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.weight(1f)) {
                                    if (state.query.isBlank()) {
                                        Text(
                                            "Search",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    BasicTextField(
                                        value = state.query,
                                        onValueChange = onQueryChange,
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                                IconButton(onClick = {
                                    if (state.query.isNotBlank()) {
                                        onQueryChange("")
                                    } else {
                                        showSearch = false
                                    }
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Share Buddy", style = MaterialTheme.typography.titleLarge)
                                val subtitle = buildString {
                                    append("${state.items.size} saved")
                                    if (state.filter != ItemFilter.All) {
                                        append(" • ${state.filter.label()}")
                                    }
                                }
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onQueryChange("")
                        onFilterSelected(ItemFilter.All)
                        onSortSelected(ItemSort.Date)
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset filters")
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Change sort order")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        ItemSort.values().forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.label()) },
                                onClick = {
                                    onSortSelected(sort)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            NavigationBar {
                ItemFilter.values().forEach { filter ->
                    NavigationBarItem(
                        selected = state.filter == filter,
                        onClick = { onFilterSelected(filter) },
                        icon = { Icon(filter.icon(), contentDescription = filter.label()) },
                        label = { Text(filter.label()) }
                    )
                }
            }
        }
    ) { pad ->
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
                Spacer(Modifier.height(12.dp))

                if (state.items.isEmpty() && !state.isLoading) {
                    EmptyInboxState(filter = state.filter, query = state.query)
                } else {
                    val pinnedItems = state.items.filter { it.pinned }
                    val otherItems = state.items.filterNot { it.pinned }
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (pinnedItems.isNotEmpty()) {
                            item {
                                SectionHeader("Pinned", count = pinnedItems.size)
                            }
                            items(pinnedItems, key = { it.id }) { item ->
                                ItemCard(
                                    item = item,
                                    onCopy = { onCopy(item) },
                                    onImageClick = onImageSelected,
                                    onPin = { onPinToggle(item) },
                                    onDelete = { onDeleteRequested(item) },
                                    onLabelClick = onLabelRequested,
                                    onReshare = { onReshare(item) },
                                    onShowReminderDetails = onReminderDetailsRequested,
                                    onAddReminder = onReminderScheduleRequested
                                )
                            }
                            item { Spacer(Modifier.height(12.dp)) }
                        }

                        if (otherItems.isNotEmpty()) {
                            item {
                                SectionHeader("Recent", count = otherItems.size)
                            }
                        }

                        items(otherItems, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                onCopy = { onCopy(item) },
                                onImageClick = onImageSelected,
                                onPin = { onPinToggle(item) },
                                onDelete = { onDeleteRequested(item) },
                                onLabelClick = onLabelRequested,
                                onReshare = { onReshare(item) },
                                onShowReminderDetails = onReminderDetailsRequested,
                                onAddReminder = onReminderScheduleRequested
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = previewImageUri != null,
                enter = fadeIn() + scaleIn(initialScale = 0.98f),
                exit = fadeOut() + scaleOut(targetScale = 0.98f)
            ) {
                ImagePreviewOverlay(
                    uri = previewImageUri,
                    onDismiss = onDismissPreview
                )
            }
        }
    }
}

@Composable
private fun EmptyInboxState(filter: ItemFilter, query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Inbox,
                contentDescription = "Empty inbox icon",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(16.dp))
            val message = when {
                query.isNotBlank() -> "No matches for \"$query\""
                filter != ItemFilter.All -> "No ${filter.label()} items yet"
                else -> "No items yet — Share to Share Buddy from any app."
            }
            Text(message)
        }
    }
}

@Composable
private fun ImagePreviewOverlay(
    uri: Uri?,
    onDismiss: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() }
        )
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "Preview image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCard(
    item: Item,
    onCopy: () -> Unit,
    onImageClick: (Uri) -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onLabelClick: (Item) -> Unit,
    onReshare: (Item) -> Unit,
    onShowReminderDetails: (Item) -> Unit,
    onAddReminder: (Item) -> Unit
) {
    val containerColor =
        if (item.pinned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    val relativeTime = remember(item.createdAt) {
        DateUtils.getRelativeTimeSpanString(
            item.createdAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
    val reminderText = remember(item.reminderAt) {
        val at = item.reminderAt ?: return@remember null
        if (at <= System.currentTimeMillis()) null
        else "Reminds ${DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box {
            Column(Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeBadge(item.type)
                    Spacer(Modifier.weight(1f))
                    Text(
                        relativeTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (item.type == ItemType.IMAGE && item.imageUris.isNotEmpty()) {
                    AsyncImage(
                        model = item.imageUris.first(),
                        contentDescription = "Shared image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clickable { onImageClick(item.imageUris.first()) },
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

                val title = when (item.type) {
                    ItemType.LINK -> item.cleanedText ?: item.text ?: "(link)"
                    ItemType.TEXT -> item.text ?: "(text)"
                    ItemType.IMAGE -> item.label ?: "Image"
                }
                Text(
                    title,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )

                if (!item.label.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    LabelChip(item.label!!)
                }

                Spacer(Modifier.height(8.dp))

                if (reminderText != null) {
                    AssistChip(
                        onClick = { onShowReminderDetails(item) },
                        label = { Text(reminderText) },
                        leadingIcon = { Icon(Icons.Filled.Alarm, contentDescription = null) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                val hasTextToCopy = !((item.cleanedText ?: item.text).isNullOrBlank())
                val hasImageToCopy = item.type == ItemType.IMAGE && item.imageUris.isNotEmpty()
                val canCopy = hasTextToCopy || hasImageToCopy

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onCopy, enabled = canCopy, label = { Text("Copy") })
                    AssistChip(onClick = onPin, label = { Text(if (item.pinned) "Unpin" else "Pin") })
                    AssistChip(onClick = onDelete, label = { Text("Delete") })
                    AssistChip(onClick = { onLabelClick(item) }, label = { Text("Label") })
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { onReshare(item) },
                        label = { Text("Re-share") },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) }
                    )
                    val hasActiveReminder = reminderText != null
                    AssistChip(
                        onClick = {
                            if (hasActiveReminder) onShowReminderDetails(item) else onAddReminder(item)
                        },
                        label = { Text(if (hasActiveReminder) "Reminder details" else "Reminder") },
                        leadingIcon = { Icon(Icons.Filled.Alarm, contentDescription = null) }
                    )
                }
            }
            if (item.pinned) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
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
            TextButton(onClick = { onConfirm(item, labelText.ifBlank { null }) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: (Item) -> Unit, item: Item) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete item?") },
        text = { Text("Are you sure you want to delete this item? This action can't be undone.") },
        confirmButton = {
            TextButton(onClick = { onConfirm(item) }) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReminderDetailsDialog(item: Item, onDismiss: () -> Unit, onCancelReminder: (Item) -> Unit) {
    val remainingTime = remember(item.reminderAt) {
        val reminderAt = item.reminderAt ?: return@remember "No reminder set"
        val diff = reminderAt - System.currentTimeMillis()
        if (diff <= 0) {
            "Reminder overdue"
        } else {
            val minutes = diff / (1000 * 60)
            val hours = minutes / 60
            val days = hours / 24
            when {
                days > 0 -> "${days}d ${hours % 24}h remaining"
                hours > 0 -> "${hours}h ${minutes % 60}m remaining"
                minutes > 0 -> "${minutes}m remaining"
                else -> "Less than a minute remaining"
            }
        }
    }

    val reminderTime = remember(item.reminderAt) {
        item.reminderAt?.let {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it))
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
                reminderTime?.let {
                    Text("Time: $it")
                    Spacer(Modifier.height(8.dp))
                }
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

@Composable
private fun TypeBadge(type: ItemType) {
    val (icon, label) = when (type) {
        ItemType.LINK -> Icons.Filled.Link to "Link"
        ItemType.TEXT -> Icons.Filled.TextSnippet to "Text"
        ItemType.IMAGE -> Icons.Filled.Image to "Image"
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) }
    )
}

@Composable
private fun LabelChip(text: String) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text) },
        leadingIcon = { Icon(Icons.Filled.Label, contentDescription = null) }
    )
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Divider(modifier = Modifier.padding(top = 4.dp))
    }
}

private fun ItemFilter.label(): String = when (this) {
    ItemFilter.All -> "All"
    ItemFilter.Links -> "Links"
    ItemFilter.Text -> "Text"
    ItemFilter.Images -> "Images"
}

private fun ItemFilter.icon(): ImageVector = when (this) {
    ItemFilter.All -> Icons.Filled.Inbox
    ItemFilter.Links -> Icons.Filled.Link
    ItemFilter.Text -> Icons.Filled.TextSnippet
    ItemFilter.Images -> Icons.Filled.Image
}

private fun ItemSort.label(): String = when (this) {
    ItemSort.Date -> "Recent"
    ItemSort.Name -> "A-Z"
    ItemSort.Label -> "Label"
}
