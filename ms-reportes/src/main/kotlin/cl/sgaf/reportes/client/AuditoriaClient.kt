package cl.sgaf.reportes.client

import cl.sgaf.reportes.dto.APIResponse
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@JsonIgnoreProperties(ignoreUnknown = true)
data class AuditResumen(
    val totalHoy: Int,
    val porAccion: Map<String, Int>,
    val porEntidad: Map<String, Int>,
    val usuariosActivosHoy: Int
)

@Component
class AuditoriaClient(
    @param:Qualifier("auditoriaWebClient") private val webClient: WebClient
) {
    private val log = LoggerFactory.getLogger(AuditoriaClient::class.java)

    fun getResumen(): AuditResumen? {
        try {
            val responseType = object : ParameterizedTypeReference<APIResponse<AuditResumen>>() {}

            val apiResponse = webClient.get()
                .uri("/api/auditoria/eventos/resumen")
                .header("X-User-Rol", "ADMINISTRADOR")
                .retrieve()
                .bodyToMono(responseType)
                .block()

            return apiResponse?.data
        } catch (e: Exception) {
            log.error("Fallo de conexión con ms-auditoria: ${e.message}")
            return null
        }
    }
}
