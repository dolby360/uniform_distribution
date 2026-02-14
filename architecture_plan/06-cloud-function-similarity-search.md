# Step 6: Cloud Function - Similarity Search

## Goal

Create Cloud Function HTTP endpoint to process outfit photos, detect items, generate embeddings, perform similarity search, and return match results.

## ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│              Cloud Function: process_outfit                      │
│              Endpoint: /process-outfit                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  POST /process-outfit                                           │
│  Body: { "image": "base64_encoded_image" }                      │
│                                                                  │
│  ┌──────────────────────────────────────────┐                  │
│  │  1. Upload original photo to Storage     │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  2. Gemini Vision: Detect shirt & pants  │                  │
│  │     Returns bounding boxes                │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  3. Crop images for each detected item   │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  4. Generate embeddings (Vertex AI)      │                  │
│  │     - Shirt embedding (1408-dim)          │                  │
│  │     - Pants embedding (1408-dim)          │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  5. Load existing items from Firestore   │                  │
│  │     Filter by type (shirts/pants)         │                  │
│  │     In-memory list (<500 items)           │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  6. Cosine similarity search              │                  │
│  │     Compare against all existing items    │                  │
│  │     Threshold: 0.85                       │                  │
│  └──────────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  ┌──────────────────────────────────────────┐                  │
│  │  7. Return results                        │                  │
│  │     {                                     │                  │
│  │       "shirt": {                          │                  │
│  │         "matched": true/false,            │                  │
│  │         "item_id": "...",                 │                  │
│  │         "similarity": 0.92,               │                  │
│  │         "image_url": "...",               │                  │
│  │         "cropped_url": "..."              │                  │
│  │       },                                  │                  │
│  │       "pants": { ... }                    │                  │
│  │     }                                     │                  │
│  └──────────────────────────────────────────┘                  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Schemas

### Request

```typescript
interface ProcessOutfitRequest {
  image: string;  // base64-encoded JPEG
}
```

### Response

```typescript
interface ItemMatchResult {
  matched: boolean;
  item_id?: string;           // If matched
  similarity?: number;        // If matched (0.85 - 1.0)
  image_url?: string;         // Signed URL to view existing item
  cropped_url: string;        // Signed URL to newly cropped item
  embedding: number[];        // For new items
}

interface ProcessOutfitResponse {
  success: boolean;
  shirt: ItemMatchResult | null;
  pants: ItemMatchResult | null;
  original_photo_url: string;
  error?: string;
}
```

## API Endpoint Signatures

### POST /process-outfit

**Request**:
```json
{
  "image": "base64_encoded_image_data"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "original_photo_url": "https://storage.googleapis.com/...",
  "shirt": {
    "matched": true,
    "item_id": "abc123",
    "similarity": 0.92,
    "image_url": "https://storage.googleapis.com/...",
    "cropped_url": "https://storage.googleapis.com/..."
  },
  "pants": {
    "matched": false,
    "cropped_url": "https://storage.googleapis.com/...",
    "embedding": [0.1, 0.2, ...]
  }
}
```

**Error Response** (400/500):
```json
{
  "success": false,
  "error": "Error message"
}
```

**Timeout**: 60 seconds
**Memory**: 1GB
**CORS**: Enabled

## File Structure

```
backend/
  functions/
    main.py                        # Cloud Function entry point
    process_outfit.py              # Main processing logic
    requirements.txt               # Dependencies for Cloud Functions
  .gcloudignore                    # Deployment exclusions
  deploy.sh                        # Deployment script
```

## Key Code Snippets

### backend/functions/main.py

