package cl.sgaf.reportes.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class APIResponse<T>(
    val data: T?,
    val error: ErrorResponse? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val code: String,
    val message: String,
    val field: String? = null
)
