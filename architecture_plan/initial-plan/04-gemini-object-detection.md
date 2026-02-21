# Step 4: Gemini Object Detection

## Goal

Integrate Gemini Vision API to detect shirts and pants in full-body photos and extract bounding box coordinates.

## ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│              Gemini Vision Object Detection                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Input: Full-body photo (base64 or bytes)                   │
│  ┌───────────────────────────────────┐                      │
│  │   📷 User's outfit photo           │                      │
│  │   - 4:3 aspect ratio               │                      │
│  │   - Full body visible              │                      │
│  └───────────────────────────────────┘                      │
│            │                                                 │
│            ▼                                                 │
│  ┌────────────────────────────────────────────────┐        │
│  │     Gemini API Call                            │        │
│  │  Model: gemini-1.5-flash                       │        │
│  │  Prompt: "Detect shirt and pants in this      │        │
│  │           full-body photo. Return JSON with    │        │
│  │           bounding box coordinates."           │        │
│  └────────────────────────────────────────────────┘        │
│            │                                                 │
│            ▼                                                 │
│  ┌────────────────────────────────────────────────┐        │
│  │     JSON Response                              │        │
│  │  {                                             │        │
│  │    "shirt": {                                  │        │
│  │      "detected": true,                         │        │
│  │      "bounding_box": {                         │        │
│  │        "x_min": 0.25, "y_min": 0.15,          │        │
│  │        "x_max": 0.75, "y_max": 0.55           │        │
│  │      }                                         │        │
│  │    },                                          │        │
│  │    "pants": {                                  │        │
│  │      "detected": true,                         │        │
│  │      "bounding_box": {                         │        │
│  │        "x_min": 0.30, "y_min": 0.50,          │        │
│  │        "x_max": 0.70, "y_max": 0.95           │        │
│  │      }                                         │        │
│  │    }                                           │        │
│  │  }                                             │        │
│  └────────────────────────────────────────────────┘        │
│            │                                                 │
│            ▼                                                 │
│  ┌────────────────────────────────────────────────┐        │
│  │   Image Cropping (PIL)                         │        │
│  │   - Extract shirt region                       │        │
│  │   - Extract pants region                       │        │
│  │   - Add 10% padding                            │        │
│  └────────────────────────────────────────────────┘        │
│            │                                                 │
│            ▼                                                 │
│  ┌───────────────┐  ┌───────────────┐                      │
│  │  👕 Shirt      │  │  👖 Pants      │                      │
│  │  Cropped Image │  │  Cropped Image │                      │
│  └───────────────┘  └───────────────┘                      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Data Schemas

### BoundingBox

```python
@dataclass
class BoundingBox:
    x_min: float  # 0.0 - 1.0 (normalized)
    y_min: float
    x_max: float
    y_max: float
```

### ItemDetection

```python
@dataclass
class ItemDetection:
    detected: bool
    bounding_box: Optional[BoundingBox]
    confidence: float
```

### DetectionResult

```python
@dataclass
class DetectionResult:
    shirt: ItemDetection
    pants: ItemDetection
```

## API Endpoint Signatures

N/A (utility function)

## File Structure

```
backend/
  gemini/
    __init__.py
    vision_detector.py             # Gemini Vision integration
    prompts.py                     # Prompt templates
  utils/
    __init__.py
    image_cropper.py               # Crop images using bounding boxes
  tests/
    test_gemini_detection.py       # Unit tests
    test_images/
      sample_outfit.jpg            # Test image
```

## Key Code Snippets

### backend/gemini/__init__.py

```python
from .vision_detector import VisionDetector

__all__ = ['VisionDetector']
```

### backend/gemini/prompts.py

```python
DETECTION_PROMPT = """
Analyze this full-body photo and detect the shirt and pants.
Return ONLY a JSON object with this exact structure (no markdown, no extra text):
{
  "shirt": {
    "detected": true/false,
    "bounding_box": {
      "x_min": 0.0-1.0,
      "y_min": 0.0-1.0,
      "x_max": 0.0-1.0,
      "y_max": 0.0-1.0
    },
    "confidence": 0.0-1.0
  },
  "pants": {
    "detected": true/false,
    "bounding_box": {
      "x_min": 0.0-1.0,
      "y_min": 0.0-1.0,
      "x_max": 0.0-1.0,
      "y_max": 0.0-1.0
    },
    "confidence": 0.0-1.0
  }
}

Coordinates are normalized (0.0 = left/top, 1.0 = right/bottom).
If an item is not clearly visible, set "detected" to false.
"""
```

### backend/gemini/vision_detector.py

