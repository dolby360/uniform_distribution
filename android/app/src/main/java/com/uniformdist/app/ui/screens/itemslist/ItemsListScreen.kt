package com.uniformdist.app.ui.screens.itemslist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.uniformdist.app.data.model.ItemListEntry
import com.uniformdist.app.ui.theme.NeverWorn
import com.uniformdist.app.ui.theme.WornAWhileAgo
import com.uniformdist.app.ui.theme.WornLongAgo
import com.uniformdist.app.ui.theme.WornRecently
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
            TopAppBar(
                title = {
                    Text(
                        "My Wardrobe",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClothes,
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add outfit", style = MaterialTheme.typography.labelLarge)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    LoadingPlaceholderGrid()
                }

                uiState.error != null && uiState.shirts.isEmpty() && uiState.pants.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Couldn't load your wardrobe",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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
                        WardrobeTabs(
                            selectedIndex = pagerState.currentPage,
                            counts = listOf(uiState.shirts.size, uiState.pants.size),
                            onSelect = { index ->
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                        )

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            val tab = ItemTypeTab.entries[page]
                            val items = if (tab == ItemTypeTab.SHIRTS) uiState.shirts else uiState.pants

                            if (items.isEmpty()) {
                                EmptyWardrobe(label = tab.label.lowercase())
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 8.dp,
                                        bottom = 96.dp,
                                    ),
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

/** Pill-style segmented control driving (and driven by) the pager. */
@Composable
private fun WardrobeTabs(
    selectedIndex: Int,
    counts: List<Int>,
    onSelect: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            ItemTypeTab.entries.forEachIndexed { index, tab ->
                val selected = selectedIndex == index
                val pillColor by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "tabPill",
                )
                val textColor by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tabText",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(pillColor)
                        .clickable { onSelect(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${tab.label} · ${counts[index]}",
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWardrobe(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Checkroom,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No $label yet",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Snap a photo to start tracking your wardrobe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Pulsing placeholder cards shown while the wardrobe loads from cache/network. */
@Composable
private fun LoadingPlaceholderGrid() {
    val pulse by rememberInfiniteTransition(label = "placeholderPulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "placeholderAlpha",
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .alpha(pulse)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "pressScale")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
            ),
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Bottom scrim keeps the overlaid stats readable on any photo.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        ) {
            Text(
                "Worn ${item.wear_count}×",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(recencyColor(item.days_since_worn), CircleShape),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    lastWornLabel(item.days_since_worn),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        IconButton(
            onClick = onInfoTap,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(34.dp)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Item details",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// Color-codes how due an item is for a wear: green = worn recently,
// amber = a while ago, terracotta = neglected, gray = never worn.
private fun recencyColor(daysSince: Int?): Color = when {
    daysSince == null -> NeverWorn
    daysSince <= 7 -> WornRecently
    daysSince < 30 -> WornAWhileAgo
    else -> WornLongAgo
}

private fun lastWornLabel(daysSince: Int?): String = when {
    daysSince == null -> "Never worn"
    daysSince == 0 -> "Last worn today"
    daysSince == 1 -> "Last worn yesterday"
    daysSince < 30 -> "Last worn $daysSince days ago"
    else -> "Last worn 30+ days ago"
}
