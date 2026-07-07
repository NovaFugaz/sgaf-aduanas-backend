package cl.sgaf.reportes.config

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
                    .title("SGAF Microservicio de Reportes API")
                    .version("1.0.0")
                    .description("API para la generación de reportes y exportación en formato PDF o Excel de la actividad de trámites y auditorías en SGAF.")
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
            }
            operation
        }
    }
}
