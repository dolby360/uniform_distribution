package com.uniformdist.app.ui.screens.itemslist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.uniformdist.app.data.cache.WardrobeStore
import com.uniformdist.app.data.model.ItemListEntry
import com.uniformdist.app.data.model.ListItemsResponse
import com.uniformdist.app.data.repository.OutfitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ItemTypeTab(val backendValue: String, val label: String) {
    SHIRTS("shirt", "Shirts"),
    PANTS("pants", "Pants"),
}

/** Background-sync state, surfaced as a small spinner / checkmark in the UI. */
enum class SyncStatus { SYNCING, SYNCED, FAILED }

data class ItemsListUiState(
    val shirts: List<ItemListEntry> = emptyList(),
    val pants: List<ItemListEntry> = emptyList(),
    val selectedTab: ItemTypeTab = ItemTypeTab.SHIRTS,
    val isLoading: Boolean = true,
    val error: String? = null,
    val pendingItem: ItemListEntry? = null,
    val isLogging: Boolean = false,
    val justLoggedMessage: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCING,
)

@HiltViewModel
class ItemsListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: OutfitRepository,
    private val wardrobeStore: WardrobeStore,
    private val imageLoader: ImageLoader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemsListUiState())
    val uiState: StateFlow<ItemsListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 1) Show whatever we cached last time, instantly (images come from
            //    Coil's disk cache, keyed by hash — no network needed).
            wardrobeStore.load()?.let { cached -> applyItems(cached) }
            // 2) Refresh from the backend in the background.
            sync()
        }
    }

    /** Re-run the background sync (also used by the Retry button). */
    fun loadItems() {
        viewModelScope.launch { sync() }
    }

    /**
     * Fetch the latest list, render it, persist it for instant next-launch, and
     * prewarm the image cache so every thumbnail (both tabs) is on disk by the
     * time the checkmark shows. Images are cached by [ItemListEntry.image_hash],
     * so unchanged ones are never re-downloaded.
     */
    private suspend fun sync() {
        _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.SYNCING, error = null)
        try {
            val fresh = repository.listItems()
            applyItems(fresh)
            wardrobeStore.save(fresh)
            prewarmImages(fresh.shirts + fresh.pants)
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.SYNCED)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                syncStatus = SyncStatus.FAILED,
                error = e.message,
            )
        }
    }

    private fun prewarmImages(items: List<ItemListEntry>) {
        items.forEach { item ->
            imageLoader.enqueue(buildImageRequest(item))
        }
    }

    /**
     * Coil request for an item's thumbnail: fetched from the (rotating) signed
     * URL but cached under the stable content hash, so later loads hit the disk
     * cache instead of the network.
     */
    fun buildImageRequest(item: ItemListEntry): ImageRequest {
        val builder = ImageRequest.Builder(context).data(item.image_url)
        item.image_hash?.takeIf { it.isNotBlank() }?.let { hash ->
            builder.memoryCacheKey(hash)
                .diskCacheKey(hash)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
        }
        return builder.build()
    }

    private fun applyItems(response: ListItemsResponse) {
        _uiState.value = _uiState.value.copy(
            shirts = response.shirts.sortedBy { it.wear_count },
            pants = response.pants.sortedBy { it.wear_count },
            isLoading = false,
        )
    }

    fun selectTab(tab: ItemTypeTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun requestMarkWorn(item: ItemListEntry) {
        _uiState.value = _uiState.value.copy(pendingItem = item)
    }

    fun cancelMarkWorn() {
        _uiState.value = _uiState.value.copy(pendingItem = null)
    }

    fun dismissJustLoggedMessage() {
        _uiState.value = _uiState.value.copy(justLoggedMessage = null)
    }

    /**
     * Log a wear for the pending item. wornAtIso = null means "now"; otherwise
     * an ISO-8601 timestamp the backend will use instead of server time.
     */
    fun confirmMarkWorn(wornAtIso: String?) {
        val item = _uiState.value.pendingItem ?: return
        val itemType = _uiState.value.selectedTab.backendValue
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLogging = true, error = null)
            try {
                val response = repository.confirmMatch(
                    itemId = item.id,
                    itemType = itemType,
                    originalPhotoUrl = "",
                    wornAt = wornAtIso,
                )
                _uiState.value = _uiState.value.copy(
                    isLogging = false,
                    pendingItem = null,
                    justLoggedMessage = "Logged! Worn ${response.wear_count} times",
                )
                sync()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLogging = false,
                    pendingItem = null,
                    error = "Failed to log wear: ${e.message}",
                )
            }
        }
    }
}
