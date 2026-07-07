package cl.sgaf.tramites.dto

import cl.sgaf.tramites.domain.EstadoTramite
import cl.sgaf.tramites.domain.TipoTramite
import cl.sgaf.tramites.domain.Tramite
import java.time.OffsetDateTime
import java.util.UUID

data class TramiteResponse(
    val id: UUID,
    val folio: String,
    val tipo: TipoTramite,
    val estado: EstadoTramite,
    val solicitanteId: UUID,
    val funcionarioId: UUID?,
    val aduana: String,
    val metadata: Map<String, Any>,
    val motivoRechazo: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
) {
    companion object {
        fun fromEntity(entity: Tramite): TramiteResponse {
            return TramiteResponse(
                id = entity.id,
                folio = entity.folio,
                tipo = entity.tipo,
                estado = entity.estado,
                solicitanteId = entity.solicitanteId,
                funcionarioId = entity.funcionarioId,
                aduana = entity.aduana,
                metadata = entity.metadata,
                motivoRechazo = entity.motivoRechazo,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}
