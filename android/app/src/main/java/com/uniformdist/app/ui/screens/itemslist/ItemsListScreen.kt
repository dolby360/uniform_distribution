package com.uniformdist.app.ui.screens.itemslist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.uniformdist.app.data.model.ItemListEntry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ItemsListScreen(
    onAddClothes: () -> Unit,
    onItemDetail: (String) -> Unit,
    viewModel: ItemsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.justLoggedMessage) {
        uiState.justLoggedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissJustLoggedMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            if (uiState.shirts.isNotEmpty() || uiState.pants.isNotEmpty()) {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    if (uiState.pendingItem != null) {
        MarkWornDialog(
            isLogging = uiState.isLogging,
            onConfirm = { wornAtIso -> viewModel.confirmMarkWorn(wornAtIso) },
            onDismiss = { viewModel.cancelMarkWorn() },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "My Wardrobe",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onAddClothes,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Add new clothes")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                uiState.error != null && uiState.shirts.isEmpty() && uiState.pants.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Couldn't load your wardrobe",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                uiState.error ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadItems() }) { Text("Retry") }
                        }
                    }
                }

                else -> {
                    val pagerState = rememberPagerState(
                        initialPage = uiState.selectedTab.ordinal,
                        pageCount = { ItemTypeTab.entries.size },
                    )
                    val scope = rememberCoroutineScope()

                    // Keep the ViewModel's selected tab in sync with the pager
                    // (so swiping updates which type a logged wear belongs to).
                    LaunchedEffect(pagerState.currentPage) {
                        viewModel.selectTab(ItemTypeTab.entries[pagerState.currentPage])
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(selectedTabIndex = pagerState.currentPage) {
                            ItemTypeTab.entries.forEach { tab ->
                                val count = if (tab == ItemTypeTab.SHIRTS) uiState.shirts.size else uiState.pants.size
                                Tab(
                                    selected = pagerState.currentPage == tab.ordinal,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
                                    text = { Text("${tab.label} ($count)") },
                                )
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            val tab = ItemTypeTab.entries[page]
                            val items = if (tab == ItemTypeTab.SHIRTS) uiState.shirts else uiState.pants

                            if (items.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "No ${tab.label.lowercase()} yet",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(items, key = { it.id }) { item ->
                                        ItemCell(
                                            item = item,
                                            imageModel = viewModel.buildImageRequest(item),
                                            onTap = { viewModel.requestMarkWorn(item) },
                                            onInfoTap = { onItemDetail(item.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SyncIndicator(
                status = uiState.syncStatus,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )
        }
    }
}

@Composable
private fun SyncIndicator(status: SyncStatus, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shadowElevation = 2.dp,
    ) {
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            when (status) {
                SyncStatus.SYNCING -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                SyncStatus.SYNCED -> Icon(
                    Icons.Default.Check,
                    contentDescription = "Up to date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                SyncStatus.FAILED -> Icon(
                    Icons.Default.CloudOff,
                    contentDescription = "Sync failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ItemCell(
    item: ItemListEntry,
    imageModel: Any,
    onTap: () -> Unit,
    onInfoTap: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                )
                IconButton(
                    onClick = onInfoTap,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = CircleShape,
                        ),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Item details",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Worn ${item.wear_count}x",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    lastWornLabel(item.days_since_worn),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun lastWornLabel(daysSince: Int?): String = when {
    daysSince == null -> "Never worn"
    daysSince == 0 -> "Last worn today"
    daysSince == 1 -> "Last worn yesterday"
    daysSince < 30 -> "Last worn $daysSince days ago"
    else -> "Last worn 30+ days ago"
}
