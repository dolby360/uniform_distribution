import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from google.cloud import firestore
from datetime import datetime, timedelta, timezone
from storage.storage_client import StorageClient


def get_statistics() -> dict:
    """
    Calculate wardrobe statistics
    """
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
    storage = StorageClient()

    thirty_days_ago = datetime.now(timezone.utc) - timedelta(days=30)

    # Single query for all items
    all_items = []
    for doc in db.collection('clothing_items').stream():
        data = doc.to_dict()
        all_items.append({
            'id': doc.id,
            'type': data['type'],
            'image_url': data['image_url'],
            'wear_count': data.get('wear_count', 0),
            'last_worn': data.get('last_worn'),
        })

    # Sort for most/least worn
    sorted_by_wear = sorted(all_items, key=lambda x: x['wear_count'], reverse=True)
    most_worn_raw = sorted_by_wear[:5]
    least_worn_raw = sorted(all_items, key=lambda x: x['wear_count'])[:5]

    # Items not worn in 30+ days
    not_worn_30_raw = [
        item for item in all_items
        if item['last_worn'] is None or item['last_worn'] < thirty_days_ago
    ]

    # Sign URLs only for items we need
    seen_urls = {}
    def sign_url(url):
        if url not in seen_urls:
            seen_urls[url] = storage.get_signed_url(url)
        return seen_urls[url]

    def format_item(item):
        return {
            'id': item['id'],
            'type': item['type'],
            'image_url': sign_url(item['image_url']),
            'wear_count': item['wear_count'],
            'last_worn': item['last_worn'].isoformat() if item['last_worn'] else None,
            'days_since_worn': calculate_days_since(item['last_worn']),
        }

    most_worn = [format_item(i) for i in most_worn_raw]
    least_worn = [format_item(i) for i in least_worn_raw]
    not_worn_30_days = [format_item(i) for i in not_worn_30_raw]

    # Totals
    total_shirts = sum(1 for item in all_items if item['type'] == 'shirt')
    total_pants = sum(1 for item in all_items if item['type'] == 'pants')
    total_wears = sum(item['wear_count'] for item in all_items)

    # Wear frequency (last 30 days)
    wear_logs_query = db.collection('wear_logs')\
        .where('worn_at', '>=', thirty_days_ago)\
        .stream()

    wear_frequency = {}
    for log in wear_logs_query:
        log_data = log.to_dict()
        date_str = log_data['worn_at'].date().isoformat()
        wear_frequency[date_str] = wear_frequency.get(date_str, 0) + 1

    return {
        'most_worn': most_worn,
        'least_worn': least_worn,
        'not_worn_30_days': not_worn_30_days,
        'totals': {
            'total_shirts': total_shirts,
            'total_pants': total_pants,
            'total_items': total_shirts + total_pants,
            'total_wears': total_wears
        },
        'wear_frequency': wear_frequency
    }


def calculate_days_since(timestamp):
    """Calculate days since timestamp"""
    if timestamp is None:
        return None

    now = datetime.now(timezone.utc)
    delta = now - timestamp
    return delta.days
