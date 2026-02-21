package com.uniformdist.app.ui.screens.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniformdist.app.data.model.ProcessOutfitResponse
import com.uniformdist.app.data.repository.OutfitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

enum class CropStep { SHIRT, PANTS }

data class ManualCropUiState(
    val currentStep: CropStep = CropStep.SHIRT,
    val shirtCropRect: Rect? = null,
    val pantsCropRect: Rect? = null,
    val isProcessing: Boolean = false,
    val result: ProcessOutfitResponse? = null,
    val error: String? = null
)

@HiltViewModel
class ManualCropViewModel @Inject constructor(
    private val repository: OutfitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualCropUiState())
    val uiState: StateFlow<ManualCropUiState> = _uiState.asStateFlow()

    fun confirmShirtCrop(rect: Rect) {
        _uiState.value = _uiState.value.copy(
            shirtCropRect = rect,
            currentStep = CropStep.PANTS
        )
    }

    fun skipShirt() {
        _uiState.value = _uiState.value.copy(
            shirtCropRect = null,
            currentStep = CropStep.PANTS
        )
    }

    fun confirmPantsCrop(rect: Rect) {
        _uiState.value = _uiState.value.copy(pantsCropRect = rect)
    }

    fun skipPants() {
        _uiState.value = _uiState.value.copy(pantsCropRect = null)
    }

    fun submitCrops(imageBytes: ByteArray, imageWidth: Int, imageHeight: Int) {
        val state = _uiState.value
        if (state.shirtCropRect == null && state.pantsCropRect == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
            try {
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val originalBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                val shirtBase64 = state.shirtCropRect?.let { rect ->
                    cropAndEncode(bitmap, rect, imageWidth, imageHeight)
                }
                val pantsBase64 = state.pantsCropRect?.let { rect ->
                    cropAndEncode(bitmap, rect, imageWidth, imageHeight)
                }

                bitmap.recycle()

                val response = repository.processManualCrop(
                    originalBase64, shirtBase64, pantsBase64
                )

                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        result = response
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = response.error ?: "Unknown error from server"
                    )
                }
            } catch (e: SocketTimeoutException) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Request timed out. Please try again."
                )
            } catch (e: UnknownHostException) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "No internet connection."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Failed to process: ${e.message}"
                )
            }
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = null, error = null)
    }

    /**
     * Crop the bitmap using normalized coordinates from the overlay
     * and return as base64-encoded JPEG.
     *
     * @param bitmap The full source bitmap
     * @param rect Crop rectangle in display coordinates
     * @param displayWidth Width of the display area the rect was drawn on
     * @param displayHeight Height of the display area the rect was drawn on
     */
    private fun cropAndEncode(
        bitmap: Bitmap, rect: Rect,
        displayWidth: Int, displayHeight: Int
    ): String {
        val scaleX = bitmap.width.toFloat() / displayWidth
        val scaleY = bitmap.height.toFloat() / displayHeight

        val x = (rect.left * scaleX).toInt().coerceIn(0, bitmap.width - 1)
        val y = (rect.top * scaleY).toInt().coerceIn(0, bitmap.height - 1)
        val w = (rect.width * scaleX).toInt().coerceIn(1, bitmap.width - x)
        val h = (rect.height * scaleY).toInt().coerceIn(1, bitmap.height - y)

        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
        val stream = ByteArrayOutputStream()
        cropped.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        cropped.recycle()
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
