package com.uniformdist.app.ui.screens.itemslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniformdist.app.data.model.ItemListEntry
import com.uniformdist.app.data.repository.OutfitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ItemTypeTab(val backendValue: String, val label: String) {
    SHIRTS("shirt", "Shirts"),
    PANTS("pants", "Pants"),
}

data class ItemsListUiState(
    val shirts: List<ItemListEntry> = emptyList(),
    val pants: List<ItemListEntry> = emptyList(),
    val selectedTab: ItemTypeTab = ItemTypeTab.SHIRTS,
    val isLoading: Boolean = true,
    val error: String? = null,
    val pendingItem: ItemListEntry? = null,
    val isLogging: Boolean = false,
    val justLoggedMessage: String? = null,
)

@HiltViewModel
class ItemsListViewModel @Inject constructor(
    private val repository: OutfitRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemsListUiState())
    val uiState: StateFlow<ItemsListUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = repository.listItems()
                _uiState.value = _uiState.value.copy(
                    shirts = response.shirts,
                    pants = response.pants,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                )
            }
        }
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
                loadItems()
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
