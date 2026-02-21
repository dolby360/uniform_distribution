import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from google.cloud import firestore


MAX_SAMPLES = 10


def confirm_match(item_id: str, item_type: str, original_photo_url: str,
                  similarity_score: float = None, new_embedding: list = None,
                  cropped_url: str = None) -> dict:
    """
    Confirm match, log wear event, and optionally add new embedding sample.

    Args:
        item_id: Clothing item ID
        item_type: 'shirt' or 'pants'
        original_photo_url: gs:// URL to original photo
        similarity_score: Match confidence (optional)
        new_embedding: New 1408-dim embedding to add as sample (optional)
        cropped_url: gs:// URL to new cropped image (optional)

    Returns:
        Dict with updated item stats
    """
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))

    item_ref = db.collection('clothing_items').document(item_id)

    # Update wear stats
    update_data = {
        'wear_count': firestore.Increment(1),
        'last_worn': firestore.SERVER_TIMESTAMP
    }

    # Append new sample if provided and under cap
    if new_embedding and cropped_url:
        item_data = item_ref.get().to_dict()
        embeddings = item_data['embeddings']
        if len(embeddings) < MAX_SAMPLES:
            next_key = str(len(embeddings))
            embeddings[next_key] = new_embedding
            update_data['embeddings'] = embeddings
            image_urls = item_data['image_urls']
            image_urls.append(cropped_url)
            update_data['image_urls'] = image_urls

    item_ref.update(update_data)

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
