# Step 9: Statistics and Analytics

## Goal

Implement statistics screen showing wear analytics: most/least worn items, items not worn in 30+ days, wear frequency, and total unique items. Backend endpoint + Android UI (Kotlin + Jetpack Compose).

## ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│              Statistics & Analytics Features                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Cloud Function: GET /statistics                                │
│  ┌──────────────────────────────────────────┐                  │
│  │  Query Firestore:                        │                  │
│  │  1. Most worn items (top 5)              │                  │
│  │     ORDER BY wear_count DESC LIMIT 5     │                  │
│  │                                          │                  │
│  │  2. Least worn items (bottom 5)          │                  │
│  │     ORDER BY wear_count ASC LIMIT 5      │                  │
│  │                                          │                  │
│  │  3. Items not worn in 30+ days           │                  │
│  │     WHERE last_worn < 30_days_ago        │                  │
│  │     OR last_worn IS NULL                 │                  │
│  │                                          │                  │
│  │  4. Total counts                         │                  │
│  │     - Total shirts                       │                  │
│  │     - Total pants                        │                  │
│  │     - Total wear logs                    │                  │
│  │                                          │                  │
│  │  5. Wear frequency (last 30 days)        │                  │
│  │     wear_logs WHERE worn_at > 30d_ago    │                  │
│  │     GROUP BY date                        │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  Response:                               │                  │
│  │  {                                       │                  │
│  │    most_worn: [...],                     │                  │
│  │    least_worn: [...],                    │                  │
│  │    not_worn_30_days: [...],              │                  │
│  │    totals: { ... },                      │                  │
│  │    wear_frequency: { ... }               │                  │
│  │  }                                       │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  Android StatisticsScreen (Jetpack Compose)                     │
│  ┌──────────────────────────────────────────┐                  │
│  │  Your Wardrobe Stats                     │                  │
│  │                                          │                  │
│  │  Total Items: 15 (8 shirts, 7 pants)    │                  │
│  │                                          │                  │
│  │  Most Worn:                              │                  │
│  │  [Item cards with wear counts]           │                  │
│  │                                          │                  │
│  │  Least Worn:                             │                  │
│  │  [Item cards]                            │                  │
│  │                                          │                  │
│  │  Not Worn (30+ days):                    │                  │
│  │  [Item cards with "Last worn: 45d ago"]  │                  │
│  │                                          │                  │
│  │  Wear Frequency Chart                    │                  │
│  │  [Canvas-drawn bar chart - last 30 days] │                  │
│  └──────────────────────────────────────────┘                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Schemas

### ItemStats

```kotlin
@JsonClass(generateAdapter = true)
data class ItemStats(
    val id: String,
    val type: String,        // "shirt" | "pants"
    val image_url: String,
    val wear_count: Int,
    val last_worn: String?,  // ISO timestamp
    val days_since_worn: Int?
)
```

### StatisticsResponse

```kotlin
@JsonClass(generateAdapter = true)
data class StatisticsResponse(
    val most_worn: List<ItemStats>,
    val least_worn: List<ItemStats>,
    val not_worn_30_days: List<ItemStats>,
    val totals: Totals,
    val wear_frequency: Map<String, Int>  // "2024-01-15": 2
)

@JsonClass(generateAdapter = true)
data class Totals(
    val total_shirts: Int,
    val total_pants: Int,
    val total_items: Int,
    val total_wears: Int
)
```

## API Endpoint Signatures

### GET /statistics

**Request**: None (GET request)

**Response** (200 OK):
```json
{
  "most_worn": [
    {
      "id": "abc123",
      "type": "shirt",
      "image_url": "https://storage.googleapis.com/...",
      "wear_count": 12,
      "last_worn": "2024-01-15T10:30:00Z",
      "days_since_worn": 2
    }
  ],
  "least_worn": [...],
  "not_worn_30_days": [...],
  "totals": {
    "total_shirts": 8,
    "total_pants": 7,
    "total_items": 15,
    "total_wears": 45
  },
  "wear_frequency": {
    "2024-01-10": 2,
    "2024-01-11": 1,
    "2024-01-12": 3
  }
}
```

**Cache**: 5 minutes

## File Structure

```
backend/
  functions/
    statistics.py                  # Statistics computation logic
    main.py                        # Add statistics endpoint

android/
  app/src/main/java/com/uniformdist/app/
    data/
      model/
        StatisticsResponse.kt     # Response data classes
      api/
        UniformDistApi.kt         # Add getStatistics()
    ui/
      screens/
        statistics/
          StatisticsScreen.kt     # Statistics display (Compose)
          StatisticsViewModel.kt  # ViewModel for statistics
      components/
        ItemStatCard.kt           # Display item with stats
        WearFrequencyChart.kt     # Canvas-drawn bar chart
```

## Key Code Snippets

### backend/functions/statistics.py

