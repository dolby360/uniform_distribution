package com.uniformdist.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTakePhoto: () -> Unit,
    onViewStats: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Uniform Distribution") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onTakePhoto) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Take a photo of your outfit to log what you're wearing today.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedButton(onClick = onViewStats) {
                Text("View Statistics")
            }
        }
    }
}
