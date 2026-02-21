# Step 5: Vertex AI Embedding

## Goal

Implement Vertex AI multimodalembedding@001 integration to generate 1408-dimensional embeddings for cropped clothing images.

## ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│            Vertex AI Embedding Generation                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Input: Cropped clothing image (shirt or pants)             │
│  ┌───────────────────────────────┐                          │
│  │   👕 Cropped shirt image       │                          │
│  │   - JPEG format                │                          │
│  │   - RGB color space            │                          │
│  └───────────────────────────────┘                          │
│            │                                                 │
│            ▼                                                 │
│  ┌────────────────────────────────────────────────┐        │
│  │     Vertex AI API Call                         │        │
│  │  Model: multimodalembedding@001                │        │
│  │  Region: us-central1                           │        │
│  │  Input: Image bytes (base64)                   │        │
│  └────────────────────────────────────────────────┘        │
│            │                                                 │
│            ▼                                                 │
│  ┌────────────────────────────────────────────────┐        │
│  │     Embedding Vector                           │        │
│  │  [0.023, -0.145, 0.891, ..., 0.234]           │        │
│  │  Length: 1408 dimensions                       │        │
│  │  Type: float32                                 │        │
│  └────────────────────────────────────────────────┘        │
│            │                                                 │
│            ▼                                                 │
│  ┌────────────────────────────────────────────────┐        │
│  │   Normalization (L2 norm)                      │        │
│  │   embedding = embedding / ||embedding||        │        │
│  └────────────────────────────────────────────────┘        │
│            │                                                 │
│            ▼                                                 │
│  ┌────────────────────────────────────────────────┐        │
│  │   Normalized Embedding                         │        │
│  │   Ready for cosine similarity comparison       │        │
│  │   Stored in Firestore (1408 floats)            │        │
│  └────────────────────────────────────────────────┘        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Data Schemas

Embedding vectors are arrays of floats (no specific schema, just List[float] with length 1408)

## API Endpoint Signatures

N/A (utility function)

## File Structure

```
backend/
  embeddings/
    __init__.py
    vertex_embedder.py             # Vertex AI embedding client
    similarity.py                  # Cosine similarity calculator
  tests/
    test_embeddings.py             # Unit tests
```

## Key Code Snippets

### backend/embeddings/__init__.py

```python
from .vertex_embedder import VertexEmbedder
from .similarity import cosine_similarity, find_most_similar

__all__ = ['VertexEmbedder', 'cosine_similarity', 'find_most_similar']
```

### backend/embeddings/vertex_embedder.py

```python
from google.cloud import aiplatform
from google.cloud.aiplatform.gapic.schema import predict
import base64
import os
import numpy as np
from typing import List

class VertexEmbedder:
    def __init__(self):
        aiplatform.init(
            project=os.getenv('GCP_PROJECT_ID'),
            location=os.getenv('GCP_REGION', 'us-central1')
        )

        self.endpoint_name = (
            f"projects/{os.getenv('GCP_PROJECT_ID')}/locations/"
            f"{os.getenv('GCP_REGION', 'us-central1')}/publishers/google/"
            f"models/multimodalembedding@001"
        )

    def generate_embedding(self, image_bytes: bytes) -> List[float]:
        """
        Generate 1408-dimensional embedding for clothing image

        Args:
            image_bytes: Image as bytes (JPEG)

        Returns:
            List of 1408 floats (normalized)
        """
        # Encode image to base64
        image_base64 = base64.b64encode(image_bytes).decode('utf-8')

        # Create prediction client
        client_options = {
            "api_endpoint": f"{os.getenv('GCP_REGION', 'us-central1')}-aiplatform.googleapis.com"
        }
        client = aiplatform.gapic.PredictionServiceClient(client_options=client_options)

        # Prepare instance
        instance = predict.instance.ImageEmbeddingInstance(
            image=predict.instance.Image(bytes_base64_encoded=image_base64)
        )

        # Make prediction
        response = client.predict(
            endpoint=self.endpoint_name,
            instances=[instance.to_value()]
        )

        # Extract embedding from response
        embedding = response.predictions[0]['imageEmbedding']

        # Normalize embedding (L2 norm)
        normalized_embedding = self._normalize_embedding(embedding)

        return normalized_embedding

    def _normalize_embedding(self, embedding: List[float]) -> List[float]:
        """
        L2 normalization for cosine similarity

        Args:
            embedding: Raw embedding vector

        Returns:
            Normalized embedding (L2 norm = 1.0)
        """
        embedding_array = np.array(embedding)
        norm = np.linalg.norm(embedding_array)

        if norm == 0:
            return embedding

        normalized = embedding_array / norm
        return normalized.tolist()

    def batch_generate_embeddings(self, image_bytes_list: List[bytes]) -> List[List[float]]:
        """
        Generate embeddings for multiple images

        Args:
            image_bytes_list: List of image bytes

        Returns:
            List of embeddings
        """
        embeddings = []
        for image_bytes in image_bytes_list:
            embedding = self.generate_embedding(image_bytes)
            embeddings.append(embedding)

        return embeddings
```

