# Step 7: Confirm and Log Endpoints

## Goal

Create Cloud Function endpoints to confirm matches and log wear events, updating Firestore with new items or wear logs.

## ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│         Cloud Function: confirm_and_log                          │
│         Endpoints: /confirm-match, /add-new-item                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  POST /confirm-match                                            │
│  ┌──────────────────────────────────────────┐                  │
│  │  Body: {                                 │                  │
│  │    "item_id": "abc123",                  │                  │
│  │    "item_type": "shirt",                 │                  │
│  │    "original_photo_url": "gs://..."      │                  │
│  │  }                                       │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  1. Update clothing_items document       │                  │
│  │     - Increment wear_count               │                  │
│  │     - Update last_worn timestamp         │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  2. Create wear_logs document            │                  │
│  │     - item_id, worn_at, confidence       │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  3. Return updated item stats            │                  │
│  └──────────────────────────────────────────┘                  │
│                                                                  │
│                                                                  │
│  POST /add-new-item                                             │
│  ┌──────────────────────────────────────────┐                  │
│  │  Body: {                                 │                  │
│  │    "item_type": "pants",                 │                  │
│  │    "cropped_image_url": "gs://...",      │                  │
│  │    "embedding": [0.1, 0.2, ...],         │                  │
│  │    "original_photo_url": "gs://...",     │                  │
│  │    "log_wear": true                      │                  │
│  │  }                                       │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  1. Create clothing_items document       │                  │
│  │     - All item fields                    │                  │
│  │     - wear_count = 1 (if logging)        │                  │
│  │     - last_worn = now (if logging)       │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  2. Create wear_log (if requested)       │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  3. Return new item ID                   │                  │
│  └──────────────────────────────────────────┘                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Schemas

### ConfirmMatchRequest

```typescript
interface ConfirmMatchRequest {
  item_id: string;
  item_type: 'shirt' | 'pants';
  original_photo_url: string;
  similarity_score?: number;
}
```

### AddNewItemRequest

```typescript
interface AddNewItemRequest {
  item_type: 'shirt' | 'pants';
  cropped_image_url: string;  // gs:// URL
  embedding: number[];         // 1408 dimensions
  original_photo_url: string;
  log_wear: boolean;           // Whether to log as worn today
}
```

### Responses

```typescript
interface ConfirmMatchResponse {
  success: boolean;
  item_id: string;
  wear_count: number;
  last_worn: string;  // ISO timestamp
}

interface AddNewItemResponse {
  success: boolean;
  item_id: string;
}
```

## API Endpoint Signatures

### POST /confirm-match

Confirms user accepted the match, logs wear event

**Request**:
```json
{
  "item_id": "abc123",
  "item_type": "shirt",
  "original_photo_url": "gs://bucket/original-photos/...",
  "similarity_score": 0.92
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "item_id": "abc123",
  "wear_count": 5,
  "last_worn": "2024-01-15T10:30:00Z"
}
```

### POST /add-new-item

Adds new clothing item to database with optional wear log

**Request**:
```json
{
  "item_type": "pants",
  "cropped_image_url": "gs://bucket/cropped-items/pants/...",
  "embedding": [0.1, 0.2, ...],
  "original_photo_url": "gs://bucket/original-photos/...",
  "log_wear": true
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "item_id": "xyz789"
}
```

## File Structure

```
backend/
  functions/
    confirm_match.py               # Confirm match logic
    add_new_item.py                # Add new item logic
    main.py                        # Update with new endpoints
```

## Key Code Snippets

### backend/functions/confirm_match.py

