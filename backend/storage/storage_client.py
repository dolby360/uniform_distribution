from google.cloud import storage
import google.auth
from google.auth import iam
from google.auth.transport import requests as google_auth_requests
from google.oauth2 import service_account
import os
from datetime import timedelta


class StorageClient:
    def __init__(self):
        self.client = storage.Client(project=os.getenv('GCP_PROJECT_ID'))
        self.bucket_name = os.getenv('STORAGE_BUCKET')
        self.bucket = self.client.bucket(self.bucket_name)
        self._signing_credentials = None

    def upload_original_photo(self, image_bytes: bytes, user_id: str = 'default') -> str:
        """
        Upload full outfit photo.

        Args:
            image_bytes: Image data as bytes
            user_id: User identifier (default: 'default')

        Returns:
            gs:// URL to uploaded image
        """
        from datetime import datetime
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        blob_name = f"original-photos/{timestamp}_{user_id}_full.jpg"

        blob = self.bucket.blob(blob_name)
        blob.upload_from_string(image_bytes, content_type='image/jpeg')

        return f"gs://{self.bucket_name}/{blob_name}"

    def upload_cropped_item(self, image_bytes: bytes, item_type: str,
                           item_id: str) -> str:
        """
        Upload cropped clothing item.

        Args:
            image_bytes: Cropped image data
            item_type: 'shirt' or 'pants'
            item_id: Unique item identifier

        Returns:
            gs:// URL to uploaded image
        """
        folder = f"{item_type}s" if not item_type.endswith('s') else item_type
        blob_name = f"cropped-items/{folder}/{item_id}_{item_type}.jpg"

        blob = self.bucket.blob(blob_name)
        blob.upload_from_string(image_bytes, content_type='image/jpeg')

        return f"gs://{self.bucket_name}/{blob_name}"

    def download_image(self, gs_url: str) -> bytes:
        """
        Download image from Cloud Storage.

        Args:
            gs_url: gs://bucket/path format URL

        Returns:
            Image bytes
        """
        path = gs_url.replace(f"gs://{self.bucket_name}/", "")
        blob = self.bucket.blob(path)
        return blob.download_as_bytes()

    def _get_signing_credentials(self):
        """Get credentials capable of signing, works on both local and Cloud Run."""
        if self._signing_credentials is not None:
            return self._signing_credentials

        credentials, project = google.auth.default()

        # Local with service account key - can sign directly
        if hasattr(credentials, 'sign_bytes'):
            self._signing_credentials = credentials
            return self._signing_credentials

        # Cloud Run/Functions - use IAM API for signing
        auth_request = google_auth_requests.Request()
        credentials.refresh(auth_request)

        signer = iam.Signer(
            request=auth_request,
            credentials=credentials,
            service_account_email=credentials.service_account_email,
        )

        self._signing_credentials = service_account.Credentials(
            signer=signer,
            service_account_email=credentials.service_account_email,
            token_uri="https://oauth2.googleapis.com/token",
        )
        return self._signing_credentials

    def get_signed_url(self, url: str, expiration_minutes: int = 60) -> str:
        """
        Generate signed URL for image access.

        Args:
            url: gs://bucket/path or https://storage.googleapis.com/bucket/path URL
            expiration_minutes: URL validity period (default: 60 minutes)

        Returns:
            HTTPS signed URL for temporary access
        """
        if url.startswith(f"https://storage.googleapis.com/{self.bucket_name}/"):
            path = url.split(f"/{self.bucket_name}/", 1)[1].split("?")[0]
        else:
            path = url.replace(f"gs://{self.bucket_name}/", "")
        blob = self.bucket.blob(path)

        url = blob.generate_signed_url(
            version="v4",
            expiration=timedelta(minutes=expiration_minutes),
            method="GET",
            credentials=self._get_signing_credentials()
        )
        return url

    def delete_image(self, gs_url: str) -> bool:
        """
        Delete image from Cloud Storage.

        Args:
            gs_url: gs://bucket/path format URL

        Returns:
            True if deleted successfully
        """
        try:
            path = gs_url.replace(f"gs://{self.bucket_name}/", "")
            blob = self.bucket.blob(path)
            blob.delete()
            return True
        except Exception as e:
            print(f"Error deleting {gs_url}: {e}")
            return False

    def list_items(self, item_type: str = None) -> list:
        """
        List all stored items.

        Args:
            item_type: Filter by 'shirt' or 'pants' (optional)

        Returns:
            List of gs:// URLs
        """
        if item_type:
            folder = f"{item_type}s" if not item_type.endswith('s') else item_type
            prefix = f"cropped-items/{folder}/"
        else:
            prefix = "cropped-items/"
        blobs = self.bucket.list_blobs(prefix=prefix)

        return [f"gs://{self.bucket_name}/{blob.name}" for blob in blobs]