### backend/embeddings/similarity.py

```python
import numpy as np
from typing import List, Tuple, Optional

def cosine_similarity(embedding1: List[float], embedding2: List[float]) -> float:
    """
    Calculate cosine similarity between two embeddings

    Args:
        embedding1: First embedding vector (1408 dimensions)
        embedding2: Second embedding vector (1408 dimensions)

    Returns:
        Similarity score between 0.0 and 1.0
    """
    vec1 = np.array(embedding1)
    vec2 = np.array(embedding2)

    # Cosine similarity for normalized vectors is just dot product
    similarity = np.dot(vec1, vec2)

    # Clamp to [0, 1] range (normalized vectors should already be in range)
    return float(np.clip(similarity, 0.0, 1.0))

def find_most_similar(
    query_embedding: List[float],
    candidate_embeddings: List[Tuple[str, List[float]]],
    threshold: float = 0.85
) -> Optional[Tuple[str, float]]:
    """
    Find most similar item from candidates

    Args:
        query_embedding: Embedding to compare against
        candidate_embeddings: List of (item_id, embedding) tuples
        threshold: Minimum similarity score (default 0.85)

    Returns:
        (item_id, similarity_score) or None if no match above threshold
    """
    best_match = None
    best_score = threshold

    for item_id, embedding in candidate_embeddings:
        score = cosine_similarity(query_embedding, embedding)

        if score > best_score:
            best_score = score
            best_match = (item_id, score)

    return best_match

def find_top_k_similar(
    query_embedding: List[float],
    candidate_embeddings: List[Tuple[str, List[float]]],
    k: int = 5,
    threshold: float = 0.0
) -> List[Tuple[str, float]]:
    """
    Find top K most similar items

    Args:
        query_embedding: Embedding to compare against
        candidate_embeddings: List of (item_id, embedding) tuples
        k: Number of results to return
        threshold: Minimum similarity score (default 0.0)

    Returns:
        List of (item_id, similarity_score) sorted by score descending
    """
    scores = []

    for item_id, embedding in candidate_embeddings:
        score = cosine_similarity(query_embedding, embedding)

        if score >= threshold:
            scores.append((item_id, score))

    # Sort by score descending
    scores.sort(key=lambda x: x[1], reverse=True)

    return scores[:k]

def embedding_distance(embedding1: List[float], embedding2: List[float]) -> float:
    """
    Calculate Euclidean distance between embeddings

    Args:
        embedding1: First embedding vector
        embedding2: Second embedding vector

    Returns:
        Distance (lower = more similar)
    """
    vec1 = np.array(embedding1)
    vec2 = np.array(embedding2)

    return float(np.linalg.norm(vec1 - vec2))
```

### backend/tests/test_embeddings.py

```python
import unittest
from embeddings.vertex_embedder import VertexEmbedder
from embeddings.similarity import cosine_similarity, find_most_similar
from PIL import Image
import io
import numpy as np

class TestEmbeddings(unittest.TestCase):

    def setUp(self):
        self.embedder = VertexEmbedder()

    def create_test_image(self, color='red'):
        """Create test image"""
        test_image = Image.new('RGB', (640, 480), color=color)
        img_bytes = io.BytesIO()
        test_image.save(img_bytes, format='JPEG')
        return img_bytes.getvalue()

    def test_generate_embedding(self):
        """Test embedding generation"""
        image_bytes = self.create_test_image('blue')
        embedding = self.embedder.generate_embedding(image_bytes)

        # Check dimensions
        self.assertEqual(len(embedding), 1408)

        # Check normalized (L2 norm ≈ 1.0)
        norm = np.linalg.norm(embedding)
        self.assertAlmostEqual(norm, 1.0, places=5)

    def test_embedding_consistency(self):
        """Test that same image produces same embedding"""
        image_bytes = self.create_test_image('green')

        embedding1 = self.embedder.generate_embedding(image_bytes)
        embedding2 = self.embedder.generate_embedding(image_bytes)

        # Embeddings should be identical
        similarity = cosine_similarity(embedding1, embedding2)
        self.assertGreater(similarity, 0.99)

    def test_cosine_similarity(self):
        """Test cosine similarity calculation"""
        # Same vectors = similarity 1.0
        vec1 = [0.5] * 1408
        vec1_normalized = (np.array(vec1) / np.linalg.norm(vec1)).tolist()

        similarity = cosine_similarity(vec1_normalized, vec1_normalized)
        self.assertAlmostEqual(similarity, 1.0, places=5)

        # Orthogonal vectors = similarity ~0.0
        vec2 = [-0.5] * 1408
        vec2_normalized = (np.array(vec2) / np.linalg.norm(vec2)).tolist()

        similarity = cosine_similarity(vec1_normalized, vec2_normalized)
        self.assertLess(similarity, 0.1)

    def test_find_most_similar(self):
        """Test finding most similar item"""
        query_embedding = [0.5] * 1408
        query_normalized = (np.array(query_embedding) / np.linalg.norm(query_embedding)).tolist()

        # Create candidates
        candidates = [
            ('item1', query_normalized),  # Identical
            ('item2', (np.array([0.4] * 1408) / np.linalg.norm([0.4] * 1408)).tolist()),
            ('item3', (np.array([-0.5] * 1408) / np.linalg.norm([-0.5] * 1408)).tolist()),
        ]

        match = find_most_similar(query_normalized, candidates, threshold=0.85)

        # Should find item1 (identical)
        self.assertIsNotNone(match)
        self.assertEqual(match[0], 'item1')
        self.assertGreater(match[1], 0.99)

    def test_similar_images_high_similarity(self):
        """Test that similar images have high similarity"""
        image1 = self.create_test_image('red')
        image2 = self.create_test_image('red')  # Same color

        embedding1 = self.embedder.generate_embedding(image1)
        embedding2 = self.embedder.generate_embedding(image2)

        similarity = cosine_similarity(embedding1, embedding2)

        # Similar images should have high similarity
        self.assertGreater(similarity, 0.85)

    def test_different_images_low_similarity(self):
        """Test that different images have lower similarity"""
        image1 = self.create_test_image('red')
        image2 = self.create_test_image('blue')  # Different color

        embedding1 = self.embedder.generate_embedding(image1)
        embedding2 = self.embedder.generate_embedding(image2)

        similarity = cosine_similarity(embedding1, embedding2)

        # Different images should have lower similarity
        self.assertLess(similarity, 0.95)

if __name__ == '__main__':
    unittest.main()
```