```python
import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from google.cloud import firestore
from datetime import datetime

def confirm_match(item_id: str, item_type: str, original_photo_url: str,
                  similarity_score: float = None) -> dict:
    """
    Confirm match and log wear event

    Args:
        item_id: Clothing item ID
        item_type: 'shirt' or 'pants'
        original_photo_url: gs:// URL to original photo
        similarity_score: Match confidence (optional)

    Returns:
        Dict with updated item stats
    """
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))

    # Update clothing item
    item_ref = db.collection('clothing_items').document(item_id)
    item_ref.update({
        'wear_count': firestore.Increment(1),
        'last_worn': firestore.SERVER_TIMESTAMP
    })

    # Create wear log
    wear_log_data = {
        'item_id': item_id,
        'item_type': item_type,
        'worn_at': firestore.SERVER_TIMESTAMP,
        'confidence_score': similarity_score or 1.0,
        'original_image_url': original_photo_url
    }

    db.collection('wear_logs').add(wear_log_data)

    # Get updated item data
    item_data = item_ref.get().to_dict()

    return {
        'success': True,
        'item_id': item_id,
        'wear_count': item_data['wear_count'],
        'last_worn': item_data['last_worn'].isoformat() if item_data.get('last_worn') else None
    }
```

### backend/functions/add_new_item.py

```python
import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from google.cloud import firestore
from datetime import datetime

def add_new_item(item_type: str, cropped_image_url: str,
                 embedding: list, original_photo_url: str,
                 log_wear: bool = False) -> dict:
    """
    Add new clothing item to database

    Args:
        item_type: 'shirt' or 'pants'
        cropped_image_url: gs:// URL to cropped image
        embedding: 1408-dimensional embedding vector
        original_photo_url: gs:// URL to original photo
        log_wear: Whether to log wear event

    Returns:
        Dict with new item ID
    """
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))

    # Create new item
    item_data = {
        'type': item_type,
        'image_url': cropped_image_url,
        'embedding': embedding,
        'created_at': firestore.SERVER_TIMESTAMP,
        'last_worn': firestore.SERVER_TIMESTAMP if log_wear else None,
        'wear_count': 1 if log_wear else 0
    }

    # Add to Firestore
    doc_ref = db.collection('clothing_items').add(item_data)
    item_id = doc_ref[1].id

    # Log wear if requested
    if log_wear:
        wear_log_data = {
            'item_id': item_id,
            'item_type': item_type,
            'worn_at': firestore.SERVER_TIMESTAMP,
            'confidence_score': 1.0,
            'original_image_url': original_photo_url
        }
        db.collection('wear_logs').add(wear_log_data)

    return {
        'success': True,
        'item_id': item_id
    }
```

### backend/functions/main.py (add endpoints)

```python
import functions_framework
from flask import jsonify
import base64
from process_outfit import process_outfit_image
from confirm_match import confirm_match
from add_new_item import add_new_item

@functions_framework.http
def process_outfit(request):
    """POST /process-outfit"""
    if request.method == 'OPTIONS':
        return ('', 204, {'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'POST', 'Access-Control-Allow-Headers': 'Content-Type'})

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        request_json = request.get_json()
        if not request_json or 'image' not in request_json:
            return jsonify({'success': False, 'error': 'Missing image data'}), 400, headers

        image_base64 = request_json['image']
        image_bytes = base64.b64decode(image_base64)

        result = process_outfit_image(image_bytes)

        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500, headers


@functions_framework.http
def confirm_match_handler(request):
    """POST /confirm-match"""
    if request.method == 'OPTIONS':
        return ('', 204, {'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'POST', 'Access-Control-Allow-Headers': 'Content-Type'})

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        data = request.get_json()

        if not data or 'item_id' not in data or 'item_type' not in data or 'original_photo_url' not in data:
            return jsonify({'success': False, 'error': 'Missing required fields'}), 400, headers

        result = confirm_match(
            data['item_id'],
            data['item_type'],
            data['original_photo_url'],
            data.get('similarity_score')
        )
        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500, headers


@functions_framework.http
def add_new_item_handler(request):
    """POST /add-new-item"""
    if request.method == 'OPTIONS':
        return ('', 204, {'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'POST', 'Access-Control-Allow-Headers': 'Content-Type'})

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        data = request.get_json()

        required_fields = ['item_type', 'cropped_image_url', 'embedding', 'original_photo_url']
        if not data or not all(field in data for field in required_fields):
            return jsonify({'success': False, 'error': 'Missing required fields'}), 400, headers

        result = add_new_item(
            data['item_type'],
            data['cropped_image_url'],
            data['embedding'],
            data['original_photo_url'],
            data.get('log_wear', False)
        )
        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500, headers
```

### backend/deploy.sh (updated)

