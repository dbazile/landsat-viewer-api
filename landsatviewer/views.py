import time

from typing import Union

from django.conf import settings
from django.http import FileResponse, HttpRequest, HttpResponse, JsonResponse, HttpResponseRedirect

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
