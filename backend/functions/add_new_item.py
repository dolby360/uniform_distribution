import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from google.cloud import firestore


def add_new_item(item_type: str, cropped_image_url: str,
                 embedding: list, original_photo_url: str,
                 log_wear: bool = False) -> dict:
    """
    Add new clothing item to database.

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
        'image_urls': [cropped_image_url],
        'embeddings': {'0': embedding},
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
