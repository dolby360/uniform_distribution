package com.uniformdist.app.ui.screens.confirmation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.uniformdist.app.data.model.ItemMatchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchConfirmationScreen(
    onDone: () -> Unit,
    viewModel: MatchConfirmationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDone()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Confirm Matches") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (uiState.matchResults?.shirt == null && uiState.matchResults?.pants == null) {
                // No items detected
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No clothing items detected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Try taking another photo with better lighting",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDone) { Text("Go Back") }
                    }
                }
            }

            // Shirt section
            if (!uiState.shirtHandled) {
                uiState.matchResults?.shirt?.let { shirt ->
                    ItemMatchSection(
                        itemType = "Shirt",
                        item = shirt,
                        isLoading = uiState.isLoading,
                        onConfirm = { viewModel.confirmMatch("shirt", shirt) },
                        onAddNew = { viewModel.addNewItem("shirt", shirt) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Pants section
            if (!uiState.pantsHandled) {
                uiState.matchResults?.pants?.let { pants ->
                    ItemMatchSection(
                        itemType = "Pants",
                        item = pants,
                        isLoading = uiState.isLoading,
                        onConfirm = { viewModel.confirmMatch("pants", pants) },
                        onAddNew = { viewModel.addNewItem("pants", pants) }
                    )
                }
            }

            // Error with retry
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.retry() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemMatchSection(
    itemType: String,
    item: ItemMatchResult,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onAddNew: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = itemType,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Show the image
            val imageUrl = if (item.matched) item.image_url else item.cropped_url
            imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "$itemType image",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (item.matched) {
                Text(
                    text = "Match: ${((item.similarity ?: 0.0) * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirm,
                        enabled = !isLoading
                    ) {
                        Text("Confirm Match")
                    }
                    OutlinedButton(
                        onClick = onAddNew,
                        enabled = !isLoading
                    ) {
                        Text("Add as New")
                    }
                }
            } else {
                Text(
                    text = "New Item Detected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddNew,
                    enabled = !isLoading
                ) {
                    Text("Add to Wardrobe")
                }
            }
        }
    }
}
