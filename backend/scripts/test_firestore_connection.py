import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from google.cloud import firestore
from schemas.clothing_item import ClothingItem
from schemas.wear_log import WearLog
from dotenv import load_dotenv

load_dotenv()


def test_firestore():
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))

    # Test: Create a clothing item
    test_item = ClothingItem(
        type='shirt',
        image_url='gs://test-bucket/test.jpg',
        embedding=[0.1] * 1408,
        wear_count=0
    )
    test_item.validate()

    doc_ref = db.collection('clothing_items').add(test_item.to_dict())
    item_id = doc_ref[1].id
    print(f"Created test item: {item_id}")

    # Test: Retrieve item
    retrieved = db.collection('clothing_items').document(item_id).get()
    retrieved_item = ClothingItem.from_dict(retrieved.to_dict(), item_id)
    print(f"Retrieved item: {retrieved_item.type}")

    # Test: Create wear log
    test_log = WearLog(
        item_id=item_id,
        item_type='shirt',
        confidence_score=0.95
    )
    test_log.validate()

    log_ref = db.collection('wear_logs').add(test_log.to_dict())
    log_id = log_ref[1].id
    print(f"Created test log: {log_id}")

    # Cleanup
    db.collection('clothing_items').document(item_id).delete()
    db.collection('wear_logs').document(log_id).delete()
    print("Cleaned up test data")

    print("\nAll Firestore tests passed!")


if __name__ == '__main__':
    test_firestore()
