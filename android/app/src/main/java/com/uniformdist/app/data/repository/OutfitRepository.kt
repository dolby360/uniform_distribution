package com.uniformdist.app.data.repository

import com.uniformdist.app.data.api.UniformDistApi
import com.uniformdist.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutfitRepository @Inject constructor(
    private val api: UniformDistApi
) {
    suspend fun processOutfit(imageBase64: String): ProcessOutfitResponse {
        return api.processOutfit(request = ProcessOutfitRequest(image = imageBase64))
    }

    suspend fun confirmMatch(
        itemId: String,
        itemType: String,
        originalPhotoUrl: String,
        similarityScore: Double? = null,
        embedding: List<Double>? = null,
        croppedUrl: String? = null
    ): ConfirmMatchResponse {
        return api.confirmMatch(
            request = ConfirmMatchRequest(
                item_id = itemId,
                item_type = itemType,
                original_photo_url = originalPhotoUrl,
                similarity_score = similarityScore,
                embedding = embedding,
                cropped_url = croppedUrl
            )
        )
    }

    suspend fun addNewItem(
        itemType: String,
        croppedImageUrl: String,
        embedding: List<Double>,
        originalPhotoUrl: String,
        logWear: Boolean
    ): AddNewItemResponse {
        return api.addNewItem(
            request = AddNewItemRequest(
                item_type = itemType,
                cropped_image_url = croppedImageUrl,
                embedding = embedding,
                original_photo_url = originalPhotoUrl,
                log_wear = logWear
            )
        )
    }
}
