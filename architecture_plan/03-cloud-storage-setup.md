# Step 3: Cloud Storage Setup

## Goal

Set up Cloud Storage bucket for clothing images with proper structure, lifecycle policies, and access control.

## ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│              Cloud Storage Bucket                        │
│      uniform-dist-XXXXX.appspot.com                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  /original-photos/                                      │
│  ┌────────────────────────────────────────────┐        │
│  │  {timestamp}_{userId}_full.jpg             │        │
│  │  - Full outfit photos from camera          │        │
│  │  - Kept for audit/debugging                │        │
│  └────────────────────────────────────────────┘        │
│           │                                             │
│           ▼ (Gemini detects objects)                   │
│                                                          │
│  /cropped-items/                                        │
│  ┌────────────────────────────────────────────┐        │
│  │  /shirts/                                  │        │
│  │    - {itemId}_shirt.jpg                   │        │
│  │  /pants/                                   │        │
│  │    - {itemId}_pants.jpg                   │        │
│  │  - Individual clothing items               │        │
│  │  - Used for display and embedding          │        │
│  └────────────────────────────────────────────┘        │
│                                                          │
│  Lifecycle Policy:                                      │
│  ┌────────────────────────────────────────────┐        │
│  │  - original-photos: 30 days → Delete       │        │
│  │  - cropped-items: Keep indefinitely        │        │
│  └────────────────────────────────────────────┘        │
│                                                          │
│  Access Control:                                        │
│  ┌────────────────────────────────────────────┐        │
│  │  - Uniform access: Fine-grained            │        │
│  │  - Public read: Disabled                   │        │
│  │  - Service account: Read/Write             │        │
│  └────────────────────────────────────────────┘        │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## Data Schemas

N/A (Storage structure only)

## API Endpoint Signatures

N/A

## File Structure

```
backend/
  storage/
    __init__.py
    storage_client.py              # Cloud Storage wrapper
    image_utils.py                 # Image processing utilities
  scripts/
    setup_storage.py               # Initialize bucket and folders
    test_storage.py                # Upload/download test
```

## Key Code Snippets

### backend/storage/__init__.py

```python
from .storage_client import StorageClient

__all__ = ['StorageClient']
```

### backend/storage/storage_client.py

