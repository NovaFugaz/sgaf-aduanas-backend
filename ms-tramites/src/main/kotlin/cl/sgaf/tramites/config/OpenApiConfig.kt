package cl.sgaf.tramites.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.parameters.Parameter
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("SGAF Microservicio de Trámites API")
                    .version("1.0.0")
                    .description("API para la gestión de trámites y solicitudes en pasos fronterizos (SGAF). Exige cabeceras de autorización propagadas por el Gateway.")
            )
    }

    @Bean
    fun addGlobalHeaders(): OperationCustomizer {
        return OperationCustomizer { operation, _ ->
            val path = operation.summary ?: ""
            if (!path.contains("health", ignoreCase = true)) {
                operation.addParametersItem(
                    Parameter()
                        .`in`("header")
                        .name("X-User-Id")
                        .description("UUID del usuario autenticado (inyectado por Gateway)")
                        .required(false)
                        .schema(io.swagger.v3.oas.models.media.Schema<String>().type("string").format("uuid"))
                )
                operation.addParametersItem(
                    Parameter()
                        .`in`("header")
                        .name("X-User-Rol")
                        .description("Rol del usuario autenticado (inyectado por Gateway)")
                        .required(false)
                        .schema(io.swagger.v3.oas.models.media.Schema<String>().type("string")._enum(listOf("ADMINISTRADOR", "FUNCIONARIO", "PASAJERO")))
                )
                operation.addParametersItem(
                    Parameter()
                        .`in`("header")
                        .name("X-User-Aduana")
                        .description("Aduana del funcionario (inyectado por Gateway)")
                        .required(false)
                        .schema(io.swagger.v3.oas.models.media.Schema<String>().type("string"))
                )
            }
            operation
        }
    }
}
