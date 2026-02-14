# Step 9: Statistics and Analytics

## Goal

Implement statistics screen showing wear analytics: most/least worn items, items not worn in 30+ days, wear frequency, and total unique items.

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
│  StatisticsScreen (React Native)                                │
│  ┌──────────────────────────────────────────┐                  │
│  │  📊 Your Wardrobe Stats                  │                  │
│  │                                          │                  │
│  │  Total Items: 15 (8 shirts, 7 pants)    │                  │
│  │                                          │                  │
│  │  🔥 Most Worn:                            │                  │
│  │  [Item cards with wear counts]           │                  │
│  │                                          │                  │
│  │  😴 Least Worn:                           │                  │
│  │  [Item cards]                            │                  │
│  │                                          │                  │
│  │  ⏰ Not Worn (30+ days):                  │                  │
│  │  [Item cards with "Last worn: 45d ago"]  │                  │
│  │                                          │                  │
│  │  📈 Wear Frequency Chart                  │                  │
│  │  [Simple bar chart - last 30 days]       │                  │
│  └──────────────────────────────────────────┘                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Schemas

### ItemStats

```typescript
interface ItemStats {
  id: string;
  type: 'shirt' | 'pants';
  image_url: string;
  wear_count: number;
  last_worn: string | null;  // ISO timestamp
  days_since_worn: number | null;
}
```

### StatisticsResponse

```typescript
interface StatisticsResponse {
  most_worn: ItemStats[];      // Top 5
  least_worn: ItemStats[];     // Bottom 5
  not_worn_30_days: ItemStats[];
  totals: {
    total_shirts: number;
    total_pants: number;
    total_items: number;
    total_wears: number;
  };
  wear_frequency: {
    [date: string]: number;    // "2024-01-15": 2
  };
}
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

app/
  screens/
    StatisticsScreen.tsx           # Statistics display
    StatisticsScreen.logic.ts      # Fetch and process stats
    StatisticsScreen.styles.ts
  components/
    ItemStatCard.tsx               # Display item with stats
    WearFrequencyChart.tsx         # Simple bar chart
  api/
    cloudFunctions.ts              # Add getStatistics()
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

### app/api/cloudFunctions.ts (add function)

```typescript
export async function getStatistics(): Promise<StatisticsResponse> {
  const response = await fetch(`${API_CONFIG.BASE_URL}/statistics`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  });

  if (!response.ok) {
    throw new Error(`API error: ${response.status}`);
  }

  return response.json();
}
```

### app/screens/StatisticsScreen.tsx

```typescript
import React from 'react';
import { ScrollView, View, Text, RefreshControl } from 'react-native';
import { useStatistics } from './StatisticsScreen.logic';
import ItemStatCard from '../components/ItemStatCard';
import WearFrequencyChart from '../components/WearFrequencyChart';
import LoadingOverlay from '../components/LoadingOverlay';
import { styles } from './StatisticsScreen.styles';

export default function StatisticsScreen() {
  const { stats, loading, error, refreshing, onRefresh } = useStatistics();

  if (loading && !refreshing) {
    return <LoadingOverlay message="Loading statistics..." />;
  }

  if (error) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>Error loading statistics</Text>
        <Text style={styles.errorDetail}>{error}</Text>
      </View>
    );
  }

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
    >
      <Text style={styles.title}>📊 Wardrobe Statistics</Text>

      {/* Totals */}
      <View style={styles.totalsCard}>
        <Text style={styles.totalText}>
          Total Items: {stats.totals.total_items}
        </Text>
        <Text style={styles.subText}>
          {stats.totals.total_shirts} shirts, {stats.totals.total_pants} pants
        </Text>
        <Text style={styles.subText}>
          Total wears: {stats.totals.total_wears}
        </Text>
      </View>

      {/* Most Worn */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>🔥 Most Worn</Text>
        {stats.most_worn.length > 0 ? (
          stats.most_worn.map(item => (
            <ItemStatCard key={item.id} item={item} />
          ))
        ) : (
          <Text style={styles.emptyText}>No items yet</Text>
        )}
      </View>

      {/* Least Worn */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>😴 Least Worn</Text>
        {stats.least_worn.length > 0 ? (
          stats.least_worn.map(item => (
            <ItemStatCard key={item.id} item={item} />
          ))
        ) : (
          <Text style={styles.emptyText}>No items yet</Text>
        )}
      </View>

      {/* Not Worn 30+ Days */}
      {stats.not_worn_30_days.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>⏰ Not Worn (30+ days)</Text>
          {stats.not_worn_30_days.map(item => (
            <ItemStatCard key={item.id} item={item} showDaysSince />
          ))}
        </View>
      )}

      {/* Wear Frequency Chart */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>📈 Wear Frequency (30 days)</Text>
        <WearFrequencyChart data={stats.wear_frequency} />
      </View>
    </ScrollView>
  );
}
```

### app/screens/StatisticsScreen.logic.ts

```typescript
import { useState, useEffect } from 'react';
import { getStatistics } from '../api/cloudFunctions';
import { StatisticsResponse } from '../api/types';

