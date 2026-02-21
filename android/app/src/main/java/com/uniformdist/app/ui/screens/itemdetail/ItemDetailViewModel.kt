package com.uniformdist.app.ui.screens.itemdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniformdist.app.data.api.UniformDistApi
import com.uniformdist.app.data.model.DeleteItemImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ItemDetailUiState(
    val itemId: String = "",
    val itemType: String = "",
    val imageUrls: List<String> = emptyList(),
    val wearCount: Int = 0,
    val lastWorn: String? = null,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val deletingIndex: Int? = null,
    val error: String? = null,
    val itemWasDeleted: Boolean = false
)

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: UniformDistApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    private val itemId: String = savedStateHandle.get<String>("itemId") ?: ""

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.getItemImages(itemId = itemId)
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        itemId = itemId,
                        itemType = response.type ?: "",
                        imageUrls = response.image_urls ?: emptyList(),
                        wearCount = response.wear_count ?: 0,
                        lastWorn = response.last_worn,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.error ?: "Failed to load item"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun deleteImage(imageIndex: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeleting = true,
                deletingIndex = imageIndex,
                error = null
            )
            try {
                val response = api.deleteItemImage(
                    request = DeleteItemImageRequest(
                        item_id = itemId,
                        image_index = imageIndex
                    )
                )
                if (response.success) {
                    if (response.item_deleted == true) {
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            deletingIndex = null,
                            itemWasDeleted = true
                        )
                    } else {
                        val updatedUrls = _uiState.value.imageUrls.toMutableList()
                        updatedUrls.removeAt(imageIndex)
                        _uiState.value = _uiState.value.copy(
                            imageUrls = updatedUrls,
                            isDeleting = false,
                            deletingIndex = null
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        deletingIndex = null,
                        error = response.error ?: "Failed to delete image"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    deletingIndex = null,
                    error = "Delete failed: ${e.message}"
                )
            }
        }
    }
}
