from PIL import Image
import io
from typing import Dict, Optional

# Per-garment-type padding: (pad_x, pad_y_top, pad_y_bottom)
PADDING_BY_TYPE = {
    'shirt': (0.25, 0.10, 0.10),
    'pants': (0.15, 0.10, 0.25),
}
DEFAULT_PADDING = (0.10, 0.10, 0.10)


def crop_clothing_item(image_bytes: bytes, bounding_box: Dict,
                       padding: float = 0.1,
                       item_type: Optional[str] = None) -> bytes:
    """
    Crop image using bounding box coordinates with padding.

    Args:
        image_bytes: Original image as bytes
        bounding_box: Dict with x_min, y_min, x_max, y_max (normalized 0-1)
        padding: Additional padding around crop (default 10%), used when item_type is None
        item_type: Optional garment type ('shirt' or 'pants') for type-specific padding

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

    # Add padding — use type-specific values if available
    if item_type and item_type in PADDING_BY_TYPE:
        pad_x_ratio, pad_y_top_ratio, pad_y_bottom_ratio = PADDING_BY_TYPE[item_type]
    else:
        pad_x_ratio, pad_y_top_ratio, pad_y_bottom_ratio = (padding, padding, padding)

    box_w = x_max - x_min
    box_h = y_max - y_min
    pad_x = int(box_w * pad_x_ratio)
    pad_y_top = int(box_h * pad_y_top_ratio)
    pad_y_bottom = int(box_h * pad_y_bottom_ratio)

    x_min = max(0, x_min - pad_x)
    y_min = max(0, y_min - pad_y_top)
    x_max = min(width, x_max + pad_x)
    y_max = min(height, y_max + pad_y_bottom)

    # Crop and return
    cropped = image.crop((x_min, y_min, x_max, y_max))

    # Convert RGBA/P to RGB for JPEG compatibility
    if cropped.mode in ('RGBA', 'P'):
        cropped = cropped.convert('RGB')

    output = io.BytesIO()
    cropped.save(output, format='JPEG', quality=90)
    return output.getvalue()
