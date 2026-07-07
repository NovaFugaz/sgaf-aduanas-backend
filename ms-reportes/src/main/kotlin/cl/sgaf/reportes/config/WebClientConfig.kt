package cl.sgaf.reportes.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class WebClientConfig(
    @param:Value("\${sgaf.tramites-url:http://ms-tramites:8082}") private val tramitesUrl: String,
    @param:Value("\${sgaf.auditoria-url:http://ms-auditoria:8085}") private val auditoriaUrl: String
) {

    private fun buildWebClient(baseUrl: String): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .responseTimeout(Duration.ofSeconds(3))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(3, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(3, TimeUnit.SECONDS))
            }

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }

    @Bean
    fun tramitesWebClient(): WebClient = buildWebClient(tramitesUrl)

    @Bean
    fun auditoriaWebClient(): WebClient = buildWebClient(auditoriaUrl)
}