```python
import google.generativeai as genai
import os
import json
from PIL import Image
import io
from typing import Dict, Optional

class VisionDetector:
    def __init__(self):
        genai.configure(api_key=os.getenv('GEMINI_API_KEY'))
        self.model = genai.GenerativeModel('gemini-1.5-flash')

    def detect_clothing(self, image_bytes: bytes) -> Dict:
        """
        Detect shirt and pants in full-body photo

        Args:
            image_bytes: Image data as bytes

        Returns:
            Dict with 'shirt' and 'pants' bounding boxes
        """
        # Convert bytes to PIL Image
        image = Image.open(io.BytesIO(image_bytes))

        from .prompts import DETECTION_PROMPT

        try:
            response = self.model.generate_content([DETECTION_PROMPT, image])

            # Parse JSON from response
            result = self._parse_response(response.text)
            return result

        except Exception as e:
            raise ValueError(f"Failed to detect clothing: {e}")

    def _parse_response(self, response_text: str) -> Dict:
        """
        Parse Gemini response and extract JSON

        Args:
            response_text: Raw response from Gemini

        Returns:
            Parsed detection result
        """
        try:
            # Remove markdown code blocks if present
            text = response_text.strip()

            if text.startswith('```'):
                # Extract content between ``` blocks
                parts = text.split('```')
                if len(parts) >= 3:
                    text = parts[1]
                    # Remove language identifier (e.g., 'json')
                    if text.startswith('json'):
                        text = text[4:]

            # Parse JSON
            result = json.loads(text.strip())

            # Validate structure
            assert 'shirt' in result, "Missing 'shirt' in response"
            assert 'pants' in result, "Missing 'pants' in response"

            return result

        except json.JSONDecodeError as e:
            raise ValueError(f"Failed to parse Gemini response as JSON: {e}\nResponse: {response_text}")
        except AssertionError as e:
            raise ValueError(f"Invalid response structure: {e}\nResponse: {response_text}")

    def validate_detection(self, detection: Dict) -> bool:
        """
        Validate detection result structure

        Args:
            detection: Detection result dict

        Returns:
            True if valid
        """
        required_keys = ['shirt', 'pants']
        for key in required_keys:
            if key not in detection:
                return False

            item = detection[key]
            if not isinstance(item, dict):
                return False

            if 'detected' not in item:
                return False

            if item['detected']:
                if 'bounding_box' not in item:
                    return False

                bbox = item['bounding_box']
                bbox_keys = ['x_min', 'y_min', 'x_max', 'y_max']
                if not all(k in bbox for k in bbox_keys):
                    return False

        return True
```

### backend/utils/__init__.py

```python
from .image_cropper import crop_clothing_item

__all__ = ['crop_clothing_item']
```

### backend/utils/image_cropper.py

```python
from PIL import Image
import io
from typing import Dict

def crop_clothing_item(image_bytes: bytes, bounding_box: Dict,
                       padding: float = 0.1) -> bytes:
    """
    Crop image using bounding box coordinates with padding

    Args:
        image_bytes: Original image as bytes
        bounding_box: Dict with x_min, y_min, x_max, y_max (normalized 0-1)
        padding: Additional padding around crop (default 10%)

    Returns:
        Cropped image as JPEG bytes
    """
    image = Image.open(io.BytesIO(image_bytes))
    width, height = image.size

    # Convert normalized coordinates to pixels
    x_min = int(bounding_box['x_min'] * width)
    y_min = int(bounding_box['y_min'] * height)
    x_max = int(bounding_box['x_max'] * width)
    y_max = int(bounding_box['y_max'] * height)

    # Add padding
    pad_x = int((x_max - x_min) * padding)
    pad_y = int((y_max - y_min) * padding)

    x_min = max(0, x_min - pad_x)
    y_min = max(0, y_min - pad_y)
    x_max = min(width, x_max + pad_x)
    y_max = min(height, y_max + pad_y)

    # Crop and return
    cropped = image.crop((x_min, y_min, x_max, y_max))

    output = io.BytesIO()
    cropped.save(output, format='JPEG', quality=90)
    return output.getvalue()
```

### backend/tests/test_gemini_detection.py

```python
import unittest
from gemini.vision_detector import VisionDetector
from utils.image_cropper import crop_clothing_item
from PIL import Image
import io

