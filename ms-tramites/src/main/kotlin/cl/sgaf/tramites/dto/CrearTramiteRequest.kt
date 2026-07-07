package cl.sgaf.tramites.dto

import cl.sgaf.tramites.domain.TipoTramite
import jakarta.validation.constraints.NotNull

data class CrearTramiteRequest(
    @field:NotNull(message = "El tipo de trámite es requerido")
    val tipo: TipoTramite,

    @field:NotNull(message = "Los metadatos son requeridos")
    val metadata: Map<String, Any>
)