export const useStatistics = () => {
  const [stats, setStats] = useState<StatisticsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const fetchStatistics = async () => {
    try {
      const data = await getStatistics();
      setStats(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const onRefresh = () => {
    setRefreshing(true);
    fetchStatistics();
  };

  useEffect(() => {
    fetchStatistics();
  }, []);

  return {
    stats,
    loading,
    error,
    refreshing,
    onRefresh,
  };
};
```

### app/components/ItemStatCard.tsx

```typescript
import React from 'react';
import { View, Text, Image, StyleSheet } from 'react-native';

interface ItemStatCardProps {
  item: {
    id: string;
    type: string;
    image_url: string;
    wear_count: number;
    last_worn: string | null;
    days_since_worn: number | null;
  };
  showDaysSince?: boolean;
}

export default function ItemStatCard({ item, showDaysSince = false }: ItemStatCardProps) {
  return (
    <View style={styles.card}>
      <Image source={{ uri: item.image_url }} style={styles.image} />
      <View style={styles.info}>
        <Text style={styles.type}>{item.type}</Text>
        <Text style={styles.wearCount}>Worn {item.wear_count} times</Text>
        {showDaysSince && item.days_since_worn !== null && (
          <Text style={styles.daysSince}>
            {item.days_since_worn === 0
              ? 'Worn today'
              : item.days_since_worn === null
              ? 'Never worn'
              : `Last worn: ${item.days_since_worn}d ago`}
          </Text>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 12,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  image: {
    width: 80,
    height: 80,
    borderRadius: 8,
    marginRight: 12,
  },
  info: {
    flex: 1,
    justifyContent: 'center',
  },
  type: {
    fontSize: 16,
    fontWeight: '600',
    textTransform: 'capitalize',
    marginBottom: 4,
  },
  wearCount: {
    fontSize: 14,
    color: '#666',
  },
  daysSince: {
    fontSize: 12,
    color: '#999',
    marginTop: 4,
  },
});
```

### app/components/WearFrequencyChart.tsx

```typescript
import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

interface WearFrequencyChartProps {
  data: { [date: string]: number };
}

export default function WearFrequencyChart({ data }: WearFrequencyChartProps) {
  const dates = Object.keys(data).sort();
  const maxWears = Math.max(...Object.values(data), 1);

  return (
    <View style={styles.container}>
      {dates.length === 0 ? (
        <Text style={styles.emptyText}>No wear data for the last 30 days</Text>
      ) : (
        <View style={styles.chartContainer}>
          {dates.map(date => (
            <View key={date} style={styles.barContainer}>
              <View
                style={[
                  styles.bar,
                  { height: (data[date] / maxWears) * 100 },
                ]}
              />
              <Text style={styles.barLabel}>{data[date]}</Text>
              <Text style={styles.dateLabel}>
                {new Date(date).toLocaleDateString('en-US', {
                  month: 'short',
                  day: 'numeric',
                })}
              </Text>
            </View>
          ))}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 16,
    minHeight: 150,
  },
  emptyText: {
    textAlign: 'center',
    color: '#999',
    fontSize: 14,
  },
  chartContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    justifyContent: 'space-between',
    height: 120,
  },
  barContainer: {
    flex: 1,
    alignItems: 'center',
    marginHorizontal: 2,
  },
  bar: {
    width: '100%',
    backgroundColor: '#2196F3',
    borderTopLeftRadius: 4,
    borderTopRightRadius: 4,
    minHeight: 4,
  },
  barLabel: {
    fontSize: 10,
    fontWeight: '600',
    marginTop: 4,
  },
  dateLabel: {
    fontSize: 8,
    color: '#666',
    marginTop: 2,
    transform: [{ rotate: '-45deg' }],
  },
});
```

## Acceptance Criteria

- [ ] Statistics endpoint returns all required data
- [ ] Most/least worn items calculated correctly
- [ ] Items not worn in 30+ days identified accurately
- [ ] Total counts match Firestore data
- [ ] Wear frequency aggregated by date
- [ ] StatisticsScreen displays all sections
- [ ] Item cards show images, wear counts, and dates
- [ ] Wear frequency chart renders (simple bar chart)
- [ ] Loading state handled gracefully
- [ ] Error handling for API failures
- [ ] Statistics update after logging new wear events
- [ ] Navigation from HomeScreen to StatisticsScreen works
- [ ] Pull-to-refresh functionality works

## Setup Instructions

1. **Deploy Statistics Function**:
   ```bash
   cd backend
   ./deploy.sh  # Already includes statistics endpoint
   ```

2. **Update App Navigation**:
   Add StatisticsScreen to stack navigator in `App.tsx`

3. **Add Navigation Button**:
   Add button in HomeScreen to navigate to Statistics

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
        print(f"✓ Most worn: {len(data['most_worn'])} items")
        print(f"✓ Least worn: {len(data['least_worn'])} items")
        print(f"✓ Not worn 30+ days: {len(data['not_worn_30_days'])} items")
        print(f"✓ Total items: {data['totals']['total_items']}")
        print(f"✓ Wear frequency: {len(data['wear_frequency'])} dates")
        print("\n✓ Statistics test passed!")
    else:
        print(f"✗ Error: {response.text}")

if __name__ == '__main__':
    test_statistics()
```

Run test:
```bash
export STATISTICS_URL=https://us-central1-uniform-dist-XXXXX.cloudfunctions.net
python backend/scripts/test_statistics.py
```
