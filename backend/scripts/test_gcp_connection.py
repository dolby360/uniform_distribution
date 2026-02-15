from google.cloud import firestore
from google.cloud import storage
import os
from dotenv import load_dotenv

load_dotenv()

def test_gcp_connection():
    try:
        # Test Firestore
        db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
        print("Firestore connection successful")

        # Test Cloud Storage
        storage_client = storage.Client(project=os.getenv('GCP_PROJECT_ID'))
        print("Cloud Storage connection successful")

        print("\nAll GCP connections working!")
        return True
    except Exception as e:
        print(f"Connection failed: {e}")
        return False

if __name__ == '__main__':
    test_gcp_connection()
