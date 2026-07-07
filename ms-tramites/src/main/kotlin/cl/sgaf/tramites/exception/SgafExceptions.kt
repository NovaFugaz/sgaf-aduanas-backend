package cl.sgaf.tramites.exception

open class SgafException(
    val code: String,
    message: String,
    val field: String? = null
) : RuntimeException(message)

class TramiteNotFoundException(message: String) : SgafException("TRAMITE_NOT_FOUND", message)

class SgafValidationException(field: String, message: String) : SgafException("BAD_REQUEST", message, field)

class ForbiddenException(message: String, code: String = "FORBIDDEN") : SgafException(code, message)

class UnauthorizedException(message: String, code: String = "UNAUTHORIZED") : SgafException(code, message)

class InvalidTransitionException(message: String) : SgafException("INVALID_TRANSITION", message)
