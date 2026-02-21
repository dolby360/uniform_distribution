package com.uniformdist.app.data.repository

import com.uniformdist.app.data.api.UniformDistApi
import com.uniformdist.app.data.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OutfitRepositoryTest {

    private lateinit var api: UniformDistApi
    private lateinit var repository: OutfitRepository

    @Before
    fun setUp() {
        api = mock()
        repository = OutfitRepository(api)
    }

    @Test
    fun `processOutfit returns successful response with match`() = runTest {
        val expectedResponse = ProcessOutfitResponse(
            success = true,
            original_photo_url = "gs://bucket/photo.jpg",
            shirt = ItemMatchResult(
                matched = true,
                item_id = "shirt_001",
                similarity = 0.92,
                image_url = "https://example.com/shirt.jpg"
            )
        )

        whenever(api.processOutfit(any(), any())).thenReturn(expectedResponse)

        val result = repository.processOutfit("base64ImageData")

        assertTrue(result.success)
        assertNotNull(result.shirt)
        assertTrue(result.shirt!!.matched)
        assertEquals("shirt_001", result.shirt!!.item_id)
    }

    @Test
    fun `processOutfit sends correct request body`() = runTest {
        whenever(api.processOutfit(any(), any())).thenReturn(
            ProcessOutfitResponse(success = true)
        )

        repository.processOutfit("testBase64Data")

        val requestCaptor = argumentCaptor<ProcessOutfitRequest>()
        verify(api).processOutfit(any(), requestCaptor.capture())
        assertEquals("testBase64Data", requestCaptor.firstValue.image)
    }

    @Test
    fun `confirmMatch returns updated wear count`() = runTest {
        val expectedResponse = ConfirmMatchResponse(
            success = true,
            item_id = "abc123",
            wear_count = 5,
            last_worn = "2024-01-15T10:30:00Z"
        )

        whenever(api.confirmMatch(any(), any())).thenReturn(expectedResponse)

        val result = repository.confirmMatch(
            itemId = "abc123",
            itemType = "shirt",
            originalPhotoUrl = "gs://bucket/photo.jpg",
            similarityScore = 0.92
        )

        assertTrue(result.success)
        assertEquals(5, result.wear_count)
    }

    @Test
    fun `addNewItem returns new item id`() = runTest {
        val expectedResponse = AddNewItemResponse(
            success = true,
            item_id = "new_item_456"
        )

        whenever(api.addNewItem(any(), any())).thenReturn(expectedResponse)

        val result = repository.addNewItem(
            itemType = "pants",
            croppedImageUrl = "gs://bucket/cropped.jpg",
            embedding = listOf(0.1, 0.2, 0.3),
            originalPhotoUrl = "gs://bucket/photo.jpg",
            logWear = true
        )

        assertTrue(result.success)
        assertEquals("new_item_456", result.item_id)
    }

    @Test(expected = Exception::class)
    fun `processOutfit throws on api error`() = runTest {
        whenever(api.processOutfit(any(), any())).thenThrow(RuntimeException("Server error"))
        repository.processOutfit("base64ImageData")
    }
}
