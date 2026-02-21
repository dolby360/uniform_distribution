package com.uniformdist.app.data.model

import com.squareup.moshi.JsonClass

// --- Process Outfit ---

@JsonClass(generateAdapter = true)
data class ProcessOutfitRequest(
    val image: String  // base64-encoded JPEG
)

@JsonClass(generateAdapter = true)
data class ItemMatchResult(
    val matched: Boolean,
    val item_id: String? = null,
    val similarity: Double? = null,
    val image_url: String? = null,
    val cropped_url: String? = null,
    val embedding: List<Double>? = null
)

@JsonClass(generateAdapter = true)
data class ProcessOutfitResponse(
    val success: Boolean,
    val shirt: ItemMatchResult? = null,
    val pants: ItemMatchResult? = null,
    val original_photo_url: String? = null,
    val error: String? = null
)

// --- Confirm Match ---

@JsonClass(generateAdapter = true)
data class ConfirmMatchRequest(
    val item_id: String,
    val item_type: String,
    val original_photo_url: String,
    val similarity_score: Double? = null,
    val embedding: List<Double>? = null,
    val cropped_url: String? = null
)

@JsonClass(generateAdapter = true)
data class ConfirmMatchResponse(
    val success: Boolean,
    val item_id: String,
    val wear_count: Int,
    val last_worn: String? = null
)

// --- Add New Item ---

@JsonClass(generateAdapter = true)
data class AddNewItemRequest(
    val item_type: String,
    val cropped_image_url: String,
    val embedding: List<Double>,
    val original_photo_url: String,
    val log_wear: Boolean
)

@JsonClass(generateAdapter = true)
data class AddNewItemResponse(
    val success: Boolean,
    val item_id: String
)

// --- Statistics ---

@JsonClass(generateAdapter = true)
data class ItemStats(
    val id: String,
    val type: String,
    val image_url: String,
    val wear_count: Int,
    val last_worn: String?,
    val days_since_worn: Int?
)

@JsonClass(generateAdapter = true)
data class Totals(
    val total_shirts: Int,
    val total_pants: Int,
    val total_items: Int,
    val total_wears: Int
)

@JsonClass(generateAdapter = true)
data class StatisticsResponse(
    val most_worn: List<ItemStats>,
    val least_worn: List<ItemStats>,
    val not_worn_30_days: List<ItemStats>,
    val totals: Totals,
    val wear_frequency: Map<String, Int>
)
