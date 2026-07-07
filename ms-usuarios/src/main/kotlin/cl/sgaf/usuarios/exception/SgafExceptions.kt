package cl.sgaf.usuarios.exception

open class SgafException(
    val code: String,
    message: String,
    val field: String? = null
) : RuntimeException(message)

class UsuarioNotFoundException(message: String) : SgafException("USER_NOT_FOUND", message)

class RunAlreadyExistsException(message: String) : SgafException("RUN_ALREADY_EXISTS", message, "run")

class EmailAlreadyExistsException(message: String) : SgafException("EMAIL_ALREADY_EXISTS", message, "correo")

class SgafValidationException(field: String, message: String) : SgafException("BAD_REQUEST", message, field)

class ForbiddenException(message: String, code: String = "FORBIDDEN") : SgafException(code, message)

class UnauthorizedException(message: String, code: String = "UNAUTHORIZED") : SgafException(code, message)
