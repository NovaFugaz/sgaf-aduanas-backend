package cl.sgaf.reportes.service

import cl.sgaf.reportes.domain.ReporteFlujoDiario
import cl.sgaf.reportes.domain.ReporteFlujoSemanal
import cl.sgaf.reportes.domain.ReporteVehiculos
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class PdfExportService {

    private val primaryColor = DeviceRgb(0, 51, 102) // #003366
    private val grayColor = DeviceRgb(128, 128, 128)

    fun generateDailyFlowPdf(report: ReporteFlujoDiario): ByteArray {
        val out = ByteArrayOutputStream()
        val writer = PdfWriter(out)
        val pdfDoc = PdfDocument(writer)
        val doc = Document(pdfDoc)

        // Title Header
        doc.add(Paragraph("SGAF · Aduanas Chile")
            .setFontColor(primaryColor)
            .setFontSize(24f)
            .setBold())
        
        doc.add(Paragraph("REPORTE DE FLUJO DIARIO DE TRÁMITES")
            .setFontSize(14f)
            .setBold()
            .setMarginBottom(10f))

        doc.add(Paragraph("Fecha: ${report.fecha}  |  Aduana: ${report.aduana}  |  Tasa Aprobación: ${String.format("%.2f%%", report.tasaAprobacion * 100)}")
            .setFontSize(10f)
            .setMarginBottom(20f))

        // Totales Table
        doc.add(Paragraph("Resumen de Totales").setFontSize(12f).setBold().setMarginBottom(5f))
        val totalTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 15f, 15f, 15f, 15f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)

        listOf("Tipo de Trámite", "Declaraciones SAG", "Aut. Menor", "Salida Vehículo", "Ingreso Vehículo").forEach { text ->
            totalTable.addHeaderCell(Cell().add(Paragraph(text).setFontColor(DeviceRgb.WHITE).setBold())
                .setBackgroundColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER))
        }

        totalTable.addCell(Cell().add(Paragraph("Total Registrado")))
        totalTable.addCell(Cell().add(Paragraph(report.totales.declaracionesSag.toString()).setTextAlignment(TextAlignment.CENTER)))
        totalTable.addCell(Cell().add(Paragraph(report.totales.autorizacionesMenor.toString()).setTextAlignment(TextAlignment.CENTER)))
        totalTable.addCell(Cell().add(Paragraph(report.totales.salidasVehiculo.toString()).setTextAlignment(TextAlignment.CENTER)))
        totalTable.addCell(Cell().add(Paragraph(report.totales.ingresosVehiculo.toString()).setTextAlignment(TextAlignment.CENTER)))
        
        doc.add(totalTable)

        // Hourly breakdown table
        doc.add(Paragraph("Detalle por Hora").setFontSize(12f).setBold().setMarginBottom(5f))
        val hourTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 20f, 20f, 20f, 20f)))
            .useAllAvailableWidth()
            .setMarginBottom(30f)

        listOf("Hora", "Declaraciones SAG", "Aut. Menor", "Salida Vehículo", "Ingreso Vehículo").forEach { text ->
            hourTable.addHeaderCell(Cell().add(Paragraph(text).setFontColor(DeviceRgb.WHITE).setBold())
                .setBackgroundColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER))
        }

        for (h in report.porHora) {
            hourTable.addCell(Cell().add(Paragraph(h.hora).setTextAlignment(TextAlignment.CENTER)))
            hourTable.addCell(Cell().add(Paragraph(h.declaracionesSag.toString()).setTextAlignment(TextAlignment.CENTER)))
            hourTable.addCell(Cell().add(Paragraph(h.autorizacionesMenor.toString()).setTextAlignment(TextAlignment.CENTER)))
            hourTable.addCell(Cell().add(Paragraph(h.salidasVehiculo.toString()).setTextAlignment(TextAlignment.CENTER)))
            hourTable.addCell(Cell().add(Paragraph(h.ingresosVehiculo.toString()).setTextAlignment(TextAlignment.CENTER)))
        }

        doc.add(hourTable)

        // Footer block
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        doc.add(Paragraph("Documento generado automáticamente por SGAF · $timestamp")
            .setFontSize(8f)
            .setFontColor(grayColor)
            .setTextAlignment(TextAlignment.CENTER))

        doc.close()
        return out.toByteArray()
    }

    fun generateWeeklyFlowPdf(report: ReporteFlujoSemanal): ByteArray {
        val out = ByteArrayOutputStream()
        val writer = PdfWriter(out)
        val pdfDoc = PdfDocument(writer)
        val doc = Document(pdfDoc)

        doc.add(Paragraph("SGAF · Aduanas Chile")
            .setFontColor(primaryColor)
            .setFontSize(24f)
            .setBold())
        
        doc.add(Paragraph("REPORTE DE FLUJO SEMANAL DE TRÁMITES")
            .setFontSize(14f)
            .setBold()
            .setMarginBottom(10f))

        doc.add(Paragraph("Rango: ${report.desde} a ${report.hasta}  |  Aduana: ${report.aduana}  |  Tasa Aprobación: ${String.format("%.2f%%", report.tasaAprobacion * 100)}")
            .setFontSize(10f)
            .setMarginBottom(20f))

        // Totales
        doc.add(Paragraph("Resumen General").setFontSize(12f).setBold().setMarginBottom(5f))
        val totalTable = Table(UnitValue.createPercentArray(floatArrayOf(40f, 15f, 15f, 15f, 15f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)

        listOf("Tipo de Trámite", "Declaraciones SAG", "Aut. Menor", "Salida Vehículo", "Ingreso Vehículo").forEach { text ->
            totalTable.addHeaderCell(Cell().add(Paragraph(text).setFontColor(DeviceRgb.WHITE).setBold())
                .setBackgroundColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER))
        }

        totalTable.addCell(Cell().add(Paragraph("Total Acumulado")))
        totalTable.addCell(Cell().add(Paragraph(report.totales.declaracionesSag.toString()).setTextAlignment(TextAlignment.CENTER)))
        totalTable.addCell(Cell().add(Paragraph(report.totales.autorizacionesMenor.toString()).setTextAlignment(TextAlignment.CENTER)))
        totalTable.addCell(Cell().add(Paragraph(report.totales.salidasVehiculo.toString()).setTextAlignment(TextAlignment.CENTER)))
        totalTable.addCell(Cell().add(Paragraph(report.totales.ingresosVehiculo.toString()).setTextAlignment(TextAlignment.CENTER)))
        
        doc.add(totalTable)

        // Detail
        doc.add(Paragraph("Detalle Diario").setFontSize(12f).setBold().setMarginBottom(5f))
        val weekTable = Table(UnitValue.createPercentArray(floatArrayOf(15f, 15f, 17f, 17f, 18f, 18f)))
            .useAllAvailableWidth()
            .setMarginBottom(30f)

        listOf("Fecha", "Día", "Declaraciones SAG", "Aut. Menor", "Salida Vehículo", "Ingreso Vehículo").forEach { text ->
            weekTable.addHeaderCell(Cell().add(Paragraph(text).setFontColor(DeviceRgb.WHITE).setBold())
                .setBackgroundColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER))
        }

        for (d in report.porDia) {
            weekTable.addCell(Cell().add(Paragraph(d.fecha).setTextAlignment(TextAlignment.CENTER)))
            weekTable.addCell(Cell().add(Paragraph(d.dia).setTextAlignment(TextAlignment.CENTER)))
            weekTable.addCell(Cell().add(Paragraph(d.declaracionesSag.toString()).setTextAlignment(TextAlignment.CENTER)))
            weekTable.addCell(Cell().add(Paragraph(d.autorizacionesMenor.toString()).setTextAlignment(TextAlignment.CENTER)))
            weekTable.addCell(Cell().add(Paragraph(d.salidasVehiculo.toString()).setTextAlignment(TextAlignment.CENTER)))
            weekTable.addCell(Cell().add(Paragraph(d.ingresosVehiculo.toString()).setTextAlignment(TextAlignment.CENTER)))
        }

        doc.add(weekTable)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        doc.add(Paragraph("Documento generado automáticamente por SGAF · $timestamp")
            .setFontSize(8f)
            .setFontColor(grayColor)
            .setTextAlignment(TextAlignment.CENTER))

        doc.close()
        return out.toByteArray()
    }

    fun generateVehiclesPdf(report: ReporteVehiculos): ByteArray {
        val out = ByteArrayOutputStream()
        val writer = PdfWriter(out)
        val pdfDoc = PdfDocument(writer)
        val doc = Document(pdfDoc)

        doc.add(Paragraph("SGAF · Aduanas Chile")
            .setFontColor(primaryColor)
            .setFontSize(24f)
            .setBold())
        
        doc.add(Paragraph("REPORTE DE CONTROL DE VEHÍCULOS")
            .setFontSize(14f)
            .setBold()
            .setMarginBottom(10f))

        doc.add(Paragraph("Rango: ${report.desde} a ${report.hasta}  |  Aduana: ${report.aduana}")
            .setFontSize(10f)
            .setMarginBottom(20f))

        // Totales Table
        doc.add(Paragraph("Resumen de Totales").setFontSize(12f).setBold().setMarginBottom(5f))
        val totalTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 20f, 20f, 20f, 20f)))
            .useAllAvailableWidth()
            .setMarginBottom(20f)

        listOf("Dirección", "Chilenos Entrada", "Chilenos Salida", "Argentinos Entrada", "Argentinos Salida").forEach { text ->
            totalTable.addHeaderCell(Cell().add(Paragraph(text).setFontColor(DeviceRgb.WHITE).setBold())
                .setBackgroundColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER))
        }

        totalTable.addCell(Cell().add(Paragraph("Total General")))
        totalTable.addCell(Cell().add(Paragraph(report.totales.chilenosEntrada.toString()).setTextAlignment(TextAlignment.CENTER)))
        totalTable.addCell(Cell().add(Paragraph(report.totales.chilenosSalida.toString()).setTextAlignment(TextAlignment.CENTER)))
        totalTable.addCell(Cell().add(Paragraph(report.totales.argentinosEntrada.toString()).setTextAlignment(TextAlignment.CENTER)))
        totalTable.addCell(Cell().add(Paragraph(report.totales.argentinosSalida.toString()).setTextAlignment(TextAlignment.CENTER)))

        doc.add(totalTable)

        // Detail Table
        doc.add(Paragraph("Tráfico Diario").setFontSize(12f).setBold().setMarginBottom(5f))
        val detailTable = Table(UnitValue.createPercentArray(floatArrayOf(12f, 12f, 19f, 19f, 19f, 19f)))
            .useAllAvailableWidth()
            .setMarginBottom(30f)

        listOf("Fecha", "Día", "Chil. Entrada", "Chil. Salida", "Arg. Entrada", "Arg. Salida").forEach { text ->
            detailTable.addHeaderCell(Cell().add(Paragraph(text).setFontColor(DeviceRgb.WHITE).setBold())
                .setBackgroundColor(primaryColor)
                .setTextAlignment(TextAlignment.CENTER))
        }

        for (d in report.porDia) {
            detailTable.addCell(Cell().add(Paragraph(d.fecha).setTextAlignment(TextAlignment.CENTER)))
            detailTable.addCell(Cell().add(Paragraph(d.dia).setTextAlignment(TextAlignment.CENTER)))
            detailTable.addCell(Cell().add(Paragraph(d.chilenosEntrada.toString()).setTextAlignment(TextAlignment.CENTER)))
            detailTable.addCell(Cell().add(Paragraph(d.chilenosSalida.toString()).setTextAlignment(TextAlignment.CENTER)))
            detailTable.addCell(Cell().add(Paragraph(d.argentinosEntrada.toString()).setTextAlignment(TextAlignment.CENTER)))
            detailTable.addCell(Cell().add(Paragraph(d.argentinosSalida.toString()).setTextAlignment(TextAlignment.CENTER)))
        }

        doc.add(detailTable)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        doc.add(Paragraph("Documento generado automáticamente por SGAF · $timestamp")
            .setFontSize(8f)
            .setFontColor(grayColor)
            .setTextAlignment(TextAlignment.CENTER))

        doc.close()
        return out.toByteArray()
    }
}
