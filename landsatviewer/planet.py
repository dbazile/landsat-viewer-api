import logging

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


def get_scene(scene_id: str):
    _log.debug('Requesting scene `%s`', scene_id)

    response = _transport.get(SCENE_URL.format(scene_id=scene_id))

    if response.status_code == 404:
        raise NotFound()
    elif response.status_code != 200:
        raise Error('Planet returned HTTP {}'.format(response.status_code))

    metadata = response.json()

    try:
        geometry = {
            'type':        metadata['geometry']['type'],
            'coordinates': metadata['geometry']['coordinates'],
        }
    except (KeyError, TypeError) as err:
        _log.error('Planet returned malformed `geometry`: %s \n---\nResponse: %s\n---', err, response.text)
        raise MalformedResponse('could not extract `geometry`')

    try:
        properties = {
            'acquired_on': metadata['properties']['acquired'],
            'cloud_cover': metadata['properties']['cloud_cover'],
            'resolution':  metadata['properties']['pixel_resolution'],
            'wrs_path':    '{:03}'.format(metadata['properties']['wrs_path']),
            'wrs_row':     '{:03}'.format(metadata['properties']['wrs_row']),
        }
    except (KeyError, TypeError) as err:
        _log.error('Planet returned malformed `properties`: %s \n---\nResponse: %s\n---', err, response.text)
        raise MalformedResponse("could not parse `properties`")

    return {
        'id': scene_id,
        'geometry': geometry,
        'properties': properties,
        'type': 'Feature',
    }


class Error(Exception):
    pass


class NotFound(Error):
    pass


class MalformedResponse(Error):
    def __init__(self, message):
        super().__init__(self, 'Planet returned malformed response: {}'.format(message))
