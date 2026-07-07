package cl.sgaf.usuarios.handler

import cl.sgaf.usuarios.config.CurrentUser
import cl.sgaf.usuarios.domain.Aduana
import cl.sgaf.usuarios.domain.Rol
import cl.sgaf.usuarios.dto.APIResponse
import cl.sgaf.usuarios.dto.ActualizarUsuarioRequest
import cl.sgaf.usuarios.dto.CrearUsuarioRequest
import cl.sgaf.usuarios.dto.PaginatedData
import cl.sgaf.usuarios.dto.UsuarioResponse
import cl.sgaf.usuarios.exception.ForbiddenException
import cl.sgaf.usuarios.service.UsuarioService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Endpoints para la gestión de usuarios del sistema SGAF")
class UsuarioHandler(private val service: UsuarioService) {

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Obtener lista paginada de usuarios", description = "Retorna una lista paginada de usuarios filtrados opcionalmente por rol y/o aduana. Solo accesible por ADMINISTRADOR.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente"),
        ApiResponse(responseCode = "403", description = "Acceso denegado / Falta rol ADMINISTRADOR", content = [Content(schema = Schema(implementation = APIResponse::class))])
    )
    fun getUsuarios(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) rol: Rol?,
        @RequestParam(required = false) aduana: Aduana?
    ): ResponseEntity<APIResponse<PaginatedData<UsuarioResponse>>> {
        val userPage = service.findUsers(page, size, rol, aduana)
        val responseList = userPage.content.map { UsuarioResponse.fromEntity(it) }
        val paginatedData = PaginatedData(
            content = responseList,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages
        )
        return ResponseEntity.ok(APIResponse(data = paginatedData))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'FUNCIONARIO')")
    @Operation(summary = "Obtener usuario por ID", description = "Retorna los detalles de un usuario. El rol FUNCIONARIO solo puede consultar su propio perfil.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        ApiResponse(responseCode = "403", description = "Acceso denegado (ej. FUNCIONARIO intentando ver otro perfil)"),
        ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    )
    fun getUsuarioById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal currentUser: CurrentUser
    ): ResponseEntity<APIResponse<UsuarioResponse>> {
        if (currentUser.rol == Rol.FUNCIONARIO && currentUser.id != id) {
            throw ForbiddenException("No tienes permiso para ver el perfil de otro usuario")
        }
        val usuario = service.findById(id)
        return ResponseEntity.ok(APIResponse(data = UsuarioResponse.fromEntity(usuario)))
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear nuevo usuario", description = "Crea un nuevo registro de usuario en el sistema. La contraseña provista en texto plano se encripta con BCrypt (costo 12). Solo accesible por ADMINISTRADOR.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos de entrada inválidos / RUN o correo duplicado")
    )
    fun createUsuario(
        @Valid @RequestBody request: CrearUsuarioRequest
    ): ResponseEntity<APIResponse<UsuarioResponse>> {
        val usuario = service.createUsuario(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse(data = UsuarioResponse.fromEntity(usuario)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar usuario completamente", description = "Realiza una actualización completa del usuario. Un administrador no puede cambiar su propio rol ni desactivar su propia cuenta. Solo accesible por ADMINISTRADOR.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    )
    fun updateUsuario(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ActualizarUsuarioRequest,
        @AuthenticationPrincipal currentUser: CurrentUser
    ): ResponseEntity<APIResponse<UsuarioResponse>> {
        val usuario = service.updateUsuario(id, request, currentUser)
        return ResponseEntity.ok(APIResponse(data = UsuarioResponse.fromEntity(usuario)))
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Activar usuario", description = "Establece el estado activo de un usuario en true. Solo accesible por ADMINISTRADOR.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuario activado exitosamente"),
        ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    )
    fun activarUsuario(
        @PathVariable id: UUID
    ): ResponseEntity<APIResponse<UsuarioResponse>> {
        val usuario = service.activarUsuario(id)
        return ResponseEntity.ok(APIResponse(data = UsuarioResponse.fromEntity(usuario)))
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Desactivar usuario", description = "Establece el estado activo de un usuario en false. No se permite a un administrador desactivarse a sí mismo. Solo accesible por ADMINISTRADOR.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuario desactivado exitosamente"),
        ApiResponse(responseCode = "400", description = "Intento de desactivarse a sí mismo"),
        ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    )
    fun desactivarUsuario(
        @PathVariable id: UUID,
        @AuthenticationPrincipal currentUser: CurrentUser
    ): ResponseEntity<APIResponse<UsuarioResponse>> {
        val usuario = service.desactivarUsuario(id, currentUser)
        return ResponseEntity.ok(APIResponse(data = UsuarioResponse.fromEntity(usuario)))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Eliminar usuario (soft delete)", description = "Realiza un borrado lógico del usuario en el sistema. Modifica el RUN añadiendo el sufijo '_deleted_{timestamp}' para liberar la restricción de unicidad y establece activo = false. No se permite eliminarse a sí mismo. Solo accesible por ADMINISTRADOR.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuario eliminado exitosamente"),
        ApiResponse(responseCode = "400", description = "Intento de eliminarse a sí mismo"),
        ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    )
    fun deleteUsuario(
        @PathVariable id: UUID,
        @AuthenticationPrincipal currentUser: CurrentUser
    ): ResponseEntity<APIResponse<UsuarioResponse>> {
        val usuario = service.deleteUsuario(id, currentUser)
        return ResponseEntity.ok(APIResponse(data = UsuarioResponse.fromEntity(usuario)))
    }

    @GetMapping("/por-aduana/{aduana}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'FUNCIONARIO')")
    @Operation(summary = "Obtener funcionarios activos por aduana", description = "Retorna una lista de todos los funcionarios activos que pertenecen a un paso fronterizo determinado. Accesible por ADMINISTRADOR o FUNCIONARIO.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lista de funcionarios obtenida exitosamente")
    )
    fun getFuncionariosByAduana(
        @PathVariable aduana: Aduana
    ): ResponseEntity<APIResponse<List<UsuarioResponse>>> {
        val usuarios = service.findFuncionariosByAduana(aduana)
        val responseList = usuarios.map { UsuarioResponse.fromEntity(it) }
        return ResponseEntity.ok(APIResponse(data = responseList))
    }
}
