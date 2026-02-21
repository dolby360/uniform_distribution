package com.uniformdist.app

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.uniformdist.app.data.api.ApiConfig
import com.uniformdist.app.data.model.ProcessOutfitRequest
import com.uniformdist.app.data.model.ProcessOutfitResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * End-to-end test that calls the real backend.
 *
 * Requirements:
 * - Real Cloud Function URLs configured in ApiConfig
 * - Internet access from the test device/emulator
 * - Backend functions deployed and running
 */
@RunWith(AndroidJUnit4::class)
class EndToEndApiTest {

    private lateinit var client: OkHttpClient
    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun testProcessOutfitEndpointReachable() {
        val body = """{"image": ""}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(ApiConfig.PROCESS_OUTFIT_URL)
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        assertTrue("Backend should be reachable", response.code in 200..599)
    }

    @Test
    fun testProcessOutfitWithMinimalImage() {
        val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.BLUE)
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, stream)
        val imageBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        val adapter = moshi.adapter(ProcessOutfitRequest::class.java)
        val requestBody = adapter.toJson(ProcessOutfitRequest(image = imageBase64))
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(ApiConfig.PROCESS_OUTFIT_URL)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        assertNotNull("Response body should not be null", responseBody)

        val responseAdapter = moshi.adapter(ProcessOutfitResponse::class.java)
        val processResponse = responseAdapter.fromJson(responseBody!!)

        assertNotNull("Should parse response", processResponse)
    }

    @Test
    fun testConfirmMatchEndpointReachable() {
        val body = """{}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(ApiConfig.CONFIRM_MATCH_URL)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        assertEquals("Should return 400 for missing fields", 400, response.code)
        assertTrue("Error should mention missing fields", responseBody.contains("Missing"))
    }

    @Test
    fun testAddNewItemEndpointReachable() {
        val body = """{}""".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(ApiConfig.ADD_NEW_ITEM_URL)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        assertEquals("Should return 400 for missing fields", 400, response.code)
        assertTrue("Error should mention missing fields", responseBody.contains("Missing"))
    }
}
