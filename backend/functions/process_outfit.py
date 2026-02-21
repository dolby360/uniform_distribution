import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from storage.storage_client import StorageClient
from gemini.vision_detector import VisionDetector
from utils.image_cropper import crop_clothing_item
from utils.match_pipeline import embed_and_match
from embeddings.vertex_embedder import VertexEmbedder
from google.cloud import firestore


def process_outfit_image(image_bytes: bytes) -> dict:
    """
    Main processing pipeline for outfit photo.

    Steps:
        1. Upload original photo to Cloud Storage
        2. Detect shirt & pants via Gemini Vision
        3. Crop each detected item
        4. Generate embeddings via Vertex AI
        5. Search Firestore for similar items
        6. Return match results

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

        # Crop item from original image
        cropped_bytes = crop_clothing_item(
            image_bytes,
            detection['bounding_box'],
            item_type=item_type
        )

        # 4-6. Embed, match, and build result
        result[item_type] = embed_and_match(
            cropped_bytes, item_type, storage, embedder, db
        )

    return result
