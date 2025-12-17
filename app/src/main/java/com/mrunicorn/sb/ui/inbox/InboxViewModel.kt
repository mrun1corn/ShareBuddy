package com.mrunicorn.sb.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mrunicorn.sb.data.Item
import com.mrunicorn.sb.data.ItemFilter
import com.mrunicorn.sb.data.ItemSort
import com.mrunicorn.sb.data.ItemType
import com.mrunicorn.sb.data.Repository
import com.mrunicorn.sb.reminder.ReminderScheduler
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InboxUiState(
    val query: String = "",
    val filter: ItemFilter = ItemFilter.All,
    val sortBy: ItemSort = ItemSort.Date,
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface InboxEvent {
    data class Toast(val message: String) : InboxEvent
}

class InboxViewModel(
    application: Application,
    private val repository: Repository
) : AndroidViewModel(application) {

    private val queryFlow = MutableStateFlow("")
    private val filterFlow = MutableStateFlow(ItemFilter.All)
    private val sortFlow = MutableStateFlow(ItemSort.Date)

    private val _events = MutableSharedFlow<InboxEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    private val resultsFlow = queryFlow
        .debounce(200)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { repository.inbox(it.ifBlank { null }) }

    val state: StateFlow<InboxUiState> = combine(
        resultsFlow,
        queryFlow,
        filterFlow,
        sortFlow
    ) { items, query, filter, sort ->
        InboxUiState(
            query = query,
            filter = filter,
            sortBy = sort,
            items = Repository.sortAndFilter(items, filter, sort),
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        InboxUiState()
    )

    fun onQueryChange(newQuery: String) {
        queryFlow.value = newQuery
    }

    fun onFilterSelected(filter: ItemFilter) {
        filterFlow.value = filter
    }

    fun onSortSelected(sort: ItemSort) {
        sortFlow.value = sort
    }

    fun copy(item: Item) {
        viewModelScope.launch {
            val text = item.cleanedText ?: item.text
            when {
                !text.isNullOrBlank() -> {
                    repository.copyToClipboard(text)
                    _events.emit(InboxEvent.Toast("Copied to clipboard"))
                }
                item.type == ItemType.IMAGE && item.imageUris.isNotEmpty() -> {
                    repository.copyImageToClipboard(item.imageUris.first())
                    _events.emit(InboxEvent.Toast("Image copied to clipboard"))
                }
                else -> _events.emit(InboxEvent.Toast("Nothing to copy"))
            }
        }
    }

    fun togglePin(item: Item) {
        viewModelScope.launch { repository.pin(item.id, !item.pinned) }
    }

    fun delete(item: Item) {
        viewModelScope.launch {
            repository.delete(item.id)
            _events.emit(InboxEvent.Toast("Deleted"))
        }
    }

    fun updateLabel(item: Item, label: String?) {
        viewModelScope.launch {
            repository.updateLabel(item.id, label)
            _events.emit(InboxEvent.Toast("Label updated"))
        }
    }

    fun reshare(item: Item) {
        repository.reshare(item)
    }

    fun cancelReminder(item: Item) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), item.id)
            repository.setReminder(item.id, null)
            _events.emit(InboxEvent.Toast("Reminder cancelled"))
        }
    }

    fun scheduleReminder(item: Item, millisFromNow: Long, deleteAfterReminder: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val whenAt = now + millisFromNow
            val title = item.cleanedText?.take(80) ?: item.text?.take(80) ?: "Reminder"
            repository.setReminder(item.id, whenAt)
            ReminderScheduler.schedule(
                getApplication(),
                itemId = item.id,
                title = title,
                whenAt = whenAt,
                deleteAfterReminder = deleteAfterReminder,
                label = item.label
            )
            _events.emit(InboxEvent.Toast("Reminder set!"))
        }
    }
}

class InboxViewModelFactory(
    private val application: Application,
    private val repository: Repository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InboxViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
