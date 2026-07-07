package cl.sgaf.tramites.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "tramites")
class Tramite(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    var folio: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var tipo: TipoTramite,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estado: EstadoTramite = EstadoTramite.PENDIENTE,

    @Column(name = "solicitante_id", nullable = false)
    var solicitanteId: UUID,

    @Column(name = "funcionario_id")
    var funcionarioId: UUID? = null,

    @Column(nullable = false)
    var aduana: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    var metadata: Map<String, Any> = emptyMap(),

    @Column(name = "motivo_rechazo")
    var motivoRechazo: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = OffsetDateTime.now()
    }
}
