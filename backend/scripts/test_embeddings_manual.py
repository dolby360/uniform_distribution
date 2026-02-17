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

    print(f"Embedding 1 dimensions: {len(embedding1)}")
    print(f"Embedding 2 dimensions: {len(embedding2)}")

    # Check normalization
    import numpy as np
    norm1 = np.linalg.norm(embedding1)
    norm2 = np.linalg.norm(embedding2)

    print(f"Embedding 1 L2 norm: {norm1:.6f}")
    print(f"Embedding 2 L2 norm: {norm2:.6f}")

    # Calculate similarity
    similarity = cosine_similarity(embedding1, embedding2)
    print(f"Cosine similarity: {similarity:.4f}")

    print("\nAll embedding tests passed!")


if __name__ == '__main__':
    test_embeddings()
