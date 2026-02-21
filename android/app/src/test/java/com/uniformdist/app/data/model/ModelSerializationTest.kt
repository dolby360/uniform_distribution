package com.uniformdist.app.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ModelSerializationTest {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun `ProcessOutfitRequest serializes correctly`() {
        val request = ProcessOutfitRequest(image = "base64data")
        val adapter = moshi.adapter(ProcessOutfitRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"image\""))
        assertTrue(json.contains("\"base64data\""))
    }

    @Test
    fun `ProcessOutfitResponse deserializes with matched shirt`() {
        val json = """
        {
            "success": true,
            "original_photo_url": "gs://bucket/photo.jpg",
            "shirt": {
                "matched": true,
                "item_id": "shirt_001",
                "similarity": 0.92,
                "image_url": "https://example.com/shirt.jpg",
                "cropped_url": "https://example.com/cropped.jpg",
                "embedding": [0.1, 0.2, 0.3]
            },
            "pants": null
        }
        """.trimIndent()

        val adapter = moshi.adapter(ProcessOutfitResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)
        assertEquals("gs://bucket/photo.jpg", response.original_photo_url)
        assertNotNull(response.shirt)
        assertTrue(response.shirt!!.matched)
        assertEquals("shirt_001", response.shirt!!.item_id)
        assertEquals(0.92, response.shirt!!.similarity!!, 0.01)
        assertNull(response.pants)
    }

    @Test
    fun `ProcessOutfitResponse deserializes with new item (unmatched)`() {
        val json = """
        {
            "success": true,
            "original_photo_url": "gs://bucket/photo.jpg",
            "shirt": {
                "matched": false,
                "cropped_url": "https://example.com/cropped.jpg",
                "embedding": [0.1, 0.2, 0.3]
            },
            "pants": {
                "matched": true,
                "item_id": "pants_002",
                "similarity": 0.88,
                "image_url": "https://example.com/pants.jpg"
            }
        }
        """.trimIndent()

        val adapter = moshi.adapter(ProcessOutfitResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertFalse(response!!.shirt!!.matched)
        assertNull(response.shirt!!.item_id)
        assertTrue(response.pants!!.matched)
        assertEquals("pants_002", response.pants!!.item_id)
    }

    @Test
    fun `ProcessOutfitResponse deserializes error response`() {
        val json = """
        {
            "success": false,
            "error": "Missing image data"
        }
        """.trimIndent()

        val adapter = moshi.adapter(ProcessOutfitResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertFalse(response!!.success)
        assertEquals("Missing image data", response.error)
        assertNull(response.shirt)
        assertNull(response.pants)
    }

    @Test
    fun `ConfirmMatchRequest serializes with snake_case fields`() {
        val request = ConfirmMatchRequest(
            item_id = "abc123",
            item_type = "shirt",
            original_photo_url = "gs://bucket/photo.jpg",
            similarity_score = 0.95
        )
        val adapter = moshi.adapter(ConfirmMatchRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"item_id\""))
        assertTrue(json.contains("\"item_type\""))
        assertTrue(json.contains("\"original_photo_url\""))
        assertTrue(json.contains("\"similarity_score\""))
    }

    @Test
    fun `ConfirmMatchResponse deserializes correctly`() {
        val json = """
        {
            "success": true,
            "item_id": "abc123",
            "wear_count": 5,
            "last_worn": "2024-01-15T10:30:00Z"
        }
        """.trimIndent()

        val adapter = moshi.adapter(ConfirmMatchResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)
        assertEquals("abc123", response.item_id)
        assertEquals(5, response.wear_count)
    }

    @Test
    fun `AddNewItemRequest serializes correctly`() {
        val request = AddNewItemRequest(
            item_type = "pants",
            cropped_image_url = "gs://bucket/cropped.jpg",
            embedding = listOf(0.1, 0.2, 0.3),
            original_photo_url = "gs://bucket/photo.jpg",
            log_wear = true
        )
        val adapter = moshi.adapter(AddNewItemRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"item_type\""))
        assertTrue(json.contains("\"cropped_image_url\""))
        assertTrue(json.contains("\"embedding\""))
        assertTrue(json.contains("\"log_wear\""))
        assertTrue(json.contains("true"))
    }

    @Test
    fun `AddNewItemResponse deserializes correctly`() {
        val json = """
        {
            "success": true,
            "item_id": "new_item_456"
        }
        """.trimIndent()

        val adapter = moshi.adapter(AddNewItemResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertTrue(response!!.success)
        assertEquals("new_item_456", response.item_id)
    }

    @Test
    fun `ProcessOutfitResponse roundtrip serialize-deserialize`() {
        val original = ProcessOutfitResponse(
            success = true,
            shirt = ItemMatchResult(
                matched = true,
                item_id = "s1",
                similarity = 0.95,
                image_url = "https://img.com/s1.jpg"
            ),
            pants = ItemMatchResult(
                matched = false,
                cropped_url = "https://img.com/p_crop.jpg",
                embedding = listOf(0.1, 0.2)
            ),
            original_photo_url = "gs://bucket/photo.jpg"
        )

        val adapter = moshi.adapter(ProcessOutfitResponse::class.java)
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json)

        assertEquals(original, deserialized)
    }
}
