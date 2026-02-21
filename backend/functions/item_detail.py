import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from google.cloud import firestore
from storage.storage_client import StorageClient


def get_item_images(item_id: str) -> dict:
    """
    Get all images for a clothing item with signed URLs.

    Args:
        item_id: Firestore document ID

    Returns:
        Dict with item metadata and signed image URLs
    """
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
    storage = StorageClient()

    item_ref = db.collection('clothing_items').document(item_id)
    item_doc = item_ref.get()

    if not item_doc.exists:
        return {'success': False, 'error': 'Item not found'}

    data = item_doc.to_dict()

    # Handle legacy items that may have 'image_url' (singular) instead of 'image_urls'
    image_urls = data.get('image_urls') or ([data['image_url']] if 'image_url' in data else [])

    signed_urls = [storage.get_signed_url(url) for url in image_urls]

    return {
        'success': True,
        'item_id': item_id,
        'type': data['type'],
        'image_urls': signed_urls,
        'image_count': len(signed_urls),
        'wear_count': data.get('wear_count', 0),
        'last_worn': data['last_worn'].isoformat() if data.get('last_worn') else None
    }


def delete_item_image(item_id: str, image_index: int) -> dict:
    """
    Delete a specific image from a clothing item.
    If it's the last image, deletes the entire item and its wear logs.

    Args:
        item_id: Firestore document ID
        image_index: Index of the image to delete in image_urls

    Returns:
        Dict with result (item_deleted flag indicates full deletion)
    """
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
    storage = StorageClient()

    item_ref = db.collection('clothing_items').document(item_id)
    item_doc = item_ref.get()

    if not item_doc.exists:
        return {'success': False, 'error': 'Item not found'}

    data = item_doc.to_dict()
    # Handle legacy items that may have 'image_url' (singular) instead of 'image_urls'
    image_urls = data.get('image_urls') or ([data['image_url']] if 'image_url' in data else [])
    embeddings = data.get('embeddings', {})

    if image_index < 0 or image_index >= len(image_urls):
        return {'success': False, 'error': 'Invalid image index'}

    gs_url_to_delete = image_urls[image_index]

    if len(image_urls) == 1:
        # Last image — delete entire item
        storage.delete_image(gs_url_to_delete)

        # Delete all wear logs for this item
        wear_logs = db.collection('wear_logs') \
            .where('item_id', '==', item_id).stream()
        for log in wear_logs:
            log.reference.delete()

        # Delete the item document
        item_ref.delete()

        return {
            'success': True,
            'item_deleted': True,
            'item_id': item_id
        }
    else:
        # Partial delete — remove image and re-index embeddings
        storage.delete_image(gs_url_to_delete)

        new_image_urls = image_urls[:image_index] + image_urls[image_index + 1:]

        # Rebuild embeddings with sequential keys
        new_embeddings = {}
        new_key = 0
        for old_key in range(len(image_urls)):
            if old_key == image_index:
                continue
            new_embeddings[str(new_key)] = embeddings[str(old_key)]
            new_key += 1

        item_ref.update({
            'image_urls': new_image_urls,
            'embeddings': new_embeddings
        })

        return {
            'success': True,
            'item_deleted': False,
            'item_id': item_id,
            'remaining_images': len(new_image_urls)
        }
