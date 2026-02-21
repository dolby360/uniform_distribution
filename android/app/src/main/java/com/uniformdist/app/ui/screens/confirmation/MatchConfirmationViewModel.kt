package com.uniformdist.app.ui.screens.confirmation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.uniformdist.app.data.model.ItemMatchResult
import com.uniformdist.app.data.model.ProcessOutfitResponse
import com.uniformdist.app.data.repository.OutfitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class ConfirmationUiState(
    val matchResults: ProcessOutfitResponse? = null,
    val isLoading: Boolean = false,
    val shirtHandled: Boolean = false,
    val pantsHandled: Boolean = false,
    val error: String? = null
) {
    val isDone: Boolean get() = shirtHandled && pantsHandled
}

@HiltViewModel
class MatchConfirmationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: OutfitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfirmationUiState())
    val uiState: StateFlow<ConfirmationUiState> = _uiState.asStateFlow()

    private var lastAction: (() -> Unit)? = null

    init {
        val resultJson = savedStateHandle.get<String>("resultJson") ?: ""
        val decoded = URLDecoder.decode(resultJson, "UTF-8")
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(ProcessOutfitResponse::class.java)
        val response = adapter.fromJson(decoded)

        _uiState.value = ConfirmationUiState(
            matchResults = response,
            shirtHandled = response?.shirt == null,   // auto-complete if no shirt detected
            pantsHandled = response?.pants == null     // auto-complete if no pants detected
        )
    }

    fun retry() {
        lastAction?.invoke()
    }

    fun confirmMatch(itemType: String, item: ItemMatchResult) {
        lastAction = { confirmMatch(itemType, item) }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.confirmMatch(
                    itemId = item.item_id ?: return@launch,
                    itemType = itemType,
                    originalPhotoUrl = _uiState.value.matchResults?.original_photo_url ?: "",
                    similarityScore = item.similarity,
                    embedding = item.embedding,
                    croppedUrl = item.cropped_url
                )
                markHandled(itemType)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to confirm: ${e.message}"
                )
            }
        }
    }

    fun addNewItem(itemType: String, item: ItemMatchResult) {
        lastAction = { addNewItem(itemType, item) }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.addNewItem(
                    itemType = itemType,
                    croppedImageUrl = item.cropped_url ?: "",
                    embedding = item.embedding ?: emptyList(),
                    originalPhotoUrl = _uiState.value.matchResults?.original_photo_url ?: "",
                    logWear = true
                )
                markHandled(itemType)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to add item: ${e.message}"
                )
            }
        }
    }

    private fun markHandled(itemType: String) {
        _uiState.value = when (itemType) {
            "shirt" -> _uiState.value.copy(isLoading = false, shirtHandled = true)
            "pants" -> _uiState.value.copy(isLoading = false, pantsHandled = true)
            else -> _uiState.value.copy(isLoading = false)
        }
    }
}
