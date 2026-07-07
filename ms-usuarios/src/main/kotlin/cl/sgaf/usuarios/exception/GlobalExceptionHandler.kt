package cl.sgaf.usuarios.exception

import cl.sgaf.usuarios.dto.APIResponse
import cl.sgaf.usuarios.dto.ErrorResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(SgafException::class)
    fun handleSgafException(ex: SgafException): ResponseEntity<APIResponse<Nothing>> {
        val status = when (ex.code) {
            "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND
            "FORBIDDEN" -> HttpStatus.FORBIDDEN
            "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED
            "RUN_ALREADY_EXISTS", "EMAIL_ALREADY_EXISTS" -> HttpStatus.BAD_REQUEST
            else -> HttpStatus.BAD_REQUEST
        }
        val response = APIResponse<Nothing>(
            data = null,
            error = ErrorResponse(
                code = ex.code,
                message = ex.message ?: "Error en la solicitud",
                field = ex.field
            )
        )
        return ResponseEntity(response, status)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<APIResponse<Nothing>> {
        val fieldError = ex.bindingResult.fieldError
        val fieldName = fieldError?.field
        val message = fieldError?.defaultMessage ?: "Error de validación"
        
        val response = APIResponse<Nothing>(
            data = null,
            error = ErrorResponse(
                code = "BAD_REQUEST",
                message = message,
                field = fieldName
            )
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<APIResponse<Nothing>> {
        val violation = ex.constraintViolations.firstOrNull()
        val fieldName = violation?.propertyPath?.toString()?.substringAfterLast('.')
        val message = violation?.message ?: "Violación de restricción"

        val response = APIResponse<Nothing>(
            data = null,
            error = ErrorResponse(
                code = "BAD_REQUEST",
                message = message,
                field = fieldName
            )
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<APIResponse<Nothing>> {
        val response = APIResponse<Nothing>(
            data = null,
            error = ErrorResponse(
                code = "INTERNAL_SERVER_ERROR",
                message = ex.message ?: "Ocurrió un error inesperado",
                field = null
            )
        )
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
