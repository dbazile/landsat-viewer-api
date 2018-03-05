class PayloadReader:
    class _UNSET:
        pass

    class Error(Exception):
        pass

    def __init__(self, payload: dict):
        if not isinstance(payload, dict):
            raise PayloadReader.Error('payload must be a dictionary')
        self._payload = payload

    def bool(self, key, *, optional=False):
        value = self._read(key, optional)

        if (value is None and not optional) or not isinstance(value, bool):
            raise PayloadReader.Error("'{}' must be a boolean".format(key))

        return value

    def number(self, key, *, type_=float, min_value=None, max_value=None, default=_UNSET, optional=False):
        if default is not PayloadReader._UNSET:
            optional = True

        value = self._read(key, optional)

        if value is None and default is not PayloadReader._UNSET:
            return default

        try:
            value = type_(value)
        except TypeError:
            raise PayloadReader.Error("'{}' is not a valid {}".format(key, type_.__name__))

        if min_value is not None and max_value is not None and not min_value <= value <= max_value:
            raise PayloadReader.Error("'{}' must be a {} between {} and {}".format(key, type_.__name__, min_value, max_value))

        if min_value is not None and value < min_value:
            raise PayloadReader.Error("'{}' must be a {} gte {}".format(key, type_.__name__, min_value))

        if max_value is not None and value > max_value:
            raise PayloadReader.Error("'{}' must be a {} lte {}".format(key, type_.__name__, max_value))

        return value

    def string(self, key, *, min_length=0, max_length=256, optional=False):
        value = self._read(key, optional)

        value = str(value).strip()

        if min_length is not None and max_length is not None and not min_length <= len(value) <= max_length:
            raise PayloadReader.Error("'{}' must be a string between {} and {} chars in length".format(key, min_length, max_length))

        return value

    def _read(self, key, optional):
        value = self._payload.get(key)
        if value is None and not optional:
            raise PayloadReader.Error("'{}' is not set".format(key))
        return value
