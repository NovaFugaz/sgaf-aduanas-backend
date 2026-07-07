package cl.sgaf.usuarios.dto

data class PaginatedData<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int
)
