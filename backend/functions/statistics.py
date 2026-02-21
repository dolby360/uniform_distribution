import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from google.cloud import firestore
from datetime import datetime, timedelta
from storage.storage_client import StorageClient


def get_statistics() -> dict:
    """
    Calculate wardrobe statistics
    """
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
    storage = StorageClient()

    thirty_days_ago = datetime.now() - timedelta(days=30)

    # 1. Most worn items (top 5)
    most_worn_query = db.collection('clothing_items')\
        .order_by('wear_count', direction=firestore.Query.DESCENDING)\
        .limit(5)

    most_worn = []
    for doc in most_worn_query.stream():
        data = doc.to_dict()
        most_worn.append({
            'id': doc.id,
            'type': data['type'],
            'image_url': storage.get_signed_url(data['image_url']),
            'wear_count': data['wear_count'],
            'last_worn': data.get('last_worn').isoformat() if data.get('last_worn') else None,
            'days_since_worn': calculate_days_since(data.get('last_worn'))
        })

    # 2. Least worn items (bottom 5)
    least_worn_query = db.collection('clothing_items')\
        .order_by('wear_count', direction=firestore.Query.ASCENDING)\
        .limit(5)

    least_worn = []
    for doc in least_worn_query.stream():
        data = doc.to_dict()
        least_worn.append({
            'id': doc.id,
            'type': data['type'],
            'image_url': storage.get_signed_url(data['image_url']),
            'wear_count': data['wear_count'],
            'last_worn': data.get('last_worn').isoformat() if data.get('last_worn') else None,
            'days_since_worn': calculate_days_since(data.get('last_worn'))
        })

    # 3. Items not worn in 30+ days
    all_items = db.collection('clothing_items').stream()

    not_worn_30_days = []
    for doc in all_items:
        data = doc.to_dict()
        last_worn = data.get('last_worn')

        if last_worn is None or last_worn < thirty_days_ago:
            not_worn_30_days.append({
                'id': doc.id,
                'type': data['type'],
                'image_url': storage.get_signed_url(data['image_url']),
                'wear_count': data['wear_count'],
                'last_worn': last_worn.isoformat() if last_worn else None,
                'days_since_worn': calculate_days_since(last_worn)
            })

    # 4. Totals
    all_items_list = list(db.collection('clothing_items').stream())
    total_shirts = sum(1 for item in all_items_list if item.to_dict()['type'] == 'shirt')
    total_pants = sum(1 for item in all_items_list if item.to_dict()['type'] == 'pants')

    total_wears = sum(item.to_dict().get('wear_count', 0) for item in all_items_list)

    # 5. Wear frequency (last 30 days)
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

    now = datetime.now()
    delta = now - timestamp
    return delta.days
