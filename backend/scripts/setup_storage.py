from google.cloud import storage
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from dotenv import load_dotenv
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))


def setup_storage_bucket():
    """Initialize Cloud Storage bucket and structure."""
    client = storage.Client(project=os.getenv('GCP_PROJECT_ID'))
    bucket_name = os.getenv('STORAGE_BUCKET')

    # Get or create bucket
    bucket = client.bucket(bucket_name)
    if not bucket.exists():
        bucket = client.create_bucket(bucket_name, location='us-central1')
        print(f"Bucket '{bucket_name}' created")
    else:
        print(f"Bucket '{bucket_name}' already exists")

    # Set lifecycle policy
    bucket.add_lifecycle_delete_rule(
        age=30,
        matches_prefix=['original-photos/']
    )
    bucket.patch()
    print("Lifecycle policy set (original-photos: 30-day retention)")

    # Create folder structure with placeholder files
    folders = ['original-photos/', 'cropped-items/shirts/', 'cropped-items/pants/']
    for folder in folders:
        blob = bucket.blob(f"{folder}.keep")
        blob.upload_from_string("")
        print(f"Folder '{folder}' created")

    print("\nCloud Storage setup complete!")


if __name__ == '__main__':
    setup_storage_bucket()
