import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from google.cloud import firestore
from dotenv import load_dotenv

load_dotenv()


def initialize_firestore():
    """Initialize Firestore collections and verify setup"""
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))

    collections = ['clothing_items', 'wear_logs']

    for collection_name in collections:
        collection_ref = db.collection(collection_name)
        doc_ref = collection_ref.document('_init')
        doc_ref.set({'initialized': True, 'timestamp': firestore.SERVER_TIMESTAMP})
        doc_ref.delete()
        print(f"Collection '{collection_name}' initialized")

    print("\nFirestore setup complete!")


if __name__ == '__main__':
    initialize_firestore()
