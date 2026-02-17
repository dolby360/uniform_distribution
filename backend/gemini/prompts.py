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