```python
import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from google.cloud import firestore
from datetime import datetime, timedelta
from storage.storage_client import StorageClient

def get_statistics() -> dict:
    """
    Calculate wardrobe statistics
    """
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
    storage = StorageClient()

    # 1. Most worn items (top 5)
    most_worn_query = db.collection('clothing_items')\
        .order_by('wear_count', direction=firestore.Query.DESCENDING)\
        .limit(5)

    most_worn = []
    for doc in most_worn_query.stream():
        data = doc.to_dict()
        most_worn.append({
            'id': doc.id,
            'type': data['type'],
            'image_url': storage.get_signed_url(data['image_url']),
            'wear_count': data['wear_count'],
            'last_worn': data.get('last_worn').isoformat() if data.get('last_worn') else None,
            'days_since_worn': calculate_days_since(data.get('last_worn'))
        })

    # 2. Least worn items (bottom 5)
    least_worn_query = db.collection('clothing_items')\
        .order_by('wear_count', direction=firestore.Query.ASCENDING)\
        .limit(5)

    least_worn = []
    for doc in least_worn_query.stream():
        data = doc.to_dict()
        least_worn.append({
            'id': doc.id,
            'type': data['type'],
            'image_url': storage.get_signed_url(data['image_url']),
            'wear_count': data['wear_count'],
            'last_worn': data.get('last_worn').isoformat() if data.get('last_worn') else None,
            'days_since_worn': calculate_days_since(data.get('last_worn'))
        })

    # 3. Items not worn in 30+ days
    thirty_days_ago = datetime.now() - timedelta(days=30)
    all_items = db.collection('clothing_items').stream()

    not_worn_30_days = []
    for doc in all_items:
        data = doc.to_dict()
        last_worn = data.get('last_worn')

        if last_worn is None or last_worn < thirty_days_ago:
            not_worn_30_days.append({
                'id': doc.id,
                'type': data['type'],
                'image_url': storage.get_signed_url(data['image_url']),
                'wear_count': data['wear_count'],
                'last_worn': last_worn.isoformat() if last_worn else None,
                'days_since_worn': calculate_days_since(last_worn)
            })

    # 4. Totals
    all_items_list = list(db.collection('clothing_items').stream())
    total_shirts = sum(1 for item in all_items_list if item.to_dict()['type'] == 'shirt')
    total_pants = sum(1 for item in all_items_list if item.to_dict()['type'] == 'pants')

    total_wears = sum(item.to_dict().get('wear_count', 0) for item in all_items_list)

    # 5. Wear frequency (last 30 days)
    wear_logs_query = db.collection('wear_logs')\
        .where('worn_at', '>=', thirty_days_ago)\
        .stream()

    wear_frequency = {}
    for log in wear_logs_query:
        log_data = log.to_dict()
        date_str = log_data['worn_at'].date().isoformat()
        wear_frequency[date_str] = wear_frequency.get(date_str, 0) + 1

    return {
        'most_worn': most_worn,
        'least_worn': least_worn,
        'not_worn_30_days': not_worn_30_days,
        'totals': {
            'total_shirts': total_shirts,
            'total_pants': total_pants,
            'total_items': total_shirts + total_pants,
            'total_wears': total_wears
        },
        'wear_frequency': wear_frequency
    }

def calculate_days_since(timestamp):
    """Calculate days since timestamp"""
    if timestamp is None:
        return None

    now = datetime.now()
    delta = now - timestamp
    return delta.days
```

### backend/functions/main.py (add endpoint)

```python
@functions_framework.http
def statistics_handler(request):
    """GET /statistics"""
    if request.method == 'OPTIONS':
        return ('', 204, {'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET', 'Access-Control-Allow-Headers': 'Content-Type'})

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        from statistics import get_statistics
        result = get_statistics()
        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({'error': str(e)}), 500, headers
```

### android: data/model/StatisticsResponse.kt

```kotlin
package com.uniformdist.app.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ItemStats(
    val id: String,
    val type: String,
    val image_url: String,
    val wear_count: Int,
    val last_worn: String?,
    val days_since_worn: Int?
)

@JsonClass(generateAdapter = true)
data class Totals(
    val total_shirts: Int,
    val total_pants: Int,
    val total_items: Int,
    val total_wears: Int
)

@JsonClass(generateAdapter = true)
data class StatisticsResponse(
    val most_worn: List<ItemStats>,
    val least_worn: List<ItemStats>,
    val not_worn_30_days: List<ItemStats>,
    val totals: Totals,
    val wear_frequency: Map<String, Int>
)
```

### android: data/api/UniformDistApi.kt (add function)

```kotlin
// Add to existing UniformDistApi interface:
@GET("/statistics")
suspend fun getStatistics(): StatisticsResponse
```

### android: ui/screens/statistics/StatisticsViewModel.kt

```kotlin
package com.uniformdist.app.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uniformdist.app.data.api.UniformDistApi
import com.uniformdist.app.data.model.StatisticsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val stats: StatisticsResponse? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val api: UniformDistApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        fetchStatistics()
    }

    fun fetchStatistics() {
        viewModelScope.launch {
            try {
                val data = api.getStatistics()
                _uiState.value = _uiState.value.copy(
                    stats = data,
                    isLoading = false,
                    isRefreshing = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message
                )
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        fetchStatistics()
    }
}
```

### android: ui/screens/statistics/StatisticsScreen.kt

