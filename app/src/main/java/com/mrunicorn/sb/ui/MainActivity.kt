package com.mrunicorn.sb.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.mrunicorn.sb.App
import com.mrunicorn.sb.data.Item
import com.mrunicorn.sb.data.ItemType
import com.mrunicorn.sb.ui.theme.ShareBuddyTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repo by lazy { (application as App).repo }

    private val requestNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            ShareBuddyTheme {
                var query by remember { mutableStateOf("") }
                var filter by remember { mutableStateOf<Filter>(Filter.All) }
                val items = remember { mutableStateListOf<Item>() }
                val lazyListState = rememberLazyListState()

                LaunchedEffect(query, filter) {
                    repo.inbox(query.ifBlank { null }).collectLatest { list ->
                        items.clear()
                        items.addAll(
                            when (filter) {
                                Filter.All -> list
                                Filter.Links -> list.filter { it.type == ItemType.LINK }
                                Filter.Text -> list.filter { it.type == ItemType.TEXT }
                                Filter.Images -> list.filter { it.type == ItemType.IMAGE }
                            }
                        )
                        val openItemId = intent.getStringExtra("openItemId")
                        if (openItemId != null) {
                            val index = items.indexOfFirst { it.id == openItemId }
                            if (index != -1) {
                                lazyListState.animateScrollToItem(index)
                            }
                        }
                    }
                }


                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(title = { Text("Share Buddy") })
                    }
                ) { pad ->
                    Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search saved items…") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = filter == Filter.All, onClick = { filter = Filter.All }, label = { Text("All") })
                            FilterChip(selected = filter == Filter.Links, onClick = { filter = Filter.Links }, label = { Text("Links") })
                            FilterChip(selected = filter == Filter.Text, onClick = { filter = Filter.Text }, label = { Text("Text") })
                            FilterChip(selected = filter == Filter.Images, onClick = { filter = Filter.Images }, label = { Text("Images") })
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
                                            if (!text.isNullOrBlank()) repo.copyToClipboard(text)
                                        },
                                        onPin = { lifecycleScope.launch { repo.pin(item.id, !item.pinned) } },
                                        onDelete = { lifecycleScope.launch { repo.delete(item.id) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class Filter { All, Links, Text, Images }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCard(item: Item, onCopy: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            if (item.type == ItemType.IMAGE && item.imageUris.isNotEmpty()) {
                AsyncImage(
                    model = item.imageUris.first(),
                    contentDescription = "Shared image",
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            val title = when (item.type) {
                ItemType.LINK -> item.cleanedText ?: item.text ?: "(link)"
                ItemType.TEXT -> item.text ?: "(text)"
                ItemType.IMAGE -> "[${item.imageUris.size} image(s)]"
            }
            Text(title, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onCopy, label = { Text("Copy") })
                AssistChip(onClick = onPin, label = { Text(if (item.pinned) "Unpin" else "Pin") })
                AssistChip(onClick = onDelete, label = { Text("Delete") })
            }
        }
    }
}
