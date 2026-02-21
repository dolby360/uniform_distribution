package com.uniformdist.app.ui.screens.crop

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uniformdist.app.ui.components.CropOverlay
import com.uniformdist.app.ui.components.LoadingOverlay
import java.io.File
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCropScreen(
    imageUri: String,
    onCropComplete: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ManualCropViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val decodedPath = URLDecoder.decode(imageUri, "UTF-8")
    val imageBytes = remember(decodedPath) {
        File(decodedPath).readBytes()
    }
    val bitmap = remember(imageBytes) {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    var imageDisplaySize by remember { mutableStateOf(IntSize.Zero) }

    // Default crop rect: centered rectangle covering ~60% of the image area
    var cropRect by remember(imageDisplaySize) {
        val w = imageDisplaySize.width.toFloat()
        val h = imageDisplaySize.height.toFloat()
        mutableStateOf(
            if (w > 0 && h > 0) {
                Rect(w * 0.15f, h * 0.1f, w * 0.85f, h * 0.55f)
            } else {
                Rect(50f, 50f, 300f, 400f)
            }
        )
    }

    // Navigate when result is ready
    LaunchedEffect(uiState.result) {
        uiState.result?.let { response ->
            val moshi = com.squareup.moshi.Moshi.Builder()
                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(
                com.uniformdist.app.data.model.ProcessOutfitResponse::class.java
            )
            val json = java.net.URLEncoder.encode(adapter.toJson(response), "UTF-8")
            viewModel.clearResult()
            onCropComplete(json)
        }
    }

    val stepTitle = when (uiState.currentStep) {
        CropStep.SHIRT -> "Step 1/2: Crop Shirt"
        CropStep.PANTS -> "Step 2/2: Crop Pants"
    }

    val stepSubtitle = when (uiState.currentStep) {
        CropStep.SHIRT -> "Drag the rectangle to select the shirt area"
        CropStep.PANTS -> "Drag the rectangle to select the pants area"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Crop") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Step indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stepTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stepSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Image with crop overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Outfit photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { imageDisplaySize = it }
                )
                CropOverlay(
                    cropRect = cropRect,
                    onCropRectChanged = { cropRect = it },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        when (uiState.currentStep) {
                            CropStep.SHIRT -> {
                                viewModel.skipShirt()
                                // Reset crop rect for pants step
                                val w = imageDisplaySize.width.toFloat()
                                val h = imageDisplaySize.height.toFloat()
                                cropRect = Rect(w * 0.2f, h * 0.45f, w * 0.8f, h * 0.9f)
                            }
                            CropStep.PANTS -> {
                                viewModel.skipPants()
                                // Submit with whatever we have
                                viewModel.submitCrops(
                                    imageBytes,
                                    imageDisplaySize.width,
                                    imageDisplaySize.height
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Skip")
                }

                Button(
                    onClick = {
                        when (uiState.currentStep) {
                            CropStep.SHIRT -> {
                                viewModel.confirmShirtCrop(cropRect)
                                // Reset crop rect for pants step (lower half)
                                val w = imageDisplaySize.width.toFloat()
                                val h = imageDisplaySize.height.toFloat()
                                cropRect = Rect(w * 0.2f, h * 0.45f, w * 0.8f, h * 0.9f)
                            }
                            CropStep.PANTS -> {
                                viewModel.confirmPantsCrop(cropRect)
                                viewModel.submitCrops(
                                    imageBytes,
                                    imageDisplaySize.width,
                                    imageDisplaySize.height
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when (uiState.currentStep) {
                            CropStep.SHIRT -> "Next"
                            CropStep.PANTS -> "Done"
                        }
                    )
                }
            }
        }

        // Loading overlay
        if (uiState.isProcessing) {
            LoadingOverlay(message = "Processing crops...")
        }

        // Error dialog
        uiState.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearResult() },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearResult() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