## Acceptance Criteria

- [ ] Vertex AI API enabled and accessible
- [ ] VertexEmbedder class generates 1408-dimensional embeddings
- [ ] Test image produces consistent embeddings (same image = same embedding)
- [ ] Embeddings are normalized (L2 norm = 1.0)
- [ ] Cosine similarity returns scores between 0.0 and 1.0
- [ ] Similar images (same shirt, different lighting) have similarity > 0.85
- [ ] Different items have similarity < 0.60
- [ ] Unit tests pass for embedding generation and similarity
- [ ] Batch embedding generation works correctly

## Setup Instructions

1. **Enable Vertex AI API**:
   ```bash
   gcloud services enable aiplatform.googleapis.com
   ```

2. **Grant Permissions**:
   ```bash
   gcloud projects add-iam-policy-binding uniform-dist-XXXXX \
     --member="serviceAccount:uniform-dist-sa@uniform-dist-XXXXX.iam.gserviceaccount.com" \
     --role="roles/aiplatform.user"
   ```

3. **Run Tests**:
   ```bash
   python -m pytest backend/tests/test_embeddings.py
   ```

## Verification

### Manual Test Script

```python
# backend/scripts/test_embeddings_manual.py
from embeddings.vertex_embedder import VertexEmbedder
from embeddings.similarity import cosine_similarity
from PIL import Image
import io

def test_embeddings():
    """Test embedding generation with real images"""
    embedder = VertexEmbedder()

    # Create two test images
    img1 = Image.new('RGB', (640, 480), color='red')
    img1_bytes = io.BytesIO()
    img1.save(img1_bytes, format='JPEG')

    img2 = Image.new('RGB', (640, 480), color='blue')
    img2_bytes = io.BytesIO()
    img2.save(img2_bytes, format='JPEG')

    print("Generating embeddings...")
    embedding1 = embedder.generate_embedding(img1_bytes.getvalue())
    embedding2 = embedder.generate_embedding(img2_bytes.getvalue())

    print(f"✓ Embedding 1 dimensions: {len(embedding1)}")
    print(f"✓ Embedding 2 dimensions: {len(embedding2)}")

    # Check normalization
    import numpy as np
    norm1 = np.linalg.norm(embedding1)
    norm2 = np.linalg.norm(embedding2)

    print(f"✓ Embedding 1 L2 norm: {norm1:.6f}")
    print(f"✓ Embedding 2 L2 norm: {norm2:.6f}")

    # Calculate similarity
    similarity = cosine_similarity(embedding1, embedding2)
    print(f"✓ Cosine similarity: {similarity:.4f}")

    print("\n✓ All embedding tests passed!")

if __name__ == '__main__':
    test_embeddings()
```

Run test:
```bash
python backend/scripts/test_embeddings_manual.py
```

### Expected Output

```
Generating embeddings...
✓ Embedding 1 dimensions: 1408
✓ Embedding 2 dimensions: 1408
✓ Embedding 1 L2 norm: 1.000000
✓ Embedding 2 L2 norm: 1.000000
✓ Cosine similarity: 0.7234

✓ All embedding tests passed!
```