```kotlin
package com.uniformdist.app.ui.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
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

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.padding(padding)
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
                }
            }
        }
    }
}
```

### android: ui/components/ItemStatCard.kt

```kotlin
package com.uniformdist.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.uniformdist.app.data.model.ItemStats

@Composable
fun ItemStatCard(
    item: ItemStats,
    showDaysSince: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.image_url,
                contentDescription = "${item.type} image",
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.type.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Worn ${item.wear_count} times",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showDaysSince) {
                    val dayText = when (item.days_since_worn) {
                        null -> "Never worn"
                        0 -> "Worn today"
                        else -> "Last worn: ${item.days_since_worn}d ago"
                    }
                    Text(
                        text = dayText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

### android: ui/components/WearFrequencyChart.kt

```kotlin
package com.uniformdist.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WearFrequencyChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary

    if (data.isEmpty()) {
        Card(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "No wear data for the last 30 days",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val sortedEntries = data.entries.sortedBy { it.key }
    val maxValue = sortedEntries.maxOf { it.value }.coerceAtLeast(1)

    Card(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(16.dp)
        ) {
            val barWidth = size.width / sortedEntries.size.coerceAtLeast(1)
            val chartHeight = size.height

            sortedEntries.forEachIndexed { index, entry ->
                val barHeight = (entry.value.toFloat() / maxValue) * chartHeight
                val x = index * barWidth

                drawRect(
                    color = barColor,
                    topLeft = Offset(x + barWidth * 0.1f, chartHeight - barHeight),
                    size = Size(barWidth * 0.8f, barHeight)
                )

                // Draw count label
                drawContext.canvas.nativeCanvas.drawText(
                    entry.value.toString(),
                    x + barWidth / 2,
                    chartHeight - barHeight - 8f,
                    android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                        color = android.graphics.Color.GRAY
                    }
                )
            }
        }
    }
}
```

## Acceptance Criteria

- [ ] Statistics endpoint returns all required data
- [ ] Most/least worn items calculated correctly
- [ ] Items not worn in 30+ days identified accurately
- [ ] Total counts match Firestore data
- [ ] Wear frequency aggregated by date
- [ ] StatisticsScreen displays all sections with Material 3 styling
- [ ] Item cards show images (via Coil), wear counts, and dates
- [ ] Wear frequency chart renders via Compose Canvas
- [ ] Loading state handled with CircularProgressIndicator
- [ ] Error handling with retry button
- [ ] Statistics update after logging new wear events
- [ ] Navigation from HomeScreen to StatisticsScreen works
- [ ] Pull-to-refresh functionality works
- [ ] Consistent with Material 3 theme from Step 8

## Setup Instructions

1. **Deploy Statistics Function**:
   ```bash
   cd backend
   ./deploy.sh  # Already includes statistics endpoint
   ```

2. **Add Statistics Screen to Navigation**:
   Update `NavGraph.kt` from Step 8 to include the Statistics route

3. **Build and Run**:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

## Verification

### Test Endpoint

```bash
FUNCTION_URL=$(gcloud functions describe statistics --region=us-central1 --gen2 --format="value(serviceConfig.uri)")

curl $FUNCTION_URL/statistics
```

### Expected Response

```json
{
  "most_worn": [
    {
      "id": "abc123",
      "type": "shirt",
      "image_url": "https://...",
      "wear_count": 12,
      "last_worn": "2024-01-15T10:30:00Z",
      "days_since_worn": 2
    }
  ],
  "totals": {
    "total_shirts": 8,
    "total_pants": 7,
    "total_items": 15,
    "total_wears": 45
  },
  "wear_frequency": {
    "2024-01-10": 2,
    "2024-01-11": 1
  }
}
```

### Test Script

```python
# backend/scripts/test_statistics.py
import requests
import os

def test_statistics():
    """Test statistics endpoint"""
    function_url = os.getenv('STATISTICS_URL')

    response = requests.get(f"{function_url}/statistics")

    print(f"Status: {response.status_code}")

    if response.status_code == 200:
        data = response.json()
        print(f"Most worn: {len(data['most_worn'])} items")
        print(f"Least worn: {len(data['least_worn'])} items")
        print(f"Not worn 30+ days: {len(data['not_worn_30_days'])} items")
        print(f"Total items: {data['totals']['total_items']}")
        print(f"Wear frequency: {len(data['wear_frequency'])} dates")
        print("\nStatistics test passed!")
    else:
        print(f"Error: {response.text}")

if __name__ == '__main__':
    test_statistics()
```

Run test:
```bash
export STATISTICS_URL=https://us-central1-uniform-dist-XXXXX.cloudfunctions.net
python backend/scripts/test_statistics.py
```

### Android Test Checklist

- [ ] Statistics screen opens from home screen
- [ ] Loading indicator shows while fetching
- [ ] All sections render with data
- [ ] Images load correctly via Coil
- [ ] Bar chart renders with correct proportions
- [ ] Pull-to-refresh triggers data reload
- [ ] Error state shows retry button
- [ ] Back navigation works
- [ ] Screen handles empty data gracefully
