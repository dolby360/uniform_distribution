import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from dotenv import load_dotenv
load_dotenv(os.path.join(os.path.dirname(__file__), '..', '.env'))

from gemini.vision_detector import VisionDetector
from utils.image_cropper import crop_clothing_item


def test_detection_with_real_image(image_path: str):
    """Test detection with a real outfit photo."""
    detector = VisionDetector()

    with open(image_path, 'rb') as f:
        image_bytes = f.read()

    print("Detecting clothing items...")
    result = detector.detect_clothing(image_bytes)

    print("\nDetection Result:")
    print(f"Shirt detected: {result['shirt']['detected']}")
    if result['shirt']['detected']:
        bbox = result['shirt']['bounding_box']
        print(f"  Bounding box: ({bbox['x_min']:.2f}, {bbox['y_min']:.2f}) -> ({bbox['x_max']:.2f}, {bbox['y_max']:.2f})")
        print(f"  Confidence: {result['shirt'].get('confidence', 'N/A')}")

    print(f"\nPants detected: {result['pants']['detected']}")
    if result['pants']['detected']:
        bbox = result['pants']['bounding_box']
        print(f"  Bounding box: ({bbox['x_min']:.2f}, {bbox['y_min']:.2f}) -> ({bbox['x_max']:.2f}, {bbox['y_max']:.2f})")
        print(f"  Confidence: {result['pants'].get('confidence', 'N/A')}")

    # Crop and save
    if result['shirt']['detected']:
        cropped_shirt = crop_clothing_item(image_bytes, result['shirt']['bounding_box'])
        with open('cropped_shirt.jpg', 'wb') as f:
            f.write(cropped_shirt)
        print("\nSaved cropped_shirt.jpg")

    if result['pants']['detected']:
        cropped_pants = crop_clothing_item(image_bytes, result['pants']['bounding_box'])
        with open('cropped_pants.jpg', 'wb') as f:
            f.write(cropped_pants)
        print("Saved cropped_pants.jpg")


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python test_detection_manual.py <image_path>")
        sys.exit(1)

    test_detection_with_real_image(sys.argv[1])
