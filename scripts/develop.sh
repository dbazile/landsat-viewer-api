#!/bin/bash -ex

export DEBUG=True

pipenv run ./manage.py runserver
