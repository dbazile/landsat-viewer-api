import os


BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

SECRET_KEY = os.getenv('SECRET_KEY', os.urandom(24).hex())

DEBUG = bool(os.getenv('DEBUG', False))

ALLOWED_HOSTS = os.getenv('ALLOWED_HOSTS', '.dev.bazile.org').split(',')


INSTALLED_APPS = [
    'django.contrib.contenttypes',
]

MIDDLEWARE = [
    'django.middleware.common.CommonMiddleware',
]

ROOT_URLCONF = 'landsatviewer.urls'

TEMPLATES = []

WSGI_APPLICATION = 'landsatviewer.wsgi.application'


# Database
# https://docs.djangoproject.com/en/2.0/ref/settings/#databases

DATABASES = {}


# Internationalization
# https://docs.djangoproject.com/en/2.0/topics/i18n/

LANGUAGE_CODE = 'en-us'

TIME_ZONE = 'US/Eastern'

USE_I18N = True

USE_L10N = True

USE_TZ = True