```python
import functions_framework
from flask import jsonify
import base64
from process_outfit import process_outfit_image

@functions_framework.http
def process_outfit(request):
    """
    HTTP Cloud Function: Process outfit photo

    POST /process-outfit
    Body: { "image": "base64_encoded_image" }
    """
    # CORS headers
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST',
            'Access-Control-Allow-Headers': 'Content-Type',
        }
        return ('', 204, headers)

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        # Parse request
        request_json = request.get_json()
        if not request_json or 'image' not in request_json:
            return jsonify({'success': False, 'error': 'Missing image data'}), 400, headers

        # Decode base64 image
        image_base64 = request_json['image']
        image_bytes = base64.b64decode(image_base64)

        # Process outfit
        result = process_outfit_image(image_bytes)

        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500, headers
```

### backend/functions/process_outfit.py

```python
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from storage.storage_client import StorageClient
from gemini.vision_detector import VisionDetector
from utils.image_cropper import crop_clothing_item
from embeddings.vertex_embedder import VertexEmbedder
from embeddings.similarity import find_most_similar
from google.cloud import firestore
import uuid

def process_outfit_image(image_bytes: bytes) -> dict:
    """
    Main processing pipeline for outfit photo

    Args:
        image_bytes: Image data as bytes

    Returns:
        Dict with match results for shirt and pants
    """
    storage = StorageClient()
    detector = VisionDetector()
    embedder = VertexEmbedder()
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))

    # 1. Upload original photo
    original_url = storage.upload_original_photo(image_bytes)

    # 2. Detect clothing items
    detection_result = detector.detect_clothing(image_bytes)

    result = {
        'success': True,
        'original_photo_url': storage.get_signed_url(original_url),
        'shirt': None,
        'pants': None
    }

    # 3. Process each detected item
    for item_type in ['shirt', 'pants']:
        detection = detection_result.get(item_type)

        if not detection or not detection.get('detected'):
            continue

        # Crop item
        cropped_bytes = crop_clothing_item(
            image_bytes,
            detection['bounding_box']
        )

        # Generate embedding
        embedding = embedder.generate_embedding(cropped_bytes)

        # Upload cropped image (temporary, will be saved if new item)
        temp_id = str(uuid.uuid4())
        cropped_url = storage.upload_cropped_item(
            cropped_bytes, item_type, temp_id
        )

        # Search for similar items
        existing_items = db.collection('clothing_items')\
            .where('type', '==', item_type)\
            .stream()

        candidates = [
            (item.id, item.to_dict()['embedding'])
            for item in existing_items
        ]

        match = find_most_similar(embedding, candidates, threshold=0.85)

        if match:
            item_id, similarity = match
            item_doc = db.collection('clothing_items').document(item_id).get()
            item_data = item_doc.to_dict()

            result[item_type] = {
                'matched': True,
                'item_id': item_id,
                'similarity': float(similarity),
                'image_url': storage.get_signed_url(item_data['image_url']),
                'cropped_url': storage.get_signed_url(cropped_url)
            }
        else:
            result[item_type] = {
                'matched': False,
                'cropped_url': storage.get_signed_url(cropped_url),
                'embedding': embedding
            }

    return result
```

### backend/functions/requirements.txt

```
functions-framework==3.*
google-cloud-firestore==2.14.0
google-cloud-storage==2.14.0
google-cloud-aiplatform==1.38.0
google-generativeai==0.3.2
Pillow==10.1.0
numpy==1.24.3
```

### backend/.gcloudignore

```
# Python
__pycache__/
*.py[cod]
*$py.class
*.so
.Python
env/
venv/
ENV/
*.egg-info/
dist/
build/

# Tests
tests/
*_test.py
test_*.py

# IDE
.vscode/
.idea/
*.swp
*.swo

# Misc
.env
.DS_Store
*.log
```

### backend/deploy.sh

```bash
#!/bin/bash

# Deploy Cloud Function: process-outfit

set -e

echo "Deploying process-outfit Cloud Function..."

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

echo "✓ Deployment complete!"
echo ""
echo "Function URL:"
gcloud functions describe process-outfit --region=us-central1 --gen2 --format="value(serviceConfig.uri)"
```

## Acceptance Criteria

