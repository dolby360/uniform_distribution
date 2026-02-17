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

        # Check normalized (L2 norm ~ 1.0)
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
