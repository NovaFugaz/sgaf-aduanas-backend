package cl.sgaf.tramites.config

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
class IntegrationClientConfig {

    @Value("\${INTEGRACIONES_URL:http://ms-integraciones:8083}")
    private lateinit var integracionesUrl: String

    @Value("\${AUDITORIA_URL:http://ms-auditoria:8085}")
    private lateinit var auditoriaUrl: String

    private fun createWebClient(baseUrl: String): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(10, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(10, TimeUnit.SECONDS))
            }

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }

    @Bean
    fun integracionWebClient(): WebClient {
        return createWebClient(integracionesUrl)
    }

    @Bean
    fun auditoriaWebClient(): WebClient {
        return createWebClient(auditoriaUrl)
    }
}
