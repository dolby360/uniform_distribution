import functions_framework
from flask import jsonify
import base64
from functions.process_outfit import process_outfit_image


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
