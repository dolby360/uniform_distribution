"""
Admin tool to merge accidentally-duplicated clothing items.

Usage:
    python backend/scripts/merge_items.py list                   # list all items
    python backend/scripts/merge_items.py list shirt             # filter by type
    python backend/scripts/merge_items.py merge KEEP_ID DROP_ID  # merge DROP into KEEP

Run from the project root with backend/.env loaded.
"""

import sys
import os
import argparse

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from google.cloud import firestore
from dotenv import load_dotenv

load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

MAX_SAMPLES = 10


def _db() -> firestore.Client:
    return firestore.Client(project=os.getenv('GCP_PROJECT_ID'))


def list_items(item_type: str = None) -> None:
    db = _db()
    query = db.collection('clothing_items')
    if item_type:
        query = query.where('type', '==', item_type)

    rows = []
    for doc in query.stream():
        d = doc.to_dict()
        rows.append({
            'id': doc.id,
            'type': d.get('type', '?'),
            'wear_count': d.get('wear_count', 0),
            'last_worn': d.get('last_worn'),
            'images': len(d.get('image_urls', [])),
            'first_image': (d.get('image_urls') or [''])[0],
        })

    rows.sort(key=lambda r: (r['type'], r['last_worn'] or ''))
    print(f"\n{'ID':<24} {'TYPE':<6} {'WEARS':>5} {'IMGS':>4} {'LAST_WORN':<26} IMAGE")
    print('-' * 120)
    for r in rows:
        last = r['last_worn'].isoformat() if r['last_worn'] else 'never'
        print(f"{r['id']:<24} {r['type']:<6} {r['wear_count']:>5} {r['images']:>4} {last:<26} {r['first_image']}")
    print(f"\nTotal: {len(rows)} item(s)\n")


def merge_items(keep_id: str, drop_id: str) -> None:
    """
    Fold drop_id into keep_id:
      - append drop's image_urls + embeddings to keep (cap at MAX_SAMPLES)
      - sum wear_count
      - take the max last_worn
      - reassign every wear_log.item_id from drop_id to keep_id
      - delete the drop_id doc
    """
    if keep_id == drop_id:
        raise SystemExit("ERROR: keep_id and drop_id are the same")

    db = _db()
    keep_ref = db.collection('clothing_items').document(keep_id)
    drop_ref = db.collection('clothing_items').document(drop_id)

    keep = keep_ref.get()
    drop = drop_ref.get()
    if not keep.exists:
        raise SystemExit(f"ERROR: keep item {keep_id} not found")
    if not drop.exists:
        raise SystemExit(f"ERROR: drop item {drop_id} not found")

    keep_data = keep.to_dict()
    drop_data = drop.to_dict()

    if keep_data.get('type') != drop_data.get('type'):
        raise SystemExit(
            f"ERROR: type mismatch — keep is {keep_data.get('type')}, drop is {drop_data.get('type')}. Refusing to merge."
        )

    print(f"\nMerging:")
    print(f"  KEEP  {keep_id}  type={keep_data['type']}  wears={keep_data.get('wear_count',0)}  imgs={len(keep_data.get('image_urls',[]))}")
    print(f"  DROP  {drop_id}  type={drop_data['type']}  wears={drop_data.get('wear_count',0)}  imgs={len(drop_data.get('image_urls',[]))}")

    keep_urls = list(keep_data.get('image_urls', []))
    drop_urls = list(drop_data.get('image_urls', []))
    keep_embeddings = dict(keep_data.get('embeddings', {}))
    drop_embeddings = dict(drop_data.get('embeddings', {}))

    capacity = MAX_SAMPLES - len(keep_urls)
    if capacity <= 0:
        print(f"  WARN: keep already has {len(keep_urls)} samples (cap {MAX_SAMPLES}); will not append more.")
        new_urls = keep_urls
        new_embeddings = keep_embeddings
    else:
        # Iterate drop's embeddings in their original index order to keep alignment with image_urls.
        sorted_drop_keys = sorted(drop_embeddings.keys(), key=lambda k: int(k))
        appended = 0
        new_urls = list(keep_urls)
        new_embeddings = dict(keep_embeddings)
        for k in sorted_drop_keys:
            if appended >= capacity or appended >= len(drop_urls):
                break
            next_idx = len(new_urls)
            new_urls.append(drop_urls[appended])
            new_embeddings[str(next_idx)] = drop_embeddings[k]
            appended += 1
        print(f"  appended {appended}/{len(drop_urls)} sample(s) from drop into keep")

    new_wear_count = keep_data.get('wear_count', 0) + drop_data.get('wear_count', 0)
    keep_last = keep_data.get('last_worn')
    drop_last = drop_data.get('last_worn')
    if keep_last is None:
        new_last_worn = drop_last
    elif drop_last is None:
        new_last_worn = keep_last
    else:
        new_last_worn = max(keep_last, drop_last)

    # Reassign wear_logs
    log_query = db.collection('wear_logs').where('item_id', '==', drop_id)
    reassigned = 0
    batch = db.batch()
    BATCH_LIMIT = 400
    for log_doc in log_query.stream():
        batch.update(log_doc.reference, {'item_id': keep_id})
        reassigned += 1
        if reassigned % BATCH_LIMIT == 0:
            batch.commit()
            batch = db.batch()
    batch.commit()
    print(f"  reassigned {reassigned} wear_log(s) from drop to keep")

    # Apply update + delete in a final batch
    final = db.batch()
    final.update(keep_ref, {
        'image_urls': new_urls,
        'embeddings': new_embeddings,
        'wear_count': new_wear_count,
        'last_worn': new_last_worn,
    })
    final.delete(drop_ref)
    final.commit()

    print(f"\nDone. Keep item {keep_id} now has wears={new_wear_count}, imgs={len(new_urls)}, last_worn={new_last_worn}.\n")


def main():
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest='cmd', required=True)
    p_list = sub.add_parser('list', help='list all clothing items')
    p_list.add_argument('type', nargs='?', choices=['shirt', 'pants'])
    p_merge = sub.add_parser('merge', help='merge two items: drop is folded into keep, then deleted')
    p_merge.add_argument('keep_id')
    p_merge.add_argument('drop_id')
    args = parser.parse_args()

    if args.cmd == 'list':
        list_items(args.type)
    elif args.cmd == 'merge':
        merge_items(args.keep_id, args.drop_id)


if __name__ == '__main__':
    main()
