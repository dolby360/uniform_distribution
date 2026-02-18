from google.cloud import aiplatform
import base64
import os
import numpy as np
from typing import List
from google.protobuf import struct_pb2


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
        image_base64 = base64.b64encode(image_bytes).decode('utf-8')

        client_options = {
            "api_endpoint": f"{os.getenv('GCP_REGION', 'us-central1')}-aiplatform.googleapis.com"
        }
        client = aiplatform.gapic.PredictionServiceClient(client_options=client_options)

        instance = struct_pb2.Value(
            struct_value=struct_pb2.Struct(
                fields={
                    "image": struct_pb2.Value(
                        struct_value=struct_pb2.Struct(
                            fields={
                                "bytesBase64Encoded": struct_pb2.Value(
                                    string_value=image_base64
                                )
                            }
                        )
                    )
                }
            )
        )

        response = client.predict(
            endpoint=self.endpoint_name,
            instances=[instance]
        )

        embedding = list(response.predictions[0]['imageEmbedding'])

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
