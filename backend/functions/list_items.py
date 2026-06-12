import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from datetime import datetime, timezone
from google.cloud import firestore
from storage.storage_client import StorageClient


def list_items() -> dict:
    """
    Return every clothing item, grouped by type, with signed thumbnail URLs.

    Used by the manual-wear-logging UI: the user needs to browse all items,
    not just the top-N from /statistics.

    Returns:
        {
          "shirts": [{ id, image_url, wear_count, last_worn, days_since_worn }, ...],
          "pants":  [...]
        }
        Each list sorted by last_worn desc (None last) so recently-worn items
        — the most likely candidates for "I wore this today" — surface first.
    """
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
    storage = StorageClient()

    # Content hashes for all cropped items in one listing (cheap), so clients
    # can cache images by hash. Stored image_urls may be gs:// paths or https
    # signed URLs, so look up by normalized blob path. Signed URLs are still
    # generated per item (local crypto) and cached so duplicates sign once.
    hash_by_path = storage.get_hashes_by_path("cropped-items/")
    seen_urls: dict = {}
    def sign_url(url: str) -> str:
        if url not in seen_urls:
            seen_urls[url] = storage.get_signed_url(url)
        return seen_urls[url]

    now = datetime.now(timezone.utc)

    shirts = []
    pants = []

    for doc in db.collection('clothing_items').stream():
        data = doc.to_dict()
        last_worn = data.get('last_worn')
        stored_url = data['image_urls'][0]
        entry = {
            'id': doc.id,
            'image_url': sign_url(stored_url),
            'image_hash': hash_by_path.get(storage._blob_path(stored_url)),
            'wear_count': data.get('wear_count', 0),
            'last_worn': last_worn.isoformat() if last_worn else None,
            'days_since_worn': (now - last_worn).days if last_worn else None,
        }
        if data['type'] == 'shirt':
            shirts.append(entry)
        elif data['type'] == 'pants':
            pants.append(entry)

    def by_recent(items):
        # Recently-worn first; items never worn (last_worn=None) at the end.
        with_date = [i for i in items if i['last_worn'] is not None]
        without_date = [i for i in items if i['last_worn'] is None]
        with_date.sort(key=lambda i: i['last_worn'], reverse=True)
        return with_date + without_date

    return {
        'shirts': by_recent(shirts),
        'pants': by_recent(pants),
    }
