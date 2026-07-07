package cl.sgaf.reportes.client

import cl.sgaf.reportes.dto.APIResponse
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class TramiteDTO(
    val id: UUID,
    val folio: String,
    val tipo: String,
    val estado: String,
    val solicitanteId: UUID,
    val funcionarioId: UUID?,
    val aduana: String,
    val createdAt: String,
    val metadata: Map<String, Any>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaginatedTramites(
    val content: List<TramiteDTO>
)

@Component
class TramitesClient(
    @param:Qualifier("tramitesWebClient") private val webClient: WebClient
) {
    private val log = LoggerFactory.getLogger(TramitesClient::class.java)

    fun getTramites(desde: LocalDate, hasta: LocalDate, aduana: String?): List<TramiteDTO> {
        try {
            val responseType = object : ParameterizedTypeReference<APIResponse<PaginatedTramites>>() {}

            val apiResponse = webClient.get()
                .uri { builder ->
                    var b = builder.path("/api/tramites")
                        .queryParam("desde", desde.toString())
                        .queryParam("hasta", hasta.toString())
                        .queryParam("page", 0)
                        .queryParam("size", 10000)
                    if (!aduana.isNullOrBlank()) {
                        b = b.queryParam("aduana", aduana)
                    }
                    b.build()
                }
                .header("X-User-Id", "00000000-0000-0000-0000-000000000000")
                .header("X-User-Rol", "ADMINISTRADOR")
                .retrieve()
                .bodyToMono(responseType)
                .block()

            return apiResponse?.data?.content ?: emptyList()
        } catch (e: Exception) {
            log.error("Fallo de conexión con ms-tramites: ${e.message}")
            throw e
        }
    }
}