```python
from google.cloud import storage
import os
from datetime import datetime, timedelta
from PIL import Image
import io

class StorageClient:
    def __init__(self):
        self.client = storage.Client(project=os.getenv('GCP_PROJECT_ID'))
        self.bucket_name = os.getenv('STORAGE_BUCKET')
        self.bucket = self.client.bucket(self.bucket_name)

    def upload_original_photo(self, image_bytes: bytes, user_id: str = 'default') -> str:
        """
        Upload full outfit photo

        Args:
            image_bytes: Image data as bytes
            user_id: User identifier (default: 'default')

        Returns:
            gs:// URL to uploaded image
        """
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        blob_name = f"original-photos/{timestamp}_{user_id}_full.jpg"

        blob = self.bucket.blob(blob_name)
        blob.upload_from_string(image_bytes, content_type='image/jpeg')

        return f"gs://{self.bucket_name}/{blob_name}"

    def upload_cropped_item(self, image_bytes: bytes, item_type: str,
                           item_id: str) -> str:
        """
        Upload cropped clothing item

        Args:
            image_bytes: Cropped image data
            item_type: 'shirt' or 'pants'
            item_id: Unique item identifier

        Returns:
            gs:// URL to uploaded image
        """
        blob_name = f"cropped-items/{item_type}s/{item_id}_{item_type}.jpg"

        blob = self.bucket.blob(blob_name)
        blob.upload_from_string(image_bytes, content_type='image/jpeg')

        return f"gs://{self.bucket_name}/{blob_name}"

    def download_image(self, gs_url: str) -> bytes:
        """
        Download image from Cloud Storage

        Args:
            gs_url: gs://bucket/path format URL

        Returns:
            Image bytes
        """
        # Parse gs://bucket/path format
        path = gs_url.replace(f"gs://{self.bucket_name}/", "")
        blob = self.bucket.blob(path)
        return blob.download_as_bytes()

    def get_signed_url(self, gs_url: str, expiration_minutes: int = 60) -> str:
        """
        Generate signed URL for image access

        Args:
            gs_url: gs://bucket/path format URL
            expiration_minutes: URL validity period (default: 60 minutes)

        Returns:
            HTTPS signed URL for temporary access
        """
        path = gs_url.replace(f"gs://{self.bucket_name}/", "")
        blob = self.bucket.blob(path)

        url = blob.generate_signed_url(
            version="v4",
            expiration=timedelta(minutes=expiration_minutes),
            method="GET"
        )
        return url

    def delete_image(self, gs_url: str) -> bool:
        """
        Delete image from Cloud Storage

        Args:
            gs_url: gs://bucket/path format URL

        Returns:
            True if deleted successfully
        """
        try:
            path = gs_url.replace(f"gs://{self.bucket_name}/", "")
            blob = self.bucket.blob(path)
            blob.delete()
            return True
        except Exception as e:
            print(f"Error deleting {gs_url}: {e}")
            return False

    def list_items(self, item_type: str = None) -> list:
        """
        List all stored items

        Args:
            item_type: Filter by 'shirt' or 'pants' (optional)

        Returns:
            List of gs:// URLs
        """
        prefix = f"cropped-items/{item_type}s/" if item_type else "cropped-items/"
        blobs = self.bucket.list_blobs(prefix=prefix)

        return [f"gs://{self.bucket_name}/{blob.name}" for blob in blobs]
```

### backend/storage/image_utils.py

```python
from PIL import Image
import io

def resize_image(image_bytes: bytes, max_size: tuple = (1024, 1024)) -> bytes:
    """
    Resize image to maximum dimensions while maintaining aspect ratio

    Args:
        image_bytes: Original image bytes
        max_size: Maximum (width, height)

    Returns:
        Resized image bytes
    """
    image = Image.open(io.BytesIO(image_bytes))
    image.thumbnail(max_size, Image.Resampling.LANCZOS)

    output = io.BytesIO()
    image.save(output, format='JPEG', quality=90)
    return output.getvalue()

def create_thumbnail(image_bytes: bytes, size: tuple = (200, 200)) -> bytes:
    """
    Create square thumbnail

    Args:
        image_bytes: Original image bytes
        size: Thumbnail dimensions

    Returns:
        Thumbnail image bytes
    """
    image = Image.open(io.BytesIO(image_bytes))

    # Create square crop from center
    width, height = image.size
    min_dim = min(width, height)

    left = (width - min_dim) // 2
    top = (height - min_dim) // 2
    right = left + min_dim
    bottom = top + min_dim

    cropped = image.crop((left, top, right, bottom))
    cropped.thumbnail(size, Image.Resampling.LANCZOS)

    output = io.BytesIO()
    cropped.save(output, format='JPEG', quality=85)
    return output.getvalue()
```

### backend/scripts/setup_storage.py

```python
from google.cloud import storage
import os

def setup_storage_bucket():
    """Initialize Cloud Storage bucket and structure"""
    client = storage.Client(project=os.getenv('GCP_PROJECT_ID'))
    bucket_name = os.getenv('STORAGE_BUCKET')

    # Get or create bucket
    bucket = client.bucket(bucket_name)
    if not bucket.exists():
        bucket = client.create_bucket(bucket_name, location='us-central1')
        print(f"✓ Bucket '{bucket_name}' created")
    else:
        print(f"✓ Bucket '{bucket_name}' already exists")

    # Set lifecycle policy
    bucket.add_lifecycle_delete_rule(
        age=30,
        matches_prefix=['original-photos/']
    )
    bucket.patch()
    print("✓ Lifecycle policy set (original-photos: 30-day retention)")

    # Create folder structure with placeholder files
    folders = ['original-photos/', 'cropped-items/shirts/', 'cropped-items/pants/']
    for folder in folders:
        blob = bucket.blob(f"{folder}.keep")
        blob.upload_from_string("")
        print(f"✓ Folder '{folder}' created")

    print("\n✓ Cloud Storage setup complete!")

if __name__ == '__main__':
    setup_storage_bucket()
```

