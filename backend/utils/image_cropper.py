from PIL import Image
import io
from typing import Dict


def crop_clothing_item(image_bytes: bytes, bounding_box: Dict,
                       padding: float = 0.1) -> bytes:
    """
    Crop image using bounding box coordinates with padding.

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
