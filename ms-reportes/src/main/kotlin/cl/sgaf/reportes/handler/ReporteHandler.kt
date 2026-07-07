package cl.sgaf.reportes.handler

import cl.sgaf.reportes.domain.TipoReporte
import cl.sgaf.reportes.dto.APIResponse
import cl.sgaf.reportes.dto.ErrorResponse
import cl.sgaf.reportes.service.ExcelExportService
import cl.sgaf.reportes.service.PdfExportService
import cl.sgaf.reportes.service.ReporteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Endpoints para generación y exportación de reportes y métricas")
class ReporteHandler(
    private val service: ReporteService,
    private val pdfExportService: PdfExportService,
    private val excelExportService: ExcelExportService
) {

    @GetMapping("/flujo-diario")
    @Operation(summary = "Obtener reporte de flujo diario", description = "Retorna el flujo de trámites agrupados por hora para un día determinado.")
    fun getFlujoDiario(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fecha: LocalDate?,
        @RequestParam(required = false) aduana: String?
    ): ResponseEntity<APIResponse<Any>> {
        val targetDate = fecha ?: LocalDate.now()
        val data = service.getFlujoDiario(targetDate, aduana)
        return ResponseEntity.ok(APIResponse(data = data))
    }

    @GetMapping("/flujo-semanal")
    @Operation(summary = "Obtener reporte de flujo semanal", description = "Retorna el flujo de trámites agrupados por día para un rango de fechas (máx. 31 días).")
    fun getFlujoSemanal(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) desde: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) hasta: LocalDate?,
        @RequestParam(required = false) aduana: String?
    ): ResponseEntity<APIResponse<Any>> {
        val dateDesde = desde ?: LocalDate.now().minusDays(7)
        val dateHasta = hasta ?: LocalDate.now()

        if (ChronoUnit.DAYS.between(dateDesde, dateHasta) > 31) {
            return ResponseEntity.badRequest().body(
                APIResponse(
                    data = null,
                    error = ErrorResponse("BAD_REQUEST", "El rango de fechas no puede exceder los 31 días")
                )
            )
        }

        val data = service.getFlujoSemanal(dateDesde, dateHasta, aduana)
        return ResponseEntity.ok(APIResponse(data = data))
    }

    @GetMapping("/vehiculos")
    @Operation(summary = "Obtener reporte de vehículos", description = "Retorna el tráfico de vehículos chilenos y argentinos agrupados por día.")
    fun getVehiculos(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) desde: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) hasta: LocalDate?,
        @RequestParam(required = false) aduana: String?
    ): ResponseEntity<APIResponse<Any>> {
        val dateDesde = desde ?: LocalDate.now().minusDays(7)
        val dateHasta = hasta ?: LocalDate.now()

        val data = service.getVehiculos(dateDesde, dateHasta, aduana)
        return ResponseEntity.ok(APIResponse(data = data))
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Obtener métricas agregadas del dashboard", description = "Retorna métricas operativas clave para el panel del funcionario y administrador.")
    fun getDashboard(
        @RequestParam(required = false) aduana: String?
    ): ResponseEntity<APIResponse<Any>> {
        val data = service.getDashboard(aduana)
        return ResponseEntity.ok(APIResponse(data = data))
    }

    @GetMapping("/exportar")
    @Operation(summary = "Exportar reportes a PDF o Excel", description = "Genera y retorna el archivo descargable correspondiente al reporte y formato solicitados.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Archivo exportado exitosamente"),
        ApiResponse(responseCode = "400", description = "Parámetros inválidos o formato desconocido")
    )
    fun exportar(
        @RequestParam tipo: TipoReporte,
        @RequestParam formato: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fecha: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) desde: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) hasta: LocalDate?,
        @RequestParam(required = false) aduana: String?
    ): ResponseEntity<Any> {
        val dateFecha = fecha ?: LocalDate.now()
        val dateDesde = desde ?: LocalDate.now().minusDays(7)
        val dateHasta = hasta ?: LocalDate.now()

        val isPdf = formato.equals("pdf", ignoreCase = true)
        val isExcel = formato.equals("excel", ignoreCase = true)

        if (!isPdf && !isExcel) {
            return ResponseEntity.badRequest().body(
                APIResponse(
                    data = null,
                    error = ErrorResponse("BAD_REQUEST", "Formato no soportado: debe ser 'pdf' o 'excel'")
                )
            )
        }

        val aduanaLabel = aduana ?: "LosLibertadores"
        val cleanAduana = aduanaLabel.replace("\\s".toRegex(), "")

        return when (tipo) {
            TipoReporte.FLUJO_DIARIO -> {
                val report = service.getFlujoDiario(dateFecha, aduana)
                if (isPdf) {
                    val bytes = pdfExportService.generateDailyFlowPdf(report)
                    buildFileResponse(bytes, "reporte_flujo_diario_${dateFecha}_$cleanAduana.pdf", "application/pdf")
                } else {
                    val bytes = excelExportService.exportDailyFlow(report)
                    buildFileResponse(bytes, "reporte_flujo_diario_${dateFecha}_$cleanAduana.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                }
            }
            TipoReporte.FLUJO_SEMANAL -> {
                if (ChronoUnit.DAYS.between(dateDesde, dateHasta) > 31) {
                    return ResponseEntity.badRequest().body(
                        APIResponse(
                            data = null,
                            error = ErrorResponse("BAD_REQUEST", "El rango de fechas no puede exceder los 31 días")
                        )
                    )
                }
                val report = service.getFlujoSemanal(dateDesde, dateHasta, aduana)
                if (isPdf) {
                    val bytes = pdfExportService.generateWeeklyFlowPdf(report)
                    buildFileResponse(bytes, "reporte_flujo_semanal_${dateDesde}_a_${dateHasta}_$cleanAduana.pdf", "application/pdf")
                } else {
                    val bytes = excelExportService.exportWeeklyFlow(report)
                    buildFileResponse(bytes, "reporte_flujo_semanal_${dateDesde}_a_${dateHasta}_$cleanAduana.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                }
            }
            TipoReporte.VEHICULOS -> {
                val report = service.getVehiculos(dateDesde, dateHasta, aduana)
                if (isPdf) {
                    val bytes = pdfExportService.generateVehiclesPdf(report)
                    buildFileResponse(bytes, "reporte_vehiculos_${dateDesde}_a_${dateHasta}_$cleanAduana.pdf", "application/pdf")
                } else {
                    val bytes = excelExportService.exportVehicles(report)
                    buildFileResponse(bytes, "reporte_vehiculos_${dateDesde}_a_${dateHasta}_$cleanAduana.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                }
            }
        }
    }

    private fun buildFileResponse(bytes: ByteArray, filename: String, contentType: String): ResponseEntity<Any> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(contentType)
        headers.setContentDispositionFormData("attachment", filename)
        headers.cacheControl = "must-revalidate, post-check=0, pre-check=0"
        return ResponseEntity.ok()
            .headers(headers)
            .body(bytes)
    }
}