class TestGeminiDetection(unittest.TestCase):

    def setUp(self):
        self.detector = VisionDetector()

    def test_detect_clothing(self):
        """Test detection with sample image"""
        # Create test image
        test_image = Image.new('RGB', (640, 480), color='blue')
        img_bytes = io.BytesIO()
        test_image.save(img_bytes, format='JPEG')
        image_data = img_bytes.getvalue()

        # Detect clothing
        result = self.detector.detect_clothing(image_data)

        # Assertions
        self.assertIn('shirt', result)
        self.assertIn('pants', result)
        self.assertIsInstance(result['shirt'], dict)
        self.assertIsInstance(result['pants'], dict)

    def test_validate_detection(self):
        """Test detection validation"""
        valid_detection = {
            'shirt': {
                'detected': True,
                'bounding_box': {
                    'x_min': 0.2, 'y_min': 0.1,
                    'x_max': 0.8, 'y_max': 0.6
                },
                'confidence': 0.95
            },
            'pants': {
                'detected': True,
                'bounding_box': {
                    'x_min': 0.3, 'y_min': 0.5,
                    'x_max': 0.7, 'y_max': 0.9
                },
                'confidence': 0.88
            }
        }

        self.assertTrue(self.detector.validate_detection(valid_detection))

    def test_crop_image(self):
        """Test image cropping"""
        # Create test image
        test_image = Image.new('RGB', (640, 480), color='red')
        img_bytes = io.BytesIO()
        test_image.save(img_bytes, format='JPEG')
        image_data = img_bytes.getvalue()

        # Crop region
        bbox = {'x_min': 0.2, 'y_min': 0.1, 'x_max': 0.8, 'y_max': 0.6}
        cropped = crop_clothing_item(image_data, bbox, padding=0.1)

        # Verify cropped image
        cropped_image = Image.open(io.BytesIO(cropped))
        self.assertGreater(cropped_image.width, 0)
        self.assertGreater(cropped_image.height, 0)

if __name__ == '__main__':
    unittest.main()
```

## Acceptance Criteria

- [ ] Gemini API key configured and authenticated
- [ ] VisionDetector class successfully calls Gemini API
- [ ] Test photo with visible shirt and pants returns valid bounding boxes
- [ ] Bounding box coordinates are normalized (0.0-1.0 range)
- [ ] Cropped images extracted correctly with 10% padding
- [ ] Error handling for failed detections (no clothing visible)
- [ ] Unit tests pass with sample images
- [ ] Validation method correctly identifies valid/invalid detections

## Setup Instructions

1. **Get Gemini API Key**:
   - Visit https://aistudio.google.com/app/apikey
   - Create API key
   - Add to `.env`: `GEMINI_API_KEY=your-key-here`

2. **Test Detection**:
   ```bash
   python -m pytest backend/tests/test_gemini_detection.py
   ```

## Verification

### Manual Test Script

```python
# backend/scripts/test_detection_manual.py
from gemini.vision_detector import VisionDetector
from utils.image_cropper import crop_clothing_item
from storage.storage_client import StorageClient

def test_detection_with_real_image(image_path: str):
    """Test detection with a real outfit photo"""
    detector = VisionDetector()

    # Read image
    with open(image_path, 'rb') as f:
        image_bytes = f.read()

    print("Detecting clothing items...")
    result = detector.detect_clothing(image_bytes)

    print("\nDetection Result:")
    print(f"Shirt detected: {result['shirt']['detected']}")
    if result['shirt']['detected']:
        bbox = result['shirt']['bounding_box']
        print(f"  Bounding box: ({bbox['x_min']:.2f}, {bbox['y_min']:.2f}) → ({bbox['x_max']:.2f}, {bbox['y_max']:.2f})")
        print(f"  Confidence: {result['shirt'].get('confidence', 'N/A')}")

    print(f"\nPants detected: {result['pants']['detected']}")
    if result['pants']['detected']:
        bbox = result['pants']['bounding_box']
        print(f"  Bounding box: ({bbox['x_min']:.2f}, {bbox['y_min']:.2f}) → ({bbox['x_max']:.2f}, {bbox['y_max']:.2f})")
        print(f"  Confidence: {result['pants'].get('confidence', 'N/A')}")

    # Crop and save
    if result['shirt']['detected']:
        cropped_shirt = crop_clothing_item(image_bytes, result['shirt']['bounding_box'])
        with open('cropped_shirt.jpg', 'wb') as f:
            f.write(cropped_shirt)
        print("\n✓ Saved cropped_shirt.jpg")

    if result['pants']['detected']:
        cropped_pants = crop_clothing_item(image_bytes, result['pants']['bounding_box'])
        with open('cropped_pants.jpg', 'wb') as f:
            f.write(cropped_pants)
        print("✓ Saved cropped_pants.jpg")

if __name__ == '__main__':
    import sys
    if len(sys.argv) < 2:
        print("Usage: python test_detection_manual.py <image_path>")
        sys.exit(1)

    test_detection_with_real_image(sys.argv[1])
```

Run test:
```bash
python backend/scripts/test_detection_manual.py path/to/outfit_photo.jpg
```
