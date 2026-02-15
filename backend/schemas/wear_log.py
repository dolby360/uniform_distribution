from dataclasses import dataclass
from datetime import datetime
from google.cloud.firestore import SERVER_TIMESTAMP
from typing import Optional


@dataclass
class WearLog:
    item_id: str
    item_type: str  # "shirt" or "pants"
    worn_at: datetime = SERVER_TIMESTAMP
    confidence_score: float = 1.0
    original_image_url: str = ""
    id: Optional[str] = None

    def to_dict(self):
        """Convert to Firestore document format"""
        return {
            'item_id': self.item_id,
            'item_type': self.item_type,
            'worn_at': self.worn_at,
            'confidence_score': self.confidence_score,
            'original_image_url': self.original_image_url
        }

    @staticmethod
    def from_dict(data: dict, doc_id: str = None):
        """Create WearLog from Firestore document"""
        log = WearLog(
            item_id=data['item_id'],
            item_type=data['item_type'],
            worn_at=data.get('worn_at'),
            confidence_score=data.get('confidence_score', 1.0),
            original_image_url=data.get('original_image_url', '')
        )
        log.id = doc_id
        return log

    def validate(self):
        """Validate log data"""
        assert self.item_type in ['shirt', 'pants'], f"Invalid type: {self.item_type}"
        assert 0.0 <= self.confidence_score <= 1.0, f"Invalid confidence: {self.confidence_score}"
