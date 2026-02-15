from dataclasses import dataclass
from typing import Optional, List
from datetime import datetime
from google.cloud.firestore import SERVER_TIMESTAMP


@dataclass
class ClothingItem:
    type: str  # "shirt" or "pants"
    image_url: str
    embedding: List[float]
    created_at: datetime = SERVER_TIMESTAMP
    last_worn: Optional[datetime] = None
    wear_count: int = 0
    thumbnail_url: Optional[str] = None
    id: Optional[str] = None

    def to_dict(self):
        """Convert to Firestore document format"""
        return {
            'type': self.type,
            'image_url': self.image_url,
            'embedding': self.embedding,
            'created_at': self.created_at,
            'last_worn': self.last_worn,
            'wear_count': self.wear_count,
            'thumbnail_url': self.thumbnail_url
        }

    @staticmethod
    def from_dict(data: dict, doc_id: str = None):
        """Create ClothingItem from Firestore document"""
        item = ClothingItem(
            type=data['type'],
            image_url=data['image_url'],
            embedding=data['embedding'],
            created_at=data.get('created_at'),
            last_worn=data.get('last_worn'),
            wear_count=data.get('wear_count', 0),
            thumbnail_url=data.get('thumbnail_url')
        )
        item.id = doc_id
        return item

    def validate(self):
        """Validate item data"""
        assert self.type in ['shirt', 'pants'], f"Invalid type: {self.type}"
        assert len(self.embedding) == 1408, f"Invalid embedding length: {len(self.embedding)}"
        assert self.wear_count >= 0, f"Invalid wear_count: {self.wear_count}"
