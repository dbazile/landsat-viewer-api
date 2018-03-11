#!/bin/bash -ex

cd $(dirname $(dirname $0))

export ALLOWED_HOSTS=${ALLOWED_HOSTS:=localhost,127.0.0.1,::1}
export DEBUG=${DEBUG:=True}
export SECRET_KEY=${SECRET_KEY:=secret}


pipenv run ./manage.py runserver
