import os
import hmac
import logging
import functools
from flask import jsonify

logger = logging.getLogger(__name__)

API_KEY = os.environ.get('API_KEY', '')


def require_api_key(func):
    @functools.wraps(func)
    def wrapper(request, *args, **kwargs):
        # Allow CORS preflight through
        if request.method == 'OPTIONS':
            return func(request, *args, **kwargs)

        provided_key = request.headers.get('X-API-Key', '')
        if not API_KEY or not hmac.compare_digest(provided_key, API_KEY):
            source_ip = request.headers.get('X-Forwarded-For', request.remote_addr)
            logger.warning(
                'Unauthorized access attempt from %s on %s',
                source_ip, request.path
            )
            headers = {'Access-Control-Allow-Origin': '*'}
            return jsonify({'error': 'Unauthorized'}), 401, headers

        return func(request, *args, **kwargs)
    return wrapper
