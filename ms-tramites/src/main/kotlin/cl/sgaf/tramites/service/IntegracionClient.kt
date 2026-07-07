package cl.sgaf.tramites.service

import cl.sgaf.tramites.domain.metadata.SagMetadata
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID

@Service
class IntegracionClient(
    private val integracionWebClient: WebClient,
    private val auditoriaWebClient: WebClient
) {
    private val log = LoggerFactory.getLogger(IntegracionClient::class.java)

    fun validarSag(tramiteId: UUID, metadata: SagMetadata): Mono<Map<String, Any>> {
        val requestBody = mapOf(
            "tramiteId" to tramiteId.toString(),
            "metadata" to metadata
        )
        return integracionWebClient.post()
            .uri("/api/integraciones/sag")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Map<String, Any>>() {})
            .timeout(Duration.ofSeconds(10))
            .onErrorResume { ex ->
                log.error("Fallo al validar SAG para tramite $tramiteId: ${ex.message}", ex)
                Mono.just(mapOf("resultadoValidacion" to "FALLO_INTEGRACION: ${ex.message}"))
            }
    }

    fun consultarAduanaArgentina(patenteArgentina: String, titularDocumento: String): Map<String, Any>? {
        val requestBody = mapOf(
            "patenteArgentina" to patenteArgentina,
            "titularDocumento" to titularDocumento
        )
        return try {
            integracionWebClient.post()
                .uri("/api/integraciones/aduana-argentina")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<Map<String, Any>>() {})
                .timeout(Duration.ofSeconds(5))
                .block()
        } catch (ex: Exception) {
            log.error("Error al consultar aduana argentina: ${ex.message}")
            throw ex
        }
    }

    fun registrarAuditoria(
        entidad: String,
        entidadId: UUID,
        accion: String,
        usuarioId: UUID,
        detalle: String
    ) {
        val body = mapOf(
            "entidad" to entidad,
            "entidadId" to entidadId.toString(),
            "accion" to accion,
            "usuarioId" to usuarioId.toString(),
            "detalle" to detalle,
            "timestamp" to OffsetDateTime.now().toString()
        )
        auditoriaWebClient.post()
            .uri("/api/auditoria/eventos")
            .bodyValue(body)
            .retrieve()
            .toBodilessEntity()
            .subscribe(
                { /* success */ },
                { ex ->
                    log.warn("Fallo al registrar evento de auditoría en ms-auditoria para tramite $entidadId: ${ex.message}")
                }
            )
    }
}
