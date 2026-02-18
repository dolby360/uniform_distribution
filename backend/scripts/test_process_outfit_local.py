"""
Local test for the process_outfit pipeline using a real image.

Usage:
    cd backend
    python scripts/test_process_outfit_local.py

    Or with a custom image:
    python scripts/test_process_outfit_local.py --image-path ../assets/2.png
"""
import sys
import os
import argparse

# Add backend dir to path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from dotenv import load_dotenv
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

from functions.process_outfit import process_outfit_image


def main():
    parser = argparse.ArgumentParser(description='Test process_outfit pipeline locally')
    parser.add_argument(
        '--image-path',
        default=os.path.join(os.path.dirname(__file__), '..', '..', 'assets', '2.png'),
        help='Path to test image (default: assets/2.png)'
    )
    args = parser.parse_args()

    image_path = os.path.abspath(args.image_path)
    if not os.path.exists(image_path):
        print(f"Error: Image not found at {image_path}")
        sys.exit(1)

    print(f"Loading image: {image_path}")
    with open(image_path, 'rb') as f:
        image_bytes = f.read()
    print(f"Image size: {len(image_bytes)} bytes")

    print("\n--- Running process_outfit_image pipeline ---\n")
    try:
        result = process_outfit_image(image_bytes)
    except Exception as e:
        print(f"Pipeline failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

    print(f"Success: {result['success']}")
    print(f"Original photo URL: {result['original_photo_url'][:80]}...")

    for item_type in ['shirt', 'pants']:
        item = result.get(item_type)
        if item is None:
            print(f"\n{item_type.upper()}: Not detected")
            continue

        print(f"\n{item_type.upper()}:")
        if item['matched']:
            print(f"  Matched: YES")
            print(f"  Item ID: {item['item_id']}")
            print(f"  Similarity: {item['similarity']:.4f}")
            print(f"  Existing image URL: {item['image_url'][:80]}...")
        else:
            print(f"  Matched: NO (new item)")
            print(f"  Embedding dims: {len(item['embedding'])}")

        print(f"  Cropped URL: {item['cropped_url'][:80]}...")

    print("\nPipeline test complete!")


if __name__ == '__main__':
    main()
