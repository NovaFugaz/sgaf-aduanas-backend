package cl.sgaf.tramites.service

import cl.sgaf.tramites.config.CurrentUser
import cl.sgaf.tramites.domain.*
import cl.sgaf.tramites.domain.metadata.*
import cl.sgaf.tramites.dto.CambiarEstadoRequest
import cl.sgaf.tramites.dto.CrearTramiteRequest
import cl.sgaf.tramites.exception.ForbiddenException
import cl.sgaf.tramites.exception.InvalidTransitionException
import cl.sgaf.tramites.exception.SgafValidationException
import cl.sgaf.tramites.exception.TramiteNotFoundException
import cl.sgaf.tramites.repository.TramiteRepository
import jakarta.persistence.criteria.Predicate
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.messaging.support.MessageBuilder
import org.springframework.statemachine.StateMachineEventResult
import org.springframework.statemachine.config.StateMachineFactory
import org.springframework.statemachine.support.DefaultStateMachineContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class TramiteService(
    private val repository: TramiteRepository,
    private val folioService: FolioService,
    private val integracionClient: IntegracionClient,
    private val metadataValidator: MetadataValidator,
    private val stateMachineFactory: StateMachineFactory<EstadoTramite, EventoTramite>
) {
    private val log = LoggerFactory.getLogger(TramiteService::class.java)

    @Transactional
    fun createTramite(request: CrearTramiteRequest, currentUser: CurrentUser, aduanaHeader: String?): Tramite {
        // Enforce aduana: defaults to user's aduana or header or "Desconocida"
        val aduanaValue = currentUser.aduana ?: aduanaHeader ?: "Los Libertadores"

        // Folio generation
        val folioVal = folioService.generateFolio()

        // Create initial tramite entity
        val tramite = Tramite(
            folio = folioVal,
            tipo = request.tipo,
            estado = EstadoTramite.PENDIENTE,
            solicitanteId = currentUser.id,
            aduana = aduanaValue,
            metadata = request.metadata
        )

        // Type-specific logic
        when (request.tipo) {
            TipoTramite.DECLARACION_SAG -> {
                val sagMetadata = metadataValidator.validateAndConvert(request.tipo, request.metadata) as SagMetadata
                if (!sagMetadata.tieneAlimentos && !sagMetadata.tieneProductosVegetales &&
                    !sagMetadata.tieneProductosAnimales && !sagMetadata.tieneMascotas) {
                    // Auto-approval when no flags are true
                    tramite.estado = EstadoTramite.APROBADO
                    repository.save(tramite)
                } else {
                    // Stays PENDIENTE and calls ms-integraciones async
                    val saved = repository.save(tramite)
                    integracionClient.validarSag(saved.id, sagMetadata)
                        .subscribe { response ->
                            val resultado = response["resultadoValidacion"]?.toString() ?: "PENDIENTE_ACCION_MANUAL"
                            actualizarResultadoSag(saved.id, resultado)
                        }
                }
            }
            TipoTramite.INGRESO_VEHICULO -> {
                val ingresoMetadata = metadataValidator.validateAndConvert(request.tipo, request.metadata) as IngresoVehiculoMetadata
                var rawResponse: String? = null
                try {
                    val res = integracionClient.consultarAduanaArgentina(ingresoMetadata.patenteArgentina, ingresoMetadata.titularDocumento)
                    rawResponse = res?.toString() ?: "SUCCESS"
                } catch (e: Exception) {
                    rawResponse = "INTEGRATION_FAILED: ${e.message}"
                }

                val updatedMetadata = request.metadata.toMutableMap()
                updatedMetadata["respuestaAduanaArg"] = rawResponse
                updatedMetadata["fechaVencimiento"] = ingresoMetadata.fechaIngreso.plusDays(180).toString()

                tramite.metadata = updatedMetadata
                repository.save(tramite)
            }
            TipoTramite.AUTORIZACION_MENOR -> {
                metadataValidator.validateAndConvert(request.tipo, request.metadata)
                repository.save(tramite)
            }
            TipoTramite.SALIDA_VEHICULO -> {
                metadataValidator.validateAndConvert(request.tipo, request.metadata)
                repository.save(tramite)
            }
        }

        // Log audit
        integracionClient.registrarAuditoria(
            entidad = "TRAMITE",
            entidadId = tramite.id,
            accion = "TRAMITE_CREADO",
            usuarioId = currentUser.id,
            detalle = "Creado trámite tipo ${request.tipo} con folio ${tramite.folio}"
        )

        return tramite
    }

    @Transactional
    fun actualizarResultadoSag(tramiteId: UUID, resultado: String) {
        val tramite = repository.findById(tramiteId).orElse(null) ?: return
        val currentMetadata = tramite.metadata.toMutableMap()
        currentMetadata["resultadoValidacion"] = resultado
        tramite.metadata = currentMetadata
        
        val prevEstado = tramite.estado
        if (resultado.contains("APROBADO", ignoreCase = true) || resultado.equals("SUCCESS", ignoreCase = true)) {
            tramite.estado = EstadoTramite.APROBADO
        } else {
            // In case of negative response or failure, transition to RECHAZADO
            tramite.estado = EstadoTramite.RECHAZADO
            tramite.motivoRechazo = "Rechazo automático por validación SAG: $resultado"
        }
        
        repository.save(tramite)

        integracionClient.registrarAuditoria(
            entidad = "TRAMITE",
            entidadId = tramite.id,
            accion = "ESTADO_CAMBIADO",
            usuarioId = tramite.solicitanteId,
            detalle = "Validación SAG asíncrona completada. Estado: $prevEstado -> ${tramite.estado}"
        )
    }

    @Transactional(readOnly = true)
    fun findTramites(
        page: Int,
        size: Int,
        tipo: TipoTramite?,
        estado: EstadoTramite?,
        aduana: String?,
        solicitanteId: UUID?,
        desde: LocalDate?,
        hasta: LocalDate?,
        currentUser: CurrentUser
    ): Page<Tramite> {
        val spec = Specification<Tramite> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            // Role filtering
            when (currentUser.rol) {
                Rol.PASAJERO -> {
                    predicates.add(cb.equal(root.get<UUID>("solicitanteId"), currentUser.id))
                }
                Rol.FUNCIONARIO -> {
                    if (!currentUser.aduana.isNullOrBlank()) {
                        predicates.add(cb.equal(root.get<String>("aduana"), currentUser.aduana))
                    }
                }
                Rol.ADMINISTRADOR -> {
                    // No restriction
                }
            }

            // Optional filters
            if (tipo != null) {
                predicates.add(cb.equal(root.get<TipoTramite>("tipo"), tipo))
            }
            if (estado != null) {
                predicates.add(cb.equal(root.get<EstadoTramite>("estado"), estado))
            }
            if (!aduana.isNullOrBlank()) {
                predicates.add(cb.equal(root.get<String>("aduana"), aduana))
            }
            if (solicitanteId != null) {
                predicates.add(cb.equal(root.get<UUID>("solicitanteId"), solicitanteId))
            }
            if (desde != null) {
                val start = desde.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start))
            }
            if (hasta != null) {
                val end = hasta.plusDays(1).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
                predicates.add(cb.lessThan(root.get("createdAt"), end))
            }

            cb.and(*predicates.toTypedArray())
        }

        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        return repository.findAll(spec, pageable)
    }

    @Transactional(readOnly = true)
    fun findById(id: UUID, currentUser: CurrentUser): Tramite {
        val tramite = repository.findById(id).orElseThrow {
            TramiteNotFoundException("Trámite con ID $id no encontrado")
        }

        // Access verification
        when (currentUser.rol) {
            Rol.PASAJERO -> {
                if (tramite.solicitanteId != currentUser.id) {
                    throw ForbiddenException("No tienes permiso para ver este trámite")
                }
            }
            Rol.FUNCIONARIO -> {
                if (!currentUser.aduana.isNullOrBlank() && tramite.aduana != currentUser.aduana) {
                    throw ForbiddenException("No tienes permiso para ver trámites de otra aduana")
                }
            }
            Rol.ADMINISTRADOR -> {
                // Allowed
            }
        }

        return tramite
    }

    @Transactional
    fun cambiarEstado(id: UUID, request: CambiarEstadoRequest, currentUser: CurrentUser): Tramite {
        val tramite = repository.findById(id).orElseThrow {
            TramiteNotFoundException("Trámite con ID $id no encontrado")
        }

        // State Machine validation
        val event = when (request.nuevoEstado) {
            EstadoTramite.EN_REVISION -> EventoTramite.INICIAR_REVISION
            EstadoTramite.APROBADO -> EventoTramite.APROBAR
            EstadoTramite.RECHAZADO -> EventoTramite.RECHAZAR
            EstadoTramite.ANULADO -> EventoTramite.ANULAR
            else -> throw InvalidTransitionException("Estado destino desconocido")
        }

        val nextState = triggerStateMachineTransition(tramite.estado, event)
            ?: throw InvalidTransitionException("Transición de ${tramite.estado} a ${request.nuevoEstado} no es válida")

        // Validation for rejection reason
        if (nextState == EstadoTramite.RECHAZADO && request.motivoRechazo.isNullOrBlank()) {
            throw SgafValidationException("motivoRechazo", "El motivo de rechazo es obligatorio para el estado RECHAZADO")
        }

        val estadoAnterior = tramite.estado
        tramite.estado = nextState
        tramite.funcionarioId = currentUser.id
        tramite.motivoRechazo = if (nextState == EstadoTramite.RECHAZADO) request.motivoRechazo else null
        tramite.updatedAt = OffsetDateTime.now()

        val saved = repository.save(tramite)

        // Audit Event
        integracionClient.registrarAuditoria(
            entidad = "TRAMITE",
            entidadId = saved.id,
            accion = "ESTADO_CAMBIADO",
            usuarioId = currentUser.id,
            detalle = "Estado: $estadoAnterior → $nextState. Folio: ${saved.folio}"
        )

        return saved
    }

    private fun triggerStateMachineTransition(source: EstadoTramite, event: EventoTramite): EstadoTramite? {
        return try {
            val stateMachine = stateMachineFactory.getStateMachine(UUID.randomUUID().toString())
            stateMachine.stopReactively().block()
            stateMachine.stateMachineAccessor.doWithAllRegions { accessor ->
                accessor.resetStateMachineReactively(
                    DefaultStateMachineContext(source, null, null, null)
                ).block()
            }
            stateMachine.startReactively().block()

            val results = stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(event).build())).collectList().block()
            val transitioned = results?.any { it.resultType == StateMachineEventResult.ResultType.ACCEPTED } == true
            if (transitioned) stateMachine.state.id else null
        } catch (e: Exception) {
            log.error("Fallo de ejecución de State Machine: ${e.message}", e)
            null
        }
    }

    @Transactional(readOnly = true)
    fun getDocumento(id: UUID, currentUser: CurrentUser): Map<String, Any> {
        val tramite = findById(id, currentUser)

        if (tramite.tipo != TipoTramite.SALIDA_VEHICULO && tramite.tipo != TipoTramite.AUTORIZACION_MENOR) {
            throw TramiteNotFoundException("El tipo de trámite ${tramite.tipo} no genera documentos imprimibles")
        }

        return when (tramite.tipo) {
            TipoTramite.SALIDA_VEHICULO -> {
                mapOf(
                    "documento" to "DOCUMENTO_SALIDA_VEHICULO",
                    "folio" to tramite.folio,
                    "patente" to (tramite.metadata["patente"] ?: ""),
                    "marca" to (tramite.metadata["marca"] ?: ""),
                    "modelo" to (tramite.metadata["modelo"] ?: ""),
                    "propietario" to (tramite.metadata["propietarioNombre"] ?: ""),
                    "fechaSalida" to (tramite.metadata["fechaSalida"] ?: ""),
                    "estado" to tramite.estado
                )
            }
            TipoTramite.AUTORIZACION_MENOR -> {
                mapOf(
                    "documento" to "AUTORIZACION_VIAJE_MENOR",
                    "folio" to tramite.folio,
                    "nombreMenor" to (tramite.metadata["nombreMenor"] ?: ""),
                    "adultoResponsable" to (tramite.metadata["nombreAdultoResponsable"] ?: ""),
                    "relacion" to (tramite.metadata["relacionConMenor"] ?: ""),
                    "fechaNacimiento" to (tramite.metadata["fechaNacimientoMenor"] ?: ""),
                    "estado" to tramite.estado
                )
            }
            else -> throw TramiteNotFoundException("No soportado")
        }
    }
}