```bash
#!/bin/bash

set -e

echo "Deploying Cloud Functions..."

# Deploy process-outfit
gcloud functions deploy process-outfit \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source=. \
  --entry-point=process_outfit \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=60s \
  --memory=1GB \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID,GEMINI_API_KEY=$GEMINI_API_KEY,STORAGE_BUCKET=$STORAGE_BUCKET,GCP_REGION=$GCP_REGION

echo "✓ process-outfit deployed"

# Deploy confirm-match
gcloud functions deploy confirm-match \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source=. \
  --entry-point=confirm_match_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=10s \
  --memory=256MB \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID

echo "✓ confirm-match deployed"

# Deploy add-new-item
gcloud functions deploy add-new-item \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source=. \
  --entry-point=add_new_item_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=10s \
  --memory=256MB \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID

echo "✓ add-new-item deployed"

echo ""
echo "✓ All functions deployed!"
```

## Acceptance Criteria

- [ ] Both endpoints deployed successfully
- [ ] Confirming match increments wear_count
- [ ] Confirming match updates last_worn timestamp
- [ ] Confirming match creates wear_logs entry
- [ ] Adding new item creates clothing_items document
- [ ] Adding new item with log_wear=true creates wear log
- [ ] Error handling for missing item_id, invalid data
- [ ] Firestore data validates against schemas
- [ ] Unit tests pass for both functions
- [ ] Concurrent requests handled correctly

## Verification

### Test confirm-match

```bash
FUNCTION_URL=$(gcloud functions describe confirm-match --region=us-central1 --gen2 --format="value(serviceConfig.uri)")

curl -X POST $FUNCTION_URL/confirm-match \
  -H "Content-Type: application/json" \
  -d '{
    "item_id": "test123",
    "item_type": "shirt",
    "original_photo_url": "gs://bucket/test.jpg",
    "similarity_score": 0.92
  }'
```

### Test add-new-item

```bash
FUNCTION_URL=$(gcloud functions describe add-new-item --region=us-central1 --gen2 --format="value(serviceConfig.uri)")

curl -X POST $FUNCTION_URL/add-new-item \
  -H "Content-Type: application/json" \
  -d '{
    "item_type": "pants",
    "cropped_image_url": "gs://bucket/cropped-items/pants/test.jpg",
    "embedding": [0.1, 0.2],
    "original_photo_url": "gs://bucket/original-photos/test.jpg",
    "log_wear": true
  }'
```

### Test Script

```python
# backend/scripts/test_endpoints.py
import requests
import os

def test_confirm_match():
    """Test confirm-match endpoint"""
    function_url = os.getenv('CONFIRM_MATCH_URL')

    response = requests.post(
        f"{function_url}/confirm-match",
        json={
            "item_id": "test123",
            "item_type": "shirt",
            "original_photo_url": "gs://bucket/test.jpg",
            "similarity_score": 0.92
        }
    )

    print(f"✓ confirm-match status: {response.status_code}")
    print(f"  Response: {response.json()}")

def test_add_new_item():
    """Test add-new-item endpoint"""
    function_url = os.getenv('ADD_NEW_ITEM_URL')

    response = requests.post(
        f"{function_url}/add-new-item",
        json={
            "item_type": "pants",
            "cropped_image_url": "gs://bucket/test.jpg",
            "embedding": [0.1] * 1408,
            "original_photo_url": "gs://bucket/test.jpg",
            "log_wear": True
        }
    )

    print(f"✓ add-new-item status: {response.status_code}")
    print(f"  Response: {response.json()}")

if __name__ == '__main__':
    test_confirm_match()
    test_add_new_item()
```

Run test:
```bash
export CONFIRM_MATCH_URL=https://us-central1-uniform-dist-XXXXX.cloudfunctions.net
export ADD_NEW_ITEM_URL=https://us-central1-uniform-dist-XXXXX.cloudfunctions.net
python backend/scripts/test_endpoints.py
```

### Check Firestore

```bash
# List clothing items
gcloud firestore collections list

# Query items
gcloud firestore documents list clothing_items

# Query wear logs
gcloud firestore documents list wear_logs
```
