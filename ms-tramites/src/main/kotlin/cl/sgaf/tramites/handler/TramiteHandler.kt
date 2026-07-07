package cl.sgaf.tramites.handler

import cl.sgaf.tramites.config.CurrentUser
import cl.sgaf.tramites.domain.EstadoTramite
import cl.sgaf.tramites.domain.TipoTramite
import cl.sgaf.tramites.dto.*
import cl.sgaf.tramites.service.TramiteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/tramites")
@Tag(name = "Trámites", description = "Endpoints para la creación y gestión de trámites SGAF")
class TramiteHandler(private val service: TramiteService) {

    @PostMapping
    @Operation(summary = "Crear nuevo trámite", description = "Permite a un PASAJERO crear su propio trámite, o a un FUNCIONARIO/ADMINISTRADOR registrar un trámite en representación.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Trámite creado exitosamente"),
        ApiResponse(responseCode = "400", description = "Datos de metadatos inválidos para el tipo de trámite")
    )
    fun createTramite(
        @Valid @RequestBody request: CrearTramiteRequest,
        @AuthenticationPrincipal currentUser: CurrentUser,
        @RequestHeader(name = "X-User-Aduana", required = false) aduanaHeader: String?
    ): ResponseEntity<APIResponse<TramiteResponse>> {
        val tramite = service.createTramite(request, currentUser, aduanaHeader)
        return ResponseEntity.status(HttpStatus.CREATED).body(APIResponse(data = TramiteResponse.fromEntity(tramite)))
    }

    @GetMapping
    @Operation(summary = "Obtener lista paginada de trámites", description = "Permite listar trámites aplicando filtros opcionales. El PASAJERO solo ve sus propios trámites. El FUNCIONARIO ve los de su aduana. El ADMINISTRADOR ve todos.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Búsqueda completada exitosamente")
    )
    fun getTramites(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) tipo: TipoTramite?,
        @RequestParam(required = false) estado: EstadoTramite?,
        @RequestParam(required = false) aduana: String?,
        @RequestParam(name = "solicitante_id", required = false) solicitanteId: UUID?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) desde: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) hasta: LocalDate?,
        @AuthenticationPrincipal currentUser: CurrentUser
    ): ResponseEntity<APIResponse<PaginatedData<TramiteResponse>>> {
        val paginated = service.findTramites(page, size, tipo, estado, aduana, solicitanteId, desde, hasta, currentUser)
        val list = paginated.content.map { TramiteResponse.fromEntity(it) }
        val responseData = PaginatedData(
            content = list,
            totalElements = paginated.totalElements,
            totalPages = paginated.totalPages
        )
        return ResponseEntity.ok(APIResponse(data = responseData))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener trámite por ID", description = "Permite ver el detalle de un trámite. Aplica las mismas reglas de acceso que la lista.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Trámite encontrado"),
        ApiResponse(responseCode = "403", description = "Acceso denegado a este trámite"),
        ApiResponse(responseCode = "404", description = "Trámite no encontrado")
    )
    fun getTramiteById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal currentUser: CurrentUser
    ): ResponseEntity<APIResponse<TramiteResponse>> {
        val tramite = service.findById(id, currentUser)
        return ResponseEntity.ok(APIResponse(data = TramiteResponse.fromEntity(tramite)))
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'FUNCIONARIO')")
    @Operation(summary = "Cambiar estado de trámite", description = "Modifica el estado de un trámite a través de la máquina de estados. Solo accesible por FUNCIONARIO o ADMINISTRADOR.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente"),
        ApiResponse(responseCode = "400", description = "Transición inválida o falta motivo de rechazo"),
        ApiResponse(responseCode = "404", description = "Trámite no encontrado")
    )
    fun cambiarEstado(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CambiarEstadoRequest,
        @AuthenticationPrincipal currentUser: CurrentUser
    ): ResponseEntity<APIResponse<TramiteResponse>> {
        val tramite = service.cambiarEstado(id, request, currentUser)
        return ResponseEntity.ok(APIResponse(data = TramiteResponse.fromEntity(tramite)))
    }

    @GetMapping("/{id}/documento")
    @Operation(summary = "Obtener documento imprimible", description = "Retorna una representación JSON del documento imprimible asociado al trámite (válido para SALIDA_VEHICULO y AUTORIZACION_MENOR).")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Documento obtenido exitosamente"),
        ApiResponse(responseCode = "404", description = "El trámite no genera documentos o no existe")
    )
    fun getDocumento(
        @PathVariable id: UUID,
        @AuthenticationPrincipal currentUser: CurrentUser
    ): ResponseEntity<APIResponse<Map<String, Any>>> {
        val doc = service.getDocumento(id, currentUser)
        return ResponseEntity.ok(APIResponse(data = doc))
    }

    @GetMapping("/mis-tramites")
    @PreAuthorize("hasRole('PASAJERO')")
    @Operation(summary = "Obtener mis trámites (últimos 30 días)", description = "Shorthand para pasajeros autenticados que retorna sus propios trámites creados en los últimos 30 días.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    )
    fun getMisTramites(
        @AuthenticationPrincipal currentUser: CurrentUser
    ): ResponseEntity<APIResponse<List<TramiteResponse>>> {
        val desde = LocalDate.now().minusDays(30)
        val paginated = service.findTramites(
            page = 0,
            size = 1000,
            tipo = null,
            estado = null,
            aduana = null,
            solicitanteId = currentUser.id,
            desde = desde,
            hasta = null,
            currentUser = currentUser
        )
        val list = paginated.content.map { TramiteResponse.fromEntity(it) }
        return ResponseEntity.ok(APIResponse(data = list))
    }
}
