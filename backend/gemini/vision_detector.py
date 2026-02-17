import google.generativeai as genai
import os
import json
from PIL import Image
import io
from typing import Dict

from .prompts import DETECTION_PROMPT


class VisionDetector:
    def __init__(self):
        genai.configure(api_key=os.getenv('GEMINI_API_KEY'))
        self.model = genai.GenerativeModel('gemini-1.5-flash')

    def detect_clothing(self, image_bytes: bytes) -> Dict:
        """
        Detect shirt and pants in full-body photo.

        Args:
            image_bytes: Image data as bytes

        Returns:
            Dict with 'shirt' and 'pants' detection results
        """
        image = Image.open(io.BytesIO(image_bytes))

        try:
            response = self.model.generate_content([DETECTION_PROMPT, image])
            result = self._parse_response(response.text)
            return result
        except Exception as e:
            raise ValueError(f"Failed to detect clothing: {e}")

    def _parse_response(self, response_text: str) -> Dict:
        """
        Parse Gemini response and extract JSON.

        Args:
            response_text: Raw response from Gemini

        Returns:
            Parsed detection result
        """
        text = response_text.strip()

        if text.startswith('```'):
            parts = text.split('```')
            if len(parts) >= 3:
                text = parts[1]
                if text.startswith('json'):
                    text = text[4:]

        try:
            result = json.loads(text.strip())
        except json.JSONDecodeError as e:
            raise ValueError(
                f"Failed to parse Gemini response as JSON: {e}\n"
                f"Response: {response_text}"
            )

        if 'shirt' not in result or 'pants' not in result:
            raise ValueError(
                f"Invalid response structure, missing 'shirt' or 'pants'\n"
                f"Response: {response_text}"
            )

        return result

    def validate_detection(self, detection: Dict) -> bool:
        """
        Validate detection result structure.

        Args:
            detection: Detection result dict

        Returns:
            True if valid
        """
        for key in ['shirt', 'pants']:
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
