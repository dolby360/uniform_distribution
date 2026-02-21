import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from storage.storage_client import StorageClient
from embeddings.vertex_embedder import VertexEmbedder
from utils.match_pipeline import embed_and_match
from google.cloud import firestore


def process_manual_crop(original_image_bytes: bytes,
                        shirt_image_bytes: bytes = None,
                        pants_image_bytes: bytes = None) -> dict:
    """
    Process manually-cropped outfit images (skip AI detection).

    Args:
        original_image_bytes: Full outfit photo bytes
        shirt_image_bytes: User-cropped shirt region (None if skipped)
        pants_image_bytes: User-cropped pants region (None if skipped)

    Returns:
        Dict with match results (same structure as process_outfit_image)
    """
    storage = StorageClient()
    embedder = VertexEmbedder()
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))

    # Upload original photo
    original_url = storage.upload_original_photo(original_image_bytes)

    result = {
        'success': True,
        'original_photo_url': storage.get_signed_url(original_url),
        'shirt': None,
        'pants': None
    }

    # Process each provided crop
    items = [('shirt', shirt_image_bytes), ('pants', pants_image_bytes)]
    for item_type, crop_bytes in items:
        if crop_bytes is None:
            continue

        result[item_type] = embed_and_match(
            crop_bytes, item_type, storage, embedder, db
        )

    return result
