package cl.sgaf.usuarios.config

import cl.sgaf.usuarios.domain.Rol
import cl.sgaf.usuarios.dto.APIResponse
import cl.sgaf.usuarios.dto.ErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

data class CurrentUser(
    val id: UUID,
    val rol: Rol
)

class HeaderAuthFilter(private val objectMapper: ObjectMapper) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        // Bypass validation for Swagger UI, API docs, and Actuator health
        if (path.contains("/swagger-ui") || 
            path.contains("/v3/api-docs") || 
            path.contains("/actuator")
        ) {
            filterChain.doFilter(request, response)
            return
        }

        val userIdHeader = request.getHeader("X-User-Id")
        val userRolHeader = request.getHeader("X-User-Rol")

        if (userIdHeader.isNullOrBlank() || userRolHeader.isNullOrBlank()) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Request bypassed gateway or missing headers")
            return
        }

        val rol = try {
            Rol.valueOf(userRolHeader.uppercase())
        } catch (e: Exception) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Rol de usuario inválido en cabecera")
            return
        }

        val userId = try {
            UUID.fromString(userIdHeader)
        } catch (e: Exception) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "ID de usuario inválido en cabecera")
            return
        }

        val currentUser = CurrentUser(id = userId, rol = rol)
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${rol.name}"))
        val auth = UsernamePasswordAuthenticationToken(currentUser, null, authorities)
        SecurityContextHolder.getContext().authentication = auth

        filterChain.doFilter(request, response)
    }

    private fun sendErrorResponse(
        response: HttpServletResponse,
        status: HttpStatus,
        code: String,
        message: String
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val errorResponse = APIResponse<Nothing>(
            data = null,
            error = ErrorResponse(code = code, message = message, field = null)
        )
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(private val objectMapper: ObjectMapper) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(12)
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/api/usuarios/swagger-ui/**",
                    "/api/usuarios/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/actuator/**"
                ).permitAll()
                auth.anyRequest().authenticated()
            }
            .addFilterBefore(HeaderAuthFilter(objectMapper), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
