package com.mrunicorn.sb.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrunicorn.sb.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    val repository: Repository
) : ViewModel() {

    var sharedText by mutableStateOf<String?>(null)
        private set
    var sharedImages by mutableStateOf<List<Uri>>(emptyList())
        private set
    var labelText by mutableStateOf("")
    var isSaving by mutableStateOf(false)
        private set
    var lastSavedItemId by mutableStateOf<String?>(null)
        private set

    suspend fun setReminder(id: String, whenAt: Long, deleteAfter: Boolean) {
        repository.setReminder(id, whenAt, deleteAfter)
    }

    fun parseIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val type = intent.type ?: ""
                if (type.startsWith("text")) {
                    sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                } else if (type.startsWith("image")) {
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    uri?.let { sharedImages = listOf(it) }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) sharedImages = uris
            }
        }
    }

    suspend fun save(callingPackage: String?): String? {
        if (lastSavedItemId != null) return lastSavedItemId
        isSaving = true
        return try {
            val currentLabel = labelText.ifBlank { null }
            val savedItem = when {
                !sharedText.isNullOrBlank() ->
                    repository.saveTextOrLink(sharedText!!.trim(), sourcePkg = callingPackage, label = currentLabel)
                sharedImages.isNotEmpty() ->
                    repository.saveImages(sharedImages, sourcePkg = callingPackage, label = currentLabel)
                else -> null
            }
            lastSavedItemId = savedItem?.id
            lastSavedItemId
        } finally {
            isSaving = false
        }
    }
}
