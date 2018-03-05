import time

from typing import Union

from django.conf import settings
from django.http import FileResponse, HttpRequest, HttpResponse, JsonResponse, HttpResponseRedirect, StreamingHttpResponse
from django.views.decorators.cache import cache_page

from . import planet
from .utils import PayloadReader


CACHE_LONG       = 86400
CACHE_SHORT      = 300
DEFAULT_DAYS_AGO = 14

START_TIMESTAMP = time.time()


def favicon(_: HttpRequest, file_extension: str) -> Union[FileResponse, HttpResponseRedirect]:
    if file_extension.lower() != 'png':
        return HttpResponseRedirect('/favicon.png')

    fp = open('{}/landsatviewer/images/favicon.png'.format(settings.BASE_DIR), 'rb')

    response = FileResponse(fp)
    response['Content-Type'] = 'image/png'

    return response


def health_check(_: HttpRequest) -> HttpResponse:
    return JsonResponse({
        'uptime': round(time.time() - START_TIMESTAMP, 3),
    })


@cache_page(CACHE_LONG)
def get_scene(_: HttpRequest, scene_id: str) -> HttpResponse:
    try:
        return JsonResponse(planet.get_scene(scene_id))
    except planet.NotFound:
        return _create_error(404, 'scene not found', scene_id=scene_id)
    except planet.Error as err:
        return _create_error(500, 'scene fetch error: {}'.format(err), scene_id=scene_id)


@cache_page(CACHE_LONG)
def get_tile(_: HttpRequest, scene_id: str, x: int, y: int, z: int) -> StreamingHttpResponse:
    try:
        stream = planet.get_tile(scene_id, x, y, z)
    except planet.Error:
        stream = open('{}/landsatviewer/images/tile-error.png'.format(settings.BASE_DIR), 'rb')

    response = StreamingHttpResponse(stream)
    response['Content-Type'] = 'image/png'

    return response


@cache_page(CACHE_SHORT)
def search(request: HttpRequest) -> HttpResponse:
    reader = PayloadReader(request.GET)
    try:
        x = reader.number('x', min_value=-180, max_value=180)
        y = reader.number('y', min_value=-90, max_value=90)
        days_ago = reader.number('days_ago', min_value=1, max_value=30, type_=int, default=DEFAULT_DAYS_AGO)
    except PayloadReader.Error as err:
        return _create_error(400, 'malformed input: {}'.format(err), params=request.GET)

    try:
        return JsonResponse(planet.search(x, y, days_ago))
    except planet.Error as err:
        return _create_error(500, 'scene fetch error: {}'.format(err), params=dict(x=x, y=y, days_ago=days_ago))


#
# Helpers
#


def _create_error(status: int, message: str, **kwargs) -> HttpResponse:
    payload = {'error': message}
    for k, v in kwargs.items():
        payload[k] = v
    return JsonResponse(payload, status=status)
