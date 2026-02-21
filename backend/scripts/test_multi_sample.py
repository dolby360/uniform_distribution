"""
Integration test for multi-sample embedding support.

Tests the full flow:
  1. Process image 1.png → add as new items
  2. Process image 1.1.png (same clothes, different angle) → should match
  3. Confirm match with embedding → sample appended
  4. Verify Firestore documents have 2 embeddings each
  5. Process image 2.png (different clothes) → should NOT match existing items

Usage:
    cd backend
    python scripts/test_multi_sample.py
"""
import sys
import os

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from dotenv import load_dotenv
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

from google.cloud import firestore
from functions.process_outfit import process_outfit_image
from functions.add_new_item import add_new_item
from functions.confirm_match import confirm_match

ASSETS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'assets'))

# Track items we create so we can clean up
created_item_ids = []


def clear_database():
    """Delete all existing clothing_items and wear_logs (old schema data)."""
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
    print("--- Clearing clothing_items and wear_logs collections ---")
    for doc in db.collection('clothing_items').stream():
        doc.reference.delete()
    for doc in db.collection('wear_logs').stream():
        doc.reference.delete()
    print("Database cleared.")


def load_image(filename):
    path = os.path.join(ASSETS_DIR, filename)
    if not os.path.exists(path):
        print(f"ERROR: Image not found at {path}")
        sys.exit(1)
    with open(path, 'rb') as f:
        return f.read()


def cleanup():
    """Delete all test items we created."""
    if not created_item_ids:
        return
    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
    print(f"\n--- Cleanup: deleting {len(created_item_ids)} test items ---")
    for item_id in created_item_ids:
        db.collection('clothing_items').document(item_id).delete()
        # Also delete any wear logs for this item
        logs = db.collection('wear_logs').where('item_id', '==', item_id).stream()
        for log in logs:
            log.reference.delete()
    print("Cleanup complete.")


