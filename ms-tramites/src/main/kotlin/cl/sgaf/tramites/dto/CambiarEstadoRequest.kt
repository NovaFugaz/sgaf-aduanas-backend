package cl.sgaf.tramites.dto

import cl.sgaf.tramites.domain.EstadoTramite
import jakarta.validation.constraints.NotNull

data class CambiarEstadoRequest(
    @field:NotNull(message = "El nuevo estado es requerido")
    val nuevoEstado: EstadoTramite,

    val motivoRechazo: String?
)
