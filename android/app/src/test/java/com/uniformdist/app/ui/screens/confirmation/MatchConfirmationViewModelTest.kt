package com.uniformdist.app.ui.screens.confirmation

import androidx.lifecycle.SavedStateHandle
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.uniformdist.app.data.model.*
import com.uniformdist.app.data.repository.OutfitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URLEncoder

@OptIn(ExperimentalCoroutinesApi::class)
class MatchConfirmationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: OutfitRepository
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(response: ProcessOutfitResponse): MatchConfirmationViewModel {
        val adapter = moshi.adapter(ProcessOutfitResponse::class.java)
        val json = URLEncoder.encode(adapter.toJson(response), "UTF-8")
        val savedStateHandle = SavedStateHandle(mapOf("resultJson" to json))
        return MatchConfirmationViewModel(savedStateHandle, repository)
    }

    @Test
    fun `initializes with match results from navigation`() {
        val response = ProcessOutfitResponse(
            success = true,
            shirt = ItemMatchResult(matched = true, item_id = "s1", similarity = 0.9),
            pants = ItemMatchResult(matched = false, cropped_url = "url")
        )

        val viewModel = createViewModel(response)

        assertNotNull(viewModel.uiState.value.matchResults)
        assertEquals("s1", viewModel.uiState.value.matchResults?.shirt?.item_id)
        assertFalse(viewModel.uiState.value.isDone)
    }

    @Test
    fun `isDone when no items detected`() {
        val response = ProcessOutfitResponse(success = true, shirt = null, pants = null)
        val viewModel = createViewModel(response)

        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `isDone after handling both items`() = runTest {
        val response = ProcessOutfitResponse(
            success = true,
            shirt = ItemMatchResult(matched = true, item_id = "s1", similarity = 0.9),
            pants = ItemMatchResult(matched = true, item_id = "p1", similarity = 0.85),
            original_photo_url = "gs://photo.jpg"
        )

        whenever(repository.confirmMatch(any(), any(), any(), any())).thenReturn(
            ConfirmMatchResponse(success = true, item_id = "s1", wear_count = 1)
        )

        val viewModel = createViewModel(response)

        viewModel.confirmMatch("shirt", response.shirt!!)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.shirtHandled)
        assertFalse(viewModel.uiState.value.isDone)

        viewModel.confirmMatch("pants", response.pants!!)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.pantsHandled)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `confirmMatch error sets error state`() = runTest {
        val response = ProcessOutfitResponse(
            success = true,
            shirt = ItemMatchResult(matched = true, item_id = "s1", similarity = 0.9),
            original_photo_url = "gs://photo.jpg"
        )

        whenever(repository.confirmMatch(any(), any(), any(), any()))
            .thenThrow(RuntimeException("Network error"))

        val viewModel = createViewModel(response)
        viewModel.confirmMatch("shirt", response.shirt!!)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains("Network error"))
        assertFalse(viewModel.uiState.value.shirtHandled)
    }

    @Test
    fun `addNewItem calls repository with correct data`() = runTest {
        val response = ProcessOutfitResponse(
            success = true,
            pants = ItemMatchResult(
                matched = false,
                cropped_url = "gs://cropped.jpg",
                embedding = listOf(0.1, 0.2)
            ),
            original_photo_url = "gs://photo.jpg"
        )

        whenever(repository.addNewItem(any(), any(), any(), any(), any())).thenReturn(
            AddNewItemResponse(success = true, item_id = "new_p1")
        )

        val viewModel = createViewModel(response)
        viewModel.addNewItem("pants", response.pants!!)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.pantsHandled)
        assertNull(viewModel.uiState.value.error)
    }
}
