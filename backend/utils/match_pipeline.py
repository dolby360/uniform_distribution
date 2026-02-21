import uuid
from storage.storage_client import StorageClient
from embeddings.vertex_embedder import VertexEmbedder
from embeddings.similarity import find_most_similar
from google.cloud import firestore


def embed_and_match(crop_bytes: bytes, item_type: str,
                    storage: StorageClient, embedder: VertexEmbedder,
                    db: firestore.Client) -> dict:
    """
    Shared pipeline: upload crop, generate embedding, find match.

    Args:
        crop_bytes: Cropped image bytes (JPEG)
        item_type: 'shirt' or 'pants'
        storage: StorageClient instance
        embedder: VertexEmbedder instance
        db: Firestore client

    Returns:
        Dict with match result (matched, item_id, similarity, image_url, cropped_url, embedding)
    """
    # Generate embedding
    embedding = embedder.generate_embedding(crop_bytes)

    # Upload cropped image
    temp_id = str(uuid.uuid4())
    cropped_url = storage.upload_cropped_item(crop_bytes, item_type, temp_id)

    # Search for similar items in Firestore
    existing_items = db.collection('clothing_items')\
        .where('type', '==', item_type)\
        .stream()

    candidates = []
    for item in existing_items:
        data = item.to_dict()
        for emb in data['embeddings'].values():
            candidates.append((item.id, emb))

    match = find_most_similar(embedding, candidates, threshold=0.85)

    if match:
        item_id, similarity = match
        item_doc = db.collection('clothing_items').document(item_id).get()
        item_data = item_doc.to_dict()
        return {
            'matched': True,
            'item_id': item_id,
            'similarity': float(similarity),
            'image_url': storage.get_signed_url(item_data['image_urls'][0]),
            'cropped_url': storage.get_signed_url(cropped_url),
            'embedding': embedding
        }
    else:
        return {
            'matched': False,
            'cropped_url': storage.get_signed_url(cropped_url),
            'embedding': embedding
        }
