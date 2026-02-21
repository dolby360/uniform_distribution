package com.uniformdist.app.ui.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uniformdist.app.ui.components.ItemStatCard
import com.uniformdist.app.ui.components.WearFrequencyChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wardrobe Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && !uiState.isRefreshing -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null && uiState.stats == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error loading statistics")
                        Text(
                            uiState.error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchStatistics() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                val stats = uiState.stats ?: return@Scaffold
                val pullRefreshState = rememberPullToRefreshState()

                if (pullRefreshState.isRefreshing) {
                    LaunchedEffect(true) {
                        viewModel.refresh()
                    }
                }
                if (!uiState.isRefreshing) {
                    LaunchedEffect(true) {
                        pullRefreshState.endRefresh()
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(padding)
                        .nestedScroll(pullRefreshState.nestedScrollConnection)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Totals card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Total Items: ${stats.totals.total_items}",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    "${stats.totals.total_shirts} shirts, ${stats.totals.total_pants} pants",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Total wears: ${stats.totals.total_wears}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Most Worn
                        Text(
                            "Most Worn",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (stats.most_worn.isEmpty()) {
                            Text("No items yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            stats.most_worn.forEach { item ->
                                ItemStatCard(item = item)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Least Worn
                        Text(
                            "Least Worn",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (stats.least_worn.isEmpty()) {
                            Text("No items yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            stats.least_worn.forEach { item ->
                                ItemStatCard(item = item)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // Not worn 30+ days
                        if (stats.not_worn_30_days.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Not Worn (30+ days)",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            stats.not_worn_30_days.forEach { item ->
                                ItemStatCard(item = item, showDaysSince = true)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Wear Frequency Chart
                        Text(
                            "Wear Frequency (30 days)",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        WearFrequencyChart(data = stats.wear_frequency)
                    }

                    PullToRefreshContainer(
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}
