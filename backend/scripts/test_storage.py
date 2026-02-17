import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from dotenv import load_dotenv
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

from storage.storage_client import StorageClient
from PIL import Image
import io


def test_storage():
    """Test Cloud Storage operations."""
    client = StorageClient()

    # Create test image
    test_image = Image.new('RGB', (640, 480), color='red')
    img_bytes = io.BytesIO()
    test_image.save(img_bytes, format='JPEG')
    test_data = img_bytes.getvalue()

    print("Testing Cloud Storage operations...")

    # Test: Upload original photo
    original_url = client.upload_original_photo(test_data, user_id='test')
    print(f"Uploaded original photo: {original_url}")

    # Test: Upload cropped item
    cropped_url = client.upload_cropped_item(test_data, 'shirt', 'test123')
    print(f"Uploaded cropped item: {cropped_url}")

    # Test: Download image
    downloaded = client.download_image(cropped_url)
    assert len(downloaded) > 0, "Downloaded image is empty"
    print(f"Downloaded image: {len(downloaded)} bytes")

    # Test: Generate signed URL
    signed_url = client.get_signed_url(cropped_url, expiration_minutes=5)
    assert signed_url.startswith('https://'), "Invalid signed URL"
    print("Generated signed URL (valid for 5 minutes)")

    # Test: List items
    items = client.list_items('shirt')
    print(f"Listed {len(items)} shirt items")

    # Test: Delete images
    client.delete_image(original_url)
    client.delete_image(cropped_url)
    print("Cleaned up test images")

    print("\nAll Cloud Storage tests passed!")


if __name__ == '__main__':
    test_storage()
