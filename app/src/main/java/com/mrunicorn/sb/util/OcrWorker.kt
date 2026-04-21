package com.mrunicorn.sb.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import com.mrunicorn.sb.data.Repository
import com.mrunicorn.sb.util.TextExtractor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OcrWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: Repository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()
        val item = repository.dao.getItemById(itemId) ?: return Result.failure()

        // OCR all images
        val allText = item.imageUris.mapNotNull { uri ->
            TextExtractor.extractText(applicationContext, uri)
        }.joinToString("\n\n").trim()

        if (allText.isNotBlank()) {
            val updatedItem = item.copy(text = allText)
            repository.dao.upsert(updatedItem)
        }

        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "item_id"
    }
}