### backend/scripts/test_storage.py

```python
from storage.storage_client import StorageClient
from PIL import Image
import io

def test_storage():
    """Test Cloud Storage operations"""
    client = StorageClient()

    # Create test image
    test_image = Image.new('RGB', (640, 480), color='red')
    img_bytes = io.BytesIO()
    test_image.save(img_bytes, format='JPEG')
    test_data = img_bytes.getvalue()

    print("Testing Cloud Storage operations...")

    # Test: Upload original photo
    original_url = client.upload_original_photo(test_data, user_id='test')
    print(f"✓ Uploaded original photo: {original_url}")

    # Test: Upload cropped item
    cropped_url = client.upload_cropped_item(test_data, 'shirt', 'test123')
    print(f"✓ Uploaded cropped item: {cropped_url}")

    # Test: Download image
    downloaded = client.download_image(cropped_url)
    assert len(downloaded) > 0, "Downloaded image is empty"
    print(f"✓ Downloaded image: {len(downloaded)} bytes")

    # Test: Generate signed URL
    signed_url = client.get_signed_url(cropped_url, expiration_minutes=5)
    assert signed_url.startswith('https://'), "Invalid signed URL"
    print(f"✓ Generated signed URL (valid for 5 minutes)")

    # Test: List items
    items = client.list_items('shirt')
    print(f"✓ Listed {len(items)} shirt items")

    # Test: Delete images
    client.delete_image(original_url)
    client.delete_image(cropped_url)
    print("✓ Cleaned up test images")

    print("\n✓ All Cloud Storage tests passed!")

if __name__ == '__main__':
    test_storage()
```

## Acceptance Criteria

- [ ] Cloud Storage bucket created and accessible
- [ ] Folder structure (original-photos/, cropped-items/shirts/, cropped-items/pants/) exists
- [ ] Lifecycle policy configured (30-day retention for original-photos)
- [ ] StorageClient class can upload/download images
- [ ] Test image uploaded and retrieved successfully
- [ ] Signed URLs generated correctly with 60-minute expiration
- [ ] Lifecycle policy verified in GCP Console
- [ ] All test cases pass

## Setup Instructions

1. **Create Storage Bucket**:
   ```bash
   gsutil mb -c STANDARD -l us-central1 gs://uniform-dist-XXXXX.appspot.com
   ```

2. **Set Bucket Permissions**:
   ```bash
   gsutil iam ch serviceAccount:uniform-dist-sa@uniform-dist-XXXXX.iam.gserviceaccount.com:roles/storage.objectAdmin gs://uniform-dist-XXXXX.appspot.com
   ```

3. **Run Setup Script**:
   ```bash
   python backend/scripts/setup_storage.py
   ```

4. **Verify Setup**:
   ```bash
   gsutil ls gs://uniform-dist-XXXXX.appspot.com/
   ```

5. **Run Tests**:
   ```bash
   python backend/scripts/test_storage.py
   ```

## Verification

Check bucket contents:
```bash
gsutil ls -r gs://uniform-dist-XXXXX.appspot.com/
```

Check lifecycle policy:
```bash
gsutil lifecycle get gs://uniform-dist-XXXXX.appspot.com/
```

Expected output:
```json
{
  "lifecycle": {
    "rule": [
      {
        "action": {"type": "Delete"},
        "condition": {
          "age": 30,
          "matchesPrefix": ["original-photos/"]
        }
      }
    ]
  }
}
```
