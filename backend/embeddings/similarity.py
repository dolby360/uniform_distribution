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

    similarity = np.dot(vec1, vec2)

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