- [ ] Cloud Function deployed successfully
- [ ] HTTP endpoint accessible and returns 200 OK
- [ ] Test image with shirt and pants returns detection results
- [ ] Matched items return similarity scores > 0.85
- [ ] New items return embeddings and cropped image URLs
- [ ] Signed URLs are valid and images accessible
- [ ] Error handling for invalid images, API failures
- [ ] Response time < 30 seconds for typical outfit photo
- [ ] CORS headers allow frontend access
- [ ] Memory usage stays within 1GB limit
- [ ] Concurrent requests handled properly

## Setup Instructions

1. **Prepare Deployment**:
   ```bash
   cd backend
   chmod +x deploy.sh
   ```

2. **Set Environment Variables**:
   ```bash
   export GCP_PROJECT_ID=uniform-dist-XXXXX
   export GEMINI_API_KEY=your-gemini-key
   export STORAGE_BUCKET=uniform-dist-XXXXX.appspot.com
   export GCP_REGION=us-central1
   ```

3. **Deploy Function**:
   ```bash
   ./deploy.sh
   ```

4. **Get Function URL**:
   ```bash
   gcloud functions describe process-outfit --region=us-central1 --gen2 --format="value(serviceConfig.uri)"
   ```

## Verification

### Test with curl

```bash
# Create base64-encoded test image
python -c "
import base64
from PIL import Image
import io

img = Image.new('RGB', (640, 480), color='blue')
img_bytes = io.BytesIO()
img.save(img_bytes, format='JPEG')
encoded = base64.b64encode(img_bytes.getvalue()).decode('utf-8')
print(encoded)
" > test_image_base64.txt

FUNCTION_URL=$(gcloud functions describe process-outfit --region=us-central1 --gen2 --format="value(serviceConfig.uri)")

curl -X POST $FUNCTION_URL/process-outfit \
  -H "Content-Type: application/json" \
  -d "{\"image\": \"$(cat test_image_base64.txt)\"}"
```

### Expected Response

```json
{
  "success": true,
  "original_photo_url": "https://storage.googleapis.com/...",
  "shirt": {
    "matched": false,
    "cropped_url": "https://storage.googleapis.com/...",
    "embedding": [0.123, -0.456, ...]
  },
  "pants": {
    "matched": false,
    "cropped_url": "https://storage.googleapis.com/...",
    "embedding": [0.789, 0.234, ...]
  }
}
```

### Test Script

```python
# backend/scripts/test_cloud_function.py
import requests
import base64
from PIL import Image
import io
import os

def test_process_outfit():
    """Test process-outfit Cloud Function"""

    # Get function URL
    function_url = os.getenv('FUNCTION_URL')
    if not function_url:
        print("Error: Set FUNCTION_URL environment variable")
        return

    # Create test image
    test_image = Image.new('RGB', (640, 480), color='red')
    img_bytes = io.BytesIO()
    test_image.save(img_bytes, format='JPEG')
    image_base64 = base64.b64encode(img_bytes.getvalue()).decode('utf-8')

    # Call function
    print("Calling Cloud Function...")
    response = requests.post(
        f"{function_url}/process-outfit",
        json={"image": image_base64},
        timeout=60
    )

    print(f"Status Code: {response.status_code}")

    if response.status_code == 200:
        result = response.json()
        print(f"✓ Success: {result['success']}")
        print(f"✓ Shirt: {'matched' if result.get('shirt', {}).get('matched') else 'new'}")
        print(f"✓ Pants: {'matched' if result.get('pants', {}).get('matched') else 'new'}")
        print("\n✓ Cloud Function test passed!")
    else:
        print(f"✗ Error: {response.text}")

if __name__ == '__main__':
    test_process_outfit()
```

Run test:
```bash
export FUNCTION_URL=https://us-central1-uniform-dist-XXXXX.cloudfunctions.net
python backend/scripts/test_cloud_function.py
```

### Monitor Logs

```bash
gcloud functions logs read process-outfit --region=us-central1 --gen2 --limit=50
```
