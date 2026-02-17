from PIL import Image
import io


def resize_image(image_bytes: bytes, max_size: tuple = (1024, 1024)) -> bytes:
    """
    Resize image to maximum dimensions while maintaining aspect ratio.

    Args:
        image_bytes: Original image bytes
        max_size: Maximum (width, height)

    Returns:
        Resized image bytes
    """
    image = Image.open(io.BytesIO(image_bytes))
    image.thumbnail(max_size, Image.Resampling.LANCZOS)

    output = io.BytesIO()
    image.save(output, format='JPEG', quality=90)
    return output.getvalue()


def create_thumbnail(image_bytes: bytes, size: tuple = (200, 200)) -> bytes:
    """
    Create square thumbnail.

    Args:
        image_bytes: Original image bytes
        size: Thumbnail dimensions

    Returns:
        Thumbnail image bytes
    """
    image = Image.open(io.BytesIO(image_bytes))

    # Create square crop from center
    width, height = image.size
    min_dim = min(width, height)

    left = (width - min_dim) // 2
    top = (height - min_dim) // 2
    right = left + min_dim
    bottom = top + min_dim

    cropped = image.crop((left, top, right, bottom))
    cropped.thumbnail(size, Image.Resampling.LANCZOS)

    output = io.BytesIO()
    cropped.save(output, format='JPEG', quality=85)
    return output.getvalue()
