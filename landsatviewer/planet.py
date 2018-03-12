import json
import logging

import arrow
from django.conf import settings
from requests import Session


TILE_URL   = 'https://tiles.planet.com/data/v1/Landsat8L1G/{scene_id}/{z}/{x}/{y}.png'
SCENE_URL  = 'https://api.planet.com/data/v1/item-types/Landsat8L1G/items/{scene_id}'
SEARCH_URL = 'https://api.planet.com/data/v1/quick-search'


_log = logging.getLogger(__name__)

_transport = Session()
_transport.auth = (settings.PLANET_API_KEY, '')


def get_tile(scene_id: str, x: int, y: int, z: int):
    _log.debug('Requesting tile for scene `%s` (x=%s, y=%s, z=%s)', scene_id, x, y, z)

    response = _transport.get(TILE_URL.format(scene_id=scene_id, x=x, y=y, z=z))

    if response.status_code == 404:
        raise NotFound()
    elif response.status_code != 200:
        raise Error('Planet returned HTTP {}'.format(response.status_code))

    return response.iter_content(4096)


def get_scene(scene_id: str) -> dict:
    _log.debug('Requesting scene `%s`', scene_id)

    response = _transport.get(SCENE_URL.format(scene_id=scene_id))

    if response.status_code == 404:
        raise NotFound()
    elif response.status_code != 200:
        _log.error('Planet returned HTTP %s:\n---\nResponse:\n%s\n---', response.status_code, response.text)
        raise Error('Planet returned HTTP {}'.format(response.status_code))

    return _transform_feature(response.json())


def search(x: float, y: float, days_ago: int) -> dict:
    _log.debug('Searching (x=%s, y=%s, days_ago=%s)', x, y, days_ago)

    criteria = {
        'item_types': ['Landsat8L1G'],
        'filter': {
            'type': 'AndFilter',
            'config': [
                {
                    'type': 'DateRangeFilter',
                    'field_name': 'acquired',
                    'config': {
                        'gte': arrow.utcnow().replace(days=-days_ago).isoformat(),
                    },
                },
                {
                    'type': 'GeometryFilter',
                    'field_name': 'geometry',
                    'config': {
                        'type': 'Polygon',
                        'coordinates': _buffer_naive(x, y),
                    },
                },
            ],
        },
    }

    response = _transport.post(SEARCH_URL, json=criteria)

    if response.status_code != 200:
        _log.error('Planet returned HTTP %s:\n---\nResponse:\n%s\n---', response.status_code, response.text)
        raise Error('Planet returned HTTP {}'.format(response.status_code))

    results = response.json()

    features = results.get('features')
    if not isinstance(features, list):
        _log.error('Planet returned malformed FeatureCollection:\n---\nResponse:\n%s\n---', response.text)
        raise MalformedResponse('`features` is not a list')

    return {
        'type': 'FeatureCollection',
        'features': [_transform_feature(f) for f in features],
    }


#
# Helpers
#

def _buffer_naive(x: float, y: float, size=1.0) -> list:
    return [[
        [x - size, y - size],
        [x - size, y + size],
        [x + size, y + size],
        [x + size, y - size],
        [x - size, y - size],
    ]]


def _transform_feature(raw_feature: dict) -> dict:
    if not isinstance(raw_feature, dict):
        raise MalformedResponse('feature is not a dict')

    try:
        geometry = {
            'type':        raw_feature['geometry']['type'],
            'coordinates': raw_feature['geometry']['coordinates'],
        }
    except (KeyError, TypeError) as err:
        _log.error('Planet returned malformed `geometry`: %s \n---\nResponse: %s\n---', err, json.dumps(raw_feature, indent=4))
        raise MalformedResponse('could not extract `geometry`')

    try:
        feature_id = raw_feature['id']
    except KeyError:
        _log.error('Planet returned feature with missing `id`\n---\nResponse: %s\n---', json.dumps(raw_feature, indent=4))
        raise MalformedResponse('could not extract `id`')

    try:
        properties = {
            'acquired_on': raw_feature['properties']['acquired'],
            'cloud_cover': raw_feature['properties']['cloud_cover'],
            'resolution':  raw_feature['properties']['pixel_resolution'],
            'wrs_path':    '{:03}'.format(raw_feature['properties']['wrs_path']),
            'wrs_row':     '{:03}'.format(raw_feature['properties']['wrs_row']),
        }
    except (KeyError, TypeError) as err:
        _log.error('Planet returned malformed `properties`: %s \n---\nResponse: %s\n---', err, json.dumps(raw_feature, indent=4))
        raise MalformedResponse("could not parse `properties`")

    return {
        'id': feature_id,
        'geometry': geometry,
        'properties': properties,
        'type': 'Feature',
    }


#
# Errors
#


class Error(Exception):
    pass


class NotFound(Error):
    pass


class MalformedResponse(Error):
    def __init__(self, message):
        super().__init__(self, 'Planet returned malformed response: {}'.format(message))