def test_multi_sample():
    clear_database()

    db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
    passed = 0
    failed = 0

    def check(description, condition):
        nonlocal passed, failed
        if condition:
            print(f"  PASS: {description}")
            passed += 1
        else:
            print(f"  FAIL: {description}")
            failed += 1

    # -------------------------------------------------------
    # Step 1: Process image 1.png and add items as new
    # -------------------------------------------------------
    print("\n=== Step 1: Process 1.png (first photo) ===")
    image1 = load_image('1.png')
    result1 = process_outfit_image(image1)

    check("Pipeline succeeded", result1['success'])
    check("Shirt detected", result1['shirt'] is not None)
    check("Pants detected", result1['pants'] is not None)

    # Add detected items as new
    added_items = {}  # item_type -> item_id
    for item_type in ['shirt', 'pants']:
        item = result1[item_type]
        if item is None:
            print(f"  WARNING: {item_type} not detected, skipping")
            continue

        # Whether matched or not, we want to add as new for this test
        # Use the embedding from the result
        if item.get('matched'):
            print(f"  NOTE: {item_type} matched existing item {item['item_id']} "
                  f"(similarity={item['similarity']:.4f}), still adding as new for test")

        embedding = item.get('embedding')
        if embedding is None:
            print(f"  ERROR: No embedding returned for {item_type}")
            failed += 1
            continue

        add_result = add_new_item(
            item_type=item_type,
            cropped_image_url=item['cropped_url'],
            embedding=embedding,
            original_photo_url=result1['original_photo_url'],
            log_wear=True
        )
        added_items[item_type] = add_result['item_id']
        created_item_ids.append(add_result['item_id'])
        print(f"  Added {item_type} as new item: {add_result['item_id']}")

    check("At least one item added", len(added_items) > 0)

    # Verify initial state: each item has exactly 1 embedding
    for item_type, item_id in added_items.items():
        doc = db.collection('clothing_items').document(item_id).get().to_dict()
        check(f"{item_type} has 1 embedding initially", len(doc['embeddings']) == 1)
        check(f"{item_type} has 1 image_url initially", len(doc['image_urls']) == 1)
        check(f"{item_type} embedding is 1408-dim", len(doc['embeddings']['0']) == 1408)

    # -------------------------------------------------------
    # Step 2: Process image 1.1.png (same clothes, different angle)
    # -------------------------------------------------------
    print("\n=== Step 2: Process 1.1.png (same clothes, different angle) ===")
    image1_1 = load_image('1.1.png')
    result2 = process_outfit_image(image1_1)

    check("Pipeline succeeded", result2['success'])

    for item_type, item_id in added_items.items():
        item = result2.get(item_type)
        if item is None:
            print(f"  WARNING: {item_type} not detected in 1.1.png")
            failed += 1
            continue

        check(f"{item_type} matched in 1.1.png", item['matched'])
        if item['matched']:
            check(f"{item_type} matched correct item",
                  item['item_id'] == item_id)
            check(f"{item_type} similarity > 0.85",
                  item['similarity'] > 0.85)
            check(f"{item_type} embedding returned for matched item",
                  item.get('embedding') is not None)
            print(f"  {item_type} similarity: {item['similarity']:.4f}")

    # -------------------------------------------------------
    # Step 3: Confirm match with embedding (adds sample)
    # -------------------------------------------------------
    print("\n=== Step 3: Confirm matches (should add samples) ===")
    for item_type, item_id in added_items.items():
        item = result2.get(item_type)
        if item is None or not item.get('matched'):
            continue

        confirm_result = confirm_match(
            item_id=item['item_id'],
            item_type=item_type,
            original_photo_url=result2['original_photo_url'],
            similarity_score=item['similarity'],
            new_embedding=item['embedding'],
            cropped_url=item['cropped_url']
        )
        check(f"{item_type} confirm succeeded", confirm_result['success'])
        print(f"  {item_type} wear_count: {confirm_result['wear_count']}")

    # -------------------------------------------------------
    # Step 4: Verify multi-sample storage in Firestore
    # -------------------------------------------------------
    print("\n=== Step 4: Verify Firestore has 2 samples per item ===")
    for item_type, item_id in added_items.items():
        doc = db.collection('clothing_items').document(item_id).get().to_dict()
        num_embeddings = len(doc['embeddings'])
        num_urls = len(doc['image_urls'])
        check(f"{item_type} has 2 embeddings after confirm", num_embeddings == 2)
        check(f"{item_type} has 2 image_urls after confirm", num_urls == 2)
        check(f"{item_type} both embeddings are 1408-dim",
              all(len(e) == 1408 for e in doc['embeddings'].values()))
        check(f"{item_type} embeddings are different",
              doc['embeddings']['0'] != doc['embeddings']['1'])
        print(f"  {item_type}: {num_embeddings} embeddings, {num_urls} image_urls")

    # -------------------------------------------------------
    # Step 5: Process image 2.png (different clothes) - should NOT match
    # -------------------------------------------------------
    print("\n=== Step 5: Process 2.png (different clothes) ===")
    image2 = load_image('2.png')
    result3 = process_outfit_image(image2)

    check("Pipeline succeeded", result3['success'])
    for item_type in ['shirt', 'pants']:
        item = result3.get(item_type)
        if item is None:
            continue
        if item['matched'] and item['item_id'] in created_item_ids:
            print(f"  UNEXPECTED: {item_type} from 2.png matched test item "
                  f"{item['item_id']} (similarity={item['similarity']:.4f})")
            check(f"{item_type} from 2.png should NOT match test items", False)
        else:
            check(f"{item_type} from 2.png does not match test items", True)

    # -------------------------------------------------------
    # Summary
    # -------------------------------------------------------
    print(f"\n{'='*50}")
    print(f"Results: {passed} passed, {failed} failed")
    print(f"{'='*50}")
    return failed == 0


if __name__ == '__main__':
    try:
        success = test_multi_sample()
    except Exception as e:
        print(f"\nTest crashed: {e}")
        import traceback
        traceback.print_exc()
        success = False
    finally:
        cleanup()

    sys.exit(0 if success else 1)
