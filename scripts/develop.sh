#!/bin/bash -ex

cd $(dirname $(dirname $0))

export ALLOWED_HOSTS=${ALLOWED_HOSTS:=localhost,127.0.0.1,::1}
export DEBUG=${DEBUG:=True}
export SECRET_KEY=${SECRET_KEY:=secret}

export ALLOWED_HOSTS DEBUG SECRET_KEY

pipenv run ./manage.py runserver
