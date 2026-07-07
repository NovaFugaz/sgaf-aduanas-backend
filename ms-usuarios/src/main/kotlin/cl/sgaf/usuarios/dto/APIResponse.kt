package cl.sgaf.usuarios.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.ALWAYS)
data class APIResponse<T>(
    val data: T?,
    val error: ErrorResponse? = null
)

@JsonInclude(JsonInclude.Include.ALWAYS)
data class ErrorResponse(
    val code: String,
    val message: String,
    val field: String?
)
