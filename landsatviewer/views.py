import time

from typing import Union

from django.conf import settings
from django.http import FileResponse, HttpRequest, HttpResponse, JsonResponse, HttpResponseRedirect
from django.views.decorators.cache import cache_page

from . import planet


CACHE_LONG       = 86400

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


#
# Helpers
#


def _create_error(status: int, message: str, **kwargs) -> HttpResponse:
    payload = {'error': message}
    for k, v in kwargs.items():
        payload[k] = v
    return JsonResponse(payload, status=status)
