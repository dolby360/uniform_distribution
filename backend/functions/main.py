import functions_framework
from flask import jsonify
import base64
from process_outfit import process_outfit_image
from process_manual_crop import process_manual_crop
from confirm_match import confirm_match
from add_new_item import add_new_item
from statistics import get_statistics
from item_detail import get_item_images, delete_item_image


@functions_framework.http
def process_outfit(request):
    """
    HTTP Cloud Function: Process outfit photo

    POST /process-outfit
    Body: { "image": "base64_encoded_image" }
    """
    # CORS headers
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST',
            'Access-Control-Allow-Headers': 'Content-Type',
        }
        return ('', 204, headers)

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        request_json = request.get_json()
        if not request_json or 'image' not in request_json:
            return jsonify({'success': False, 'error': 'Missing image data'}), 400, headers

        image_base64 = request_json['image']
        image_bytes = base64.b64decode(image_base64)

        result = process_outfit_image(image_bytes)

        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500, headers


@functions_framework.http
def process_manual_crop_handler(request):
    """
    HTTP Cloud Function: Process manually-cropped outfit images

    POST /process-manual-crop
    Body: {
        "original_image": "base64...",
        "shirt_image": "base64..." (optional),
        "pants_image": "base64..." (optional)
    }
    """
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST',
            'Access-Control-Allow-Headers': 'Content-Type',
        }
        return ('', 204, headers)

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        data = request.get_json()
        if not data or 'original_image' not in data:
            return jsonify({'success': False, 'error': 'Missing original_image'}), 400, headers

        if not data.get('shirt_image') and not data.get('pants_image'):
            return jsonify({'success': False, 'error': 'At least one crop (shirt_image or pants_image) is required'}), 400, headers

        original_bytes = base64.b64decode(data['original_image'])
        shirt_bytes = base64.b64decode(data['shirt_image']) if data.get('shirt_image') else None
        pants_bytes = base64.b64decode(data['pants_image']) if data.get('pants_image') else None

        result = process_manual_crop(original_bytes, shirt_bytes, pants_bytes)

        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500, headers


@functions_framework.http
def confirm_match_handler(request):
    """
    HTTP Cloud Function: Confirm match and log wear event

    POST /confirm-match
    Body: { "item_id": "abc123", "item_type": "shirt", "original_photo_url": "gs://..." }
    """
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST',
            'Access-Control-Allow-Headers': 'Content-Type',
        }
        return ('', 204, headers)

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        data = request.get_json()

        if not data or 'item_id' not in data or 'item_type' not in data or 'original_photo_url' not in data:
            return jsonify({'success': False, 'error': 'Missing required fields'}), 400, headers

        result = confirm_match(
            data['item_id'],
            data['item_type'],
            data['original_photo_url'],
            data.get('similarity_score'),
            data.get('embedding'),
            data.get('cropped_url')
        )
        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500, headers


@functions_framework.http
def add_new_item_handler(request):
    """
    HTTP Cloud Function: Add new clothing item

    POST /add-new-item
    Body: { "item_type": "pants", "cropped_image_url": "gs://...", "embedding": [...], "original_photo_url": "gs://...", "log_wear": true }
    """
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST',
            'Access-Control-Allow-Headers': 'Content-Type',
        }
        return ('', 204, headers)

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        data = request.get_json()

        required_fields = ['item_type', 'cropped_image_url', 'embedding', 'original_photo_url']
        if not data or not all(field in data for field in required_fields):
            return jsonify({'success': False, 'error': 'Missing required fields'}), 400, headers

        result = add_new_item(
            data['item_type'],
            data['cropped_image_url'],
            data['embedding'],
            data['original_photo_url'],
            data.get('log_wear', False)
        )
        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500, headers


@functions_framework.http
def statistics_handler(request):
    """
    HTTP Cloud Function: Get wardrobe statistics

    GET /statistics
    """
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET',
            'Access-Control-Allow-Headers': 'Content-Type',
        }
        return ('', 204, headers)

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        result = get_statistics()
        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({'error': str(e)}), 500, headers


@functions_framework.http
def get_item_images_handler(request):
    """
    HTTP Cloud Function: Get all images for a clothing item

    GET /get-item-images?item_id=xxx
    """
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET',
            'Access-Control-Allow-Headers': 'Content-Type',
        }
        return ('', 204, headers)

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        item_id = request.args.get('item_id')
        if not item_id:
            return jsonify({'success': False, 'error': 'Missing item_id'}), 400, headers

        result = get_item_images(item_id)
        status = 200 if result['success'] else 404
        return jsonify(result), status, headers

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500, headers


@functions_framework.http
def delete_item_image_handler(request):
    """
    HTTP Cloud Function: Delete a specific image from a clothing item

    POST /delete-item-image
    Body: { "item_id": "xxx", "image_index": 0 }
    """
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST',
            'Access-Control-Allow-Headers': 'Content-Type',
        }
        return ('', 204, headers)

    headers = {'Access-Control-Allow-Origin': '*'}

    try:
        data = request.get_json()
        if not data or 'item_id' not in data or 'image_index' not in data:
            return jsonify({'success': False, 'error': 'Missing required fields'}), 400, headers

        result = delete_item_image(data['item_id'], data['image_index'])
        return jsonify(result), 200, headers

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500, headers
