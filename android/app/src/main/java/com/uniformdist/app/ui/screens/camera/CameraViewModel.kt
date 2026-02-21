package com.uniformdist.app.ui.screens.camera

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniformdist.app.data.model.ProcessOutfitResponse
import com.uniformdist.app.data.repository.OutfitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

data class CameraUiState(
    val isProcessing: Boolean = false,
    val result: ProcessOutfitResponse? = null,
    val error: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: OutfitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun processOutfit(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = CameraUiState(isProcessing = true)
            try {
                val imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                val response = repository.processOutfit(imageBase64)
                if (response.success) {
                    _uiState.value = CameraUiState(result = response)
                } else {
                    _uiState.value = CameraUiState(error = response.error ?: "Unknown error from server")
                }
            } catch (e: SocketTimeoutException) {
                _uiState.value = CameraUiState(error = "Request timed out. Please try again.")
            } catch (e: UnknownHostException) {
                _uiState.value = CameraUiState(error = "No internet connection.")
            } catch (e: Exception) {
                _uiState.value = CameraUiState(error = "Failed to process outfit: ${e.message}")
            }
        }
    }

    fun clearResult() {
        _uiState.value = CameraUiState()
    }
}
