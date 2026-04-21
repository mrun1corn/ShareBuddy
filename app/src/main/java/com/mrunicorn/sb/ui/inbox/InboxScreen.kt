package com.mrunicorn.sb.ui.inbox

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mrunicorn.sb.R
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
    
    LaunchedEffect(state.items, pendingScrollItemId) {
        val target = pendingScrollItemId ?: return@LaunchedEffect
        val index = state.items.indexOfFirst { it.id == target }
        if (index >= 0) {
            // Simple approach: scroll to the item index directly
            // In a real app with headers, we'd need more logic
            lazyListState.animateScrollToItem(index)
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

    if (state.isSelectionMode) {
        androidx.activity.compose.BackHandler {
            viewModel.clearSelection()
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
        onToggleSelection = viewModel::toggleSelection,
        onClearSelection = viewModel::clearSelection,
        onDeleteSelected = viewModel::deleteSelected,
        onPinSelected = viewModel::pinSelected,
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
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onPinSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val baseBlur = if (previewImageUri != null) 12.dp else 0.dp
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) }, // Fallback title
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(id = R.string.close))
                        }
                    },
                    actions = {
                        IconButton(onClick = { onPinSelected(true) }) {
                            Icon(Icons.Filled.PushPin, contentDescription = "Pin")
                        }
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(id = R.string.delete))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        if (showSearch) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(26.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.weight(1f)) {
                                        if (state.query.isBlank()) {
                                            Text(
                                                stringResource(id = R.string.search_hint),
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
                                        Icon(Icons.Filled.Close, contentDescription = null)
                                    }
                                }
                            }
                        } else {
                            Column {
                                Text(stringResource(id = R.string.app_name), style = MaterialTheme.typography.titleLarge)
                                val subtitle = stringResource(id = R.string.saved_count, state.items.size)
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            onQueryChange("")
                            onFilterSelected(ItemFilter.All)
                            onSortSelected(ItemSort.Date)
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(id = R.string.reset_filters))
                        }
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(id = R.string.search_hint))
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = stringResource(id = R.string.change_sort_order))
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            ItemSort.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.name) },
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
            }
        },
        bottomBar = {
            if (!state.isSelectionMode) {
                NavigationBar {
                    ItemFilter.values().forEach { filter ->
                        NavigationBarItem(
                            selected = state.filter == filter,
                            onClick = { onFilterSelected(filter) },
                            icon = { Icon(filter.icon(), contentDescription = filter.name) },
                            label = { Text(filter.name) }
                        )
                    }
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
                    .padding(horizontal = 16.dp)
                    .blur(baseBlur)
            ) {
                if (state.items.isEmpty() && !state.isLoading) {
                    EmptyInboxState(filter = state.filter, query = state.query)
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                onCopy = { onCopy(item) },
                                onImageClick = onImageSelected,
                                onPin = { onPinToggle(item) },
                                onDelete = { onDeleteRequested(item) },
                                onLabelClick = onLabelRequested,
                                onReshare = { onReshare(item) },
                                onShowReminderDetails = onReminderDetailsRequested,
                                onAddReminder = onReminderScheduleRequested,
                                isSelected = state.selectedIds.contains(item.id),
                                isSelectionMode = state.isSelectionMode,
                                onToggleSelection = { onToggleSelection(item.id) }
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
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(16.dp))
            val message = when {
                query.isNotBlank() -> stringResource(id = R.string.no_matches, query)
                else -> stringResource(id = R.string.no_items_yet)
            }
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ImagePreviewOverlay(
    uri: Uri?,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = state),
            contentScale = ContentScale.Fit
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(id = R.string.close),
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
    onAddReminder: (Item) -> Unit,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        item.pinned -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
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
            .padding(vertical = 6.dp)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelection()
                },
                onLongClick = { onToggleSelection() }
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            Column(Modifier.padding(16.dp)) {
                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isSelected, onCheckedChange = { onToggleSelection() })
                        Spacer(Modifier.width(8.dp))
                        Text("Selected", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.label?.let { label ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = label.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
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
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable { onImageClick(item.imageUris.first()) }
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                } else if (item.type == ItemType.LINK && !item.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                }

                val title = item.cleanedText ?: item.text ?: ""
                val context = LocalContext.current
                
                if (item.type == ItemType.LINK && title.isNotBlank()) {
                    Text(
                        title,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(title)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    )
                } else if (item.type == ItemType.IMAGE && title.isBlank()) {
                   Row(verticalAlignment = Alignment.CenterVertically) {
                       CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                       Spacer(Modifier.width(8.dp))
                       Text("Processing text...", style = MaterialTheme.typography.bodySmall)
                   }
                } else {
                    SelectionContainer {
                        Text(
                            title,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (reminderText != null) {
                    AssistChip(
                        onClick = { onShowReminderDetails(item) },
                        label = { Text(reminderText) },
                        leadingIcon = { Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onCopy) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(id = R.string.save))
                        }
                        IconButton(onClick = onPin) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = null,
                                tint = if (item.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(id = R.string.delete))
                        }
                        IconButton(onClick = { onLabelClick(item) }) {
                            Icon(Icons.Filled.Label, contentDescription = null)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { onReshare(item) }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(id = R.string.reshare))
                        }
                        IconButton(onClick = { 
                            if (reminderText != null) onShowReminderDetails(item) else onAddReminder(item)
                        }) {
                            Icon(Icons.Filled.Alarm, contentDescription = stringResource(id = R.string.remind))
                        }
                    }
                }
            }
            if (item.pinned) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
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
        title = { Text(stringResource(id = R.string.edit_label)) },
        text = {
            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                label = { Text(stringResource(id = R.string.label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(item, labelText.ifBlank { null }) }) { Text(stringResource(id = R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(onDismiss: () -> Unit, onConfirm: (Item) -> Unit, item: Item) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.delete_item_q)) },
        text = { Text(stringResource(id = R.string.delete_confirm_msg)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(item) }) { Text(stringResource(id = R.string.delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) }
        }
    )
}

@Composable
fun ReminderDetailsDialog(item: Item, onDismiss: () -> Unit, onCancelReminder: (Item) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.reminder_details)) },
        text = {
            Column {
                val displayText = item.text ?: item.cleanedText
                if (!displayText.isNullOrBlank()) {
                    Text(displayText, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                }
                item.reminderAt?.let {
                    val time = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(it))
                    Text("Time: $time")
                    if (item.deleteAfterReminder) {
                        Text("Action: Delete after reminder", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCancelReminder(item) }) { Text(stringResource(id = R.string.cancel_reminder)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.close)) }
        }
    )
}

private fun ItemFilter.icon(): ImageVector = when (this) {
    ItemFilter.All -> Icons.Filled.Inbox
    ItemFilter.Links -> Icons.Filled.Link
    ItemFilter.Text -> Icons.Filled.Description
    ItemFilter.Images -> Icons.Filled.Image
}
