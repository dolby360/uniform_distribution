import unittest
from unittest.mock import patch, MagicMock
from PIL import Image
import io
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from gemini.vision_detector import VisionDetector
from utils.image_cropper import crop_clothing_item


def _make_test_image(width=640, height=480, color='blue'):
    """Create a test JPEG image and return its bytes."""
    img = Image.new('RGB', (width, height), color=color)
    buf = io.BytesIO()
    img.save(buf, format='JPEG')
    return buf.getvalue()


class TestVisionDetectorParsing(unittest.TestCase):
    """Tests that don't require a real Gemini API call."""

    def setUp(self):
        with patch('google.generativeai.configure'):
            with patch('google.generativeai.GenerativeModel'):
                self.detector = VisionDetector()

    def test_parse_clean_json(self):
        raw = '{"shirt": {"detected": true, "bounding_box": {"x_min": 0.2, "y_min": 0.1, "x_max": 0.8, "y_max": 0.5}, "confidence": 0.9}, "pants": {"detected": true, "bounding_box": {"x_min": 0.3, "y_min": 0.5, "x_max": 0.7, "y_max": 0.95}, "confidence": 0.85}}'
        result = self.detector._parse_response(raw)
        self.assertIn('shirt', result)
        self.assertIn('pants', result)
        self.assertTrue(result['shirt']['detected'])

    def test_parse_markdown_wrapped_json(self):
        raw = '```json\n{"shirt": {"detected": false}, "pants": {"detected": false}}\n```'
        result = self.detector._parse_response(raw)
        self.assertFalse(result['shirt']['detected'])
        self.assertFalse(result['pants']['detected'])

    def test_parse_invalid_json_raises(self):
        with self.assertRaises(ValueError):
            self.detector._parse_response('not json at all')

    def test_parse_missing_keys_raises(self):
        with self.assertRaises(ValueError):
            self.detector._parse_response('{"shirt": {"detected": true}}')


class TestValidateDetection(unittest.TestCase):

    def setUp(self):
        with patch('google.generativeai.configure'):
            with patch('google.generativeai.GenerativeModel'):
                self.detector = VisionDetector()

    def test_valid_detection(self):
        detection = {
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
        self.assertTrue(self.detector.validate_detection(detection))

    def test_valid_not_detected(self):
        detection = {
            'shirt': {'detected': False},
            'pants': {'detected': False}
        }
        self.assertTrue(self.detector.validate_detection(detection))

    def test_missing_key(self):
        self.assertFalse(self.detector.validate_detection({'shirt': {'detected': True}}))

    def test_missing_bbox_when_detected(self):
        detection = {
            'shirt': {'detected': True},
            'pants': {'detected': False}
        }
        self.assertFalse(self.detector.validate_detection(detection))


class TestImageCropper(unittest.TestCase):

    def test_crop_basic(self):
        image_data = _make_test_image(640, 480, 'red')
        bbox = {'x_min': 0.2, 'y_min': 0.1, 'x_max': 0.8, 'y_max': 0.6}
        cropped = crop_clothing_item(image_data, bbox, padding=0.1)

        cropped_img = Image.open(io.BytesIO(cropped))
        self.assertGreater(cropped_img.width, 0)
        self.assertGreater(cropped_img.height, 0)

    def test_crop_no_padding(self):
        image_data = _make_test_image(640, 480)
        bbox = {'x_min': 0.25, 'y_min': 0.25, 'x_max': 0.75, 'y_max': 0.75}
        cropped = crop_clothing_item(image_data, bbox, padding=0.0)

        cropped_img = Image.open(io.BytesIO(cropped))
        # Without padding, crop should be exactly 50% of original dimensions
        self.assertAlmostEqual(cropped_img.width, 320, delta=2)
        self.assertAlmostEqual(cropped_img.height, 240, delta=2)

    def test_crop_with_edge_bbox(self):
        """Padding should be clamped to image bounds."""
        image_data = _make_test_image(640, 480)
        bbox = {'x_min': 0.0, 'y_min': 0.0, 'x_max': 0.5, 'y_max': 0.5}
        cropped = crop_clothing_item(image_data, bbox, padding=0.2)

        cropped_img = Image.open(io.BytesIO(cropped))
        self.assertGreater(cropped_img.width, 0)
        self.assertGreater(cropped_img.height, 0)


class TestDetectClothingIntegration(unittest.TestCase):
    """Test detect_clothing with mocked Gemini API."""

    @patch('google.generativeai.configure')
    @patch('google.generativeai.GenerativeModel')
    def test_detect_clothing_mocked(self, mock_model_cls, mock_configure):
        mock_response = MagicMock()
        mock_response.text = '{"shirt": {"detected": true, "bounding_box": {"x_min": 0.2, "y_min": 0.1, "x_max": 0.8, "y_max": 0.5}, "confidence": 0.9}, "pants": {"detected": true, "bounding_box": {"x_min": 0.3, "y_min": 0.5, "x_max": 0.7, "y_max": 0.95}, "confidence": 0.85}}'
        mock_model_cls.return_value.generate_content.return_value = mock_response

        detector = VisionDetector()
        image_data = _make_test_image()
        result = detector.detect_clothing(image_data)

        self.assertIn('shirt', result)
        self.assertIn('pants', result)
        self.assertTrue(result['shirt']['detected'])
        self.assertTrue(result['pants']['detected'])
        self.assertAlmostEqual(result['shirt']['bounding_box']['x_min'], 0.2)


if __name__ == '__main__':
    unittest.main()
