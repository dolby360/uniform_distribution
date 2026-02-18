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
            detection['bounding_box']
        )

        # 4. Generate embedding
        embedding = embedder.generate_embedding(cropped_bytes)

        # Upload cropped image
        temp_id = str(uuid.uuid4())
        cropped_url = storage.upload_cropped_item(
            cropped_bytes, item_type, temp_id
        )

        # 5. Search for similar items in Firestore
        existing_items = db.collection('clothing_items')\
            .where('type', '==', item_type)\
            .stream()

        candidates = [
            (item.id, item.to_dict()['embedding'])
            for item in existing_items
        ]

        match = find_most_similar(embedding, candidates, threshold=0.85)

        # 6. Build result
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
