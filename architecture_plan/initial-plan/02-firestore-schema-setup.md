# Step 2: Firestore Schema Setup

## Goal

Design and initialize Firestore database schema for clothing items and wear logs with proper indexing.

## ASCII Architecture Diagram

```
┌───────────────────────────────────────────────────────────┐
│                    Firestore Database                      │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  Collection: clothing_items                               │
│  ┌─────────────────────────────────────────────────────┐ │
│  │ Document ID: auto-generated                         │ │
│  │ {                                                    │ │
│  │   id: string                    // Auto ID          │ │
│  │   type: "shirt" | "pants"       // Clothing type    │ │
│  │   image_url: string             // Cloud Storage    │ │
│  │   embedding: number[]           // 1408 dimensions  │ │
│  │   created_at: timestamp                             │ │
│  │   last_worn: timestamp | null                       │ │
│  │   wear_count: number            // Total wears      │ │
│  │   thumbnail_url: string         // Optional         │ │
│  │ }                                                    │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                            │
│  Collection: wear_logs                                    │
│  ┌─────────────────────────────────────────────────────┐ │
│  │ Document ID: auto-generated                         │ │
│  │ {                                                    │ │
│  │   id: string                                        │ │
│  │   item_id: string               // Ref to item      │ │
│  │   item_type: "shirt" | "pants"                      │ │
│  │   worn_at: timestamp            // When logged      │ │
│  │   confidence_score: number      // Match confidence │ │
│  │   original_image_url: string    // Full photo       │ │
│  │ }                                                    │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                            │
│  Indexes:                                                 │
│  ┌─────────────────────────────────────────────────────┐ │
│  │ clothing_items:                                     │ │
│  │   - type (ASC)                                      │ │
│  │   - last_worn (DESC)                                │ │
│  │   - wear_count (DESC)                               │ │
│  │ wear_logs:                                          │ │
│  │   - item_id (ASC) + worn_at (DESC)                  │ │
│  │   - worn_at (DESC)                                  │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                            │
└───────────────────────────────────────────────────────────┘
```

## Data Schemas

### ClothingItem

```typescript
interface ClothingItem {
  id: string;                    // Firestore document ID
  type: 'shirt' | 'pants';
  image_url: string;             // gs://bucket/path/to/image.jpg
  embedding: number[];           // Length: 1408
  created_at: FirebaseFirestore.Timestamp;
  last_worn: FirebaseFirestore.Timestamp | null;
  wear_count: number;
  thumbnail_url?: string;        // Optional thumbnail
}
```

### WearLog

```typescript
interface WearLog {
  id: string;
  item_id: string;               // Reference to ClothingItem
  item_type: 'shirt' | 'pants';
  worn_at: FirebaseFirestore.Timestamp;
  confidence_score: number;      // 0.0 - 1.0
  original_image_url: string;    // Full outfit photo
}
```

## API Endpoint Signatures

N/A (schema definition only)

## File Structure

```
backend/
  schemas/
    clothing_item.py               # ClothingItem model
    wear_log.py                    # WearLog model
    __init__.py
  scripts/
    init_firestore.py              # Initialize Firestore collections
    test_firestore_connection.py   # Test script
  firestore.indexes.json           # Firestore index configuration
```

## Key Code Snippets

### backend/schemas/__init__.py

```python
from .clothing_item import ClothingItem
from .wear_log import WearLog

__all__ = ['ClothingItem', 'WearLog']
```

### backend/schemas/clothing_item.py

```python
from dataclasses import dataclass
from typing import Optional, List
from datetime import datetime
from google.cloud.firestore import SERVER_TIMESTAMP

@dataclass
class ClothingItem:
    type: str  # "shirt" or "pants"
    image_url: str
    embedding: List[float]
    created_at: datetime = SERVER_TIMESTAMP
    last_worn: Optional[datetime] = None
    wear_count: int = 0
    thumbnail_url: Optional[str] = None
    id: Optional[str] = None

    def to_dict(self):
        """Convert to Firestore document format"""
        return {
            'type': self.type,
            'image_url': self.image_url,
            'embedding': self.embedding,
            'created_at': self.created_at,
            'last_worn': self.last_worn,
            'wear_count': self.wear_count,
            'thumbnail_url': self.thumbnail_url
        }

    @staticmethod
    def from_dict(data: dict, doc_id: str = None):
        """Create ClothingItem from Firestore document"""
        item = ClothingItem(
            type=data['type'],
            image_url=data['image_url'],
            embedding=data['embedding'],
            created_at=data.get('created_at'),
            last_worn=data.get('last_worn'),
            wear_count=data.get('wear_count', 0),
            thumbnail_url=data.get('thumbnail_url')
        )
        item.id = doc_id
        return item

    def validate(self):
        """Validate item data"""
        assert self.type in ['shirt', 'pants'], f"Invalid type: {self.type}"
        assert len(self.embedding) == 1408, f"Invalid embedding length: {len(self.embedding)}"
        assert self.wear_count >= 0, f"Invalid wear_count: {self.wear_count}"
```

