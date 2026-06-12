package com.uniformdist.app.data.cache

import android.content.Context
import com.squareup.moshi.Moshi
import com.uniformdist.app.data.model.ListItemsResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the last-known wardrobe list as JSON so the grid can be rendered
 * instantly on launch (paired with [ImageCache] for the images), before the
 * background sync refreshes it from the backend.
 */
@Singleton
class WardrobeStore @Inject constructor(
    @ApplicationContext context: Context,
    moshi: Moshi,
) {
    private val file = File(context.filesDir, "wardrobe.json")
    private val adapter = moshi.adapter(ListItemsResponse::class.java)

    suspend fun load(): ListItemsResponse? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        runCatching { adapter.fromJson(file.readText()) }.getOrNull()
    }

    suspend fun save(response: ListItemsResponse) = withContext(Dispatchers.IO) {
        runCatching { file.writeText(adapter.toJson(response)) }
    }
}