### backend/schemas/wear_log.py

```python
from dataclasses import dataclass
from datetime import datetime
from google.cloud.firestore import SERVER_TIMESTAMP
from typing import Optional

@dataclass
class WearLog:
    item_id: str
    item_type: str  # "shirt" or "pants"
    worn_at: datetime = SERVER_TIMESTAMP
    confidence_score: float = 1.0
    original_image_url: str = ""
    id: Optional[str] = None

    def to_dict(self):
        """Convert to Firestore document format"""
        return {
            'item_id': self.item_id,
            'item_type': self.item_type,
            'worn_at': self.worn_at,
            'confidence_score': self.confidence_score,
            'original_image_url': self.original_image_url
        }

    @staticmethod
    def from_dict(data: dict, doc_id: str = None):
        """Create WearLog from Firestore document"""
        log = WearLog(
            item_id=data['item_id'],
            item_type=data['item_type'],
            worn_at=data.get('worn_at'),
            confidence_score=data.get('confidence_score', 1.0),
            original_image_url=data.get('original_image_url', '')
        )
        log.id = doc_id
        return log

    def validate(self):
        """Validate log data"""
        assert self.item_type in ['shirt', 'pants'], f"Invalid type: {self.item_type}"
        assert 0.0 <= self.confidence_score <= 1.0, f"Invalid confidence: {self.confidence_score}"
```

### backend/scripts/init_firestore.py

```python
from google.cloud import firestore
import os

def initialize_firestore():
    """Initialize Firestore collections and verify setup"""
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))

    # Create collections by adding a dummy document (removed later)
    collections = ['clothing_items', 'wear_logs']

    for collection_name in collections:
        collection_ref = db.collection(collection_name)
        doc_ref = collection_ref.document('_init')
        doc_ref.set({'initialized': True, 'timestamp': firestore.SERVER_TIMESTAMP})
        doc_ref.delete()
        print(f"✓ Collection '{collection_name}' initialized")

    print("\n✓ Firestore setup complete!")

if __name__ == '__main__':
    initialize_firestore()
```

### backend/firestore.indexes.json

```json
{
  "indexes": [
    {
      "collectionGroup": "clothing_items",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "type",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "wear_count",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "clothing_items",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "last_worn",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "wear_logs",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "item_id",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "worn_at",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "wear_logs",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "worn_at",
          "order": "DESCENDING"
        }
      ]
    }
  ]
}
```

## Acceptance Criteria

- [ ] Firestore database created in GCP project
- [ ] Collections 'clothing_items' and 'wear_logs' initialized
- [ ] Python script can read/write to Firestore successfully
- [ ] Schema models defined and importable
- [ ] Test data (1 item, 1 log) can be inserted and retrieved
- [ ] Indexes configured (verify in Firestore console)
- [ ] Validation methods work correctly

## Verification

### Test Script

```python
# backend/scripts/test_firestore_connection.py
from google.cloud import firestore
from schemas.clothing_item import ClothingItem
from schemas.wear_log import WearLog
import os

def test_firestore():
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))

    # Test: Create a clothing item
    test_item = ClothingItem(
        type='shirt',
        image_url='gs://test-bucket/test.jpg',
        embedding=[0.1] * 1408,
        wear_count=0
    )

    doc_ref = db.collection('clothing_items').add(test_item.to_dict())
    item_id = doc_ref[1].id
    print(f"✓ Created test item: {item_id}")

    # Test: Retrieve item
    retrieved = db.collection('clothing_items').document(item_id).get()
    retrieved_item = ClothingItem.from_dict(retrieved.to_dict(), item_id)
    print(f"✓ Retrieved item: {retrieved_item.type}")

    # Test: Create wear log
    test_log = WearLog(
        item_id=item_id,
        item_type='shirt',
        confidence_score=0.95
    )

    log_ref = db.collection('wear_logs').add(test_log.to_dict())
    log_id = log_ref[1].id
    print(f"✓ Created test log: {log_id}")

    # Cleanup
    db.collection('clothing_items').document(item_id).delete()
    db.collection('wear_logs').document(log_id).delete()
    print("✓ Cleaned up test data")

    print("\n✓ All Firestore tests passed!")

if __name__ == '__main__':
    test_firestore()
```

Run test:
```bash
python backend/scripts/test_firestore_connection.py
```

### Deploy Indexes

```bash
gcloud firestore indexes create --database=(default) --file=backend/firestore.indexes.json
```
