package cl.sgaf.reportes.service

import cl.sgaf.reportes.domain.ReporteFlujoDiario
import cl.sgaf.reportes.domain.ReporteFlujoSemanal
import cl.sgaf.reportes.domain.ReporteVehiculos
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class ExcelExportService {

    private fun createHeaderStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.color = IndexedColors.WHITE.index
        font.bold = true
        style.setFont(font)
        
        // RGB #003366
        val color = XSSFColor(byteArrayOf(0, 51, 102), null)
        style.setFillForegroundColor(color)
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        return style
    }

    fun exportDailyFlow(report: ReporteFlujoDiario): ByteArray {
        val workbook = XSSFWorkbook()
        val headerStyle = createHeaderStyle(workbook)

        // Resumen Sheet
        val summarySheet = workbook.createSheet("Resumen")
        summarySheet.createFreezePane(0, 1)

        val headerRow = summarySheet.createRow(0)
        listOf("Hora", "Declaraciones SAG", "Aut. Menor", "Salida Vehículo", "Ingreso Vehículo").forEachIndexed { idx, valText ->
            val cell = headerRow.createCell(idx)
            cell.setCellValue(valText)
            cell.setCellStyle(headerStyle)
        }

        report.porHora.forEachIndexed { rowIdx, entry ->
            val row = summarySheet.createRow(rowIdx + 1)
            row.createCell(0).setCellValue(entry.hora)
            row.createCell(1).setCellValue(entry.declaracionesSag.toDouble())
            row.createCell(2).setCellValue(entry.autorizacionesMenor.toDouble())
            row.createCell(3).setCellValue(entry.salidasVehiculo.toDouble())
            row.createCell(4).setCellValue(entry.ingresosVehiculo.toDouble())
        }

        // Totales Row
        val totalRow = summarySheet.createRow(report.porHora.size + 1)
        totalRow.createCell(0).setCellValue("TOTALES")
        totalRow.createCell(1).setCellValue(report.totales.declaracionesSag.toDouble())
        totalRow.createCell(2).setCellValue(report.totales.autorizacionesMenor.toDouble())
        totalRow.createCell(3).setCellValue(report.totales.salidasVehiculo.toDouble())
        totalRow.createCell(4).setCellValue(report.totales.ingresosVehiculo.toDouble())

        // Detalle Sheet
        val detailSheet = workbook.createSheet("Detalle")
        detailSheet.createFreezePane(0, 1)

        val detailHeaderRow = detailSheet.createRow(0)
        listOf("Aduana", "Fecha", "Tasa Aprobación").forEachIndexed { idx, valText ->
            val cell = detailHeaderRow.createCell(idx)
            cell.setCellValue(valText)
            cell.setCellStyle(headerStyle)
        }

        val detailRow = detailSheet.createRow(1)
        detailRow.createCell(0).setCellValue(report.aduana)
        detailRow.createCell(1).setCellValue(report.fecha)
        detailRow.createCell(2).setCellValue(report.tasaAprobacion)

        for (i in 0..4) summarySheet.autoSizeColumn(i)
        for (i in 0..2) detailSheet.autoSizeColumn(i)

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()
        return out.toByteArray()
    }

    fun exportWeeklyFlow(report: ReporteFlujoSemanal): ByteArray {
        val workbook = XSSFWorkbook()
        val headerStyle = createHeaderStyle(workbook)

        // Resumen Sheet
        val summarySheet = workbook.createSheet("Resumen")
        summarySheet.createFreezePane(0, 1)

        val headerRow = summarySheet.createRow(0)
        listOf("Fecha", "Día", "Declaraciones SAG", "Aut. Menor", "Salida Vehículo", "Ingreso Vehículo").forEachIndexed { idx, valText ->
            val cell = headerRow.createCell(idx)
            cell.setCellValue(valText)
            cell.setCellStyle(headerStyle)
        }

        report.porDia.forEachIndexed { rowIdx, entry ->
            val row = summarySheet.createRow(rowIdx + 1)
            row.createCell(0).setCellValue(entry.fecha)
            row.createCell(1).setCellValue(entry.dia)
            row.createCell(2).setCellValue(entry.declaracionesSag.toDouble())
            row.createCell(3).setCellValue(entry.autorizacionesMenor.toDouble())
            row.createCell(4).setCellValue(entry.salidasVehiculo.toDouble())
            row.createCell(5).setCellValue(entry.ingresosVehiculo.toDouble())
        }

        // Totales Row
        val totalRow = summarySheet.createRow(report.porDia.size + 1)
        totalRow.createCell(0).setCellValue("TOTALES")
        totalRow.createCell(2).setCellValue(report.totales.declaracionesSag.toDouble())
        totalRow.createCell(3).setCellValue(report.totales.autorizacionesMenor.toDouble())
        totalRow.createCell(4).setCellValue(report.totales.salidasVehiculo.toDouble())
        totalRow.createCell(5).setCellValue(report.totales.ingresosVehiculo.toDouble())

        // Detalle Sheet
        val detailSheet = workbook.createSheet("Detalle")
        detailSheet.createFreezePane(0, 1)

        val detailHeaderRow = detailSheet.createRow(0)
        listOf("Aduana", "Desde", "Hasta", "Tasa Aprobación").forEachIndexed { idx, valText ->
            val cell = detailHeaderRow.createCell(idx)
            cell.setCellValue(valText)
            cell.setCellStyle(headerStyle)
        }

        val detailRow = detailSheet.createRow(1)
        detailRow.createCell(0).setCellValue(report.aduana)
        detailRow.createCell(1).setCellValue(report.desde)
        detailRow.createCell(2).setCellValue(report.hasta)
        detailRow.createCell(3).setCellValue(report.tasaAprobacion)

        for (i in 0..5) summarySheet.autoSizeColumn(i)
        for (i in 0..3) detailSheet.autoSizeColumn(i)

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()
        return out.toByteArray()
    }

    fun exportVehicles(report: ReporteVehiculos): ByteArray {
        val workbook = XSSFWorkbook()
        val headerStyle = createHeaderStyle(workbook)

        // Resumen Sheet
        val summarySheet = workbook.createSheet("Resumen")
        summarySheet.createFreezePane(0, 1)

        val headerRow = summarySheet.createRow(0)
        listOf("Fecha", "Día", "Chilenos Entrada", "Chilenos Salida", "Argentinos Entrada", "Argentinos Salida").forEachIndexed { idx, valText ->
            val cell = headerRow.createCell(idx)
            cell.setCellValue(valText)
            cell.setCellStyle(headerStyle)
        }

        report.porDia.forEachIndexed { rowIdx, entry ->
            val row = summarySheet.createRow(rowIdx + 1)
            row.createCell(0).setCellValue(entry.fecha)
            row.createCell(1).setCellValue(entry.dia)
            row.createCell(2).setCellValue(entry.chilenosEntrada.toDouble())
            row.createCell(3).setCellValue(entry.chilenosSalida.toDouble())
            row.createCell(4).setCellValue(entry.argentinosEntrada.toDouble())
            row.createCell(5).setCellValue(entry.argentinosSalida.toDouble())
        }

        // Totales Row
        val totalRow = summarySheet.createRow(report.porDia.size + 1)
        totalRow.createCell(0).setCellValue("TOTALES")
        totalRow.createCell(2).setCellValue(report.totales.chilenosEntrada.toDouble())
        totalRow.createCell(3).setCellValue(report.totales.chilenosSalida.toDouble())
        totalRow.createCell(4).setCellValue(report.totales.argentinosEntrada.toDouble())
        totalRow.createCell(5).setCellValue(report.totales.argentinosSalida.toDouble())

        // Detalle Sheet
        val detailSheet = workbook.createSheet("Detalle")
        detailSheet.createFreezePane(0, 1)

        val detailHeaderRow = detailSheet.createRow(0)
        listOf("Aduana", "Desde", "Hasta").forEachIndexed { idx, valText ->
            val cell = detailHeaderRow.createCell(idx)
            cell.setCellValue(valText)
            cell.setCellStyle(headerStyle)
        }

        val detailRow = detailSheet.createRow(1)
        detailRow.createCell(0).setCellValue(report.aduana)
        detailRow.createCell(1).setCellValue(report.desde)
        detailRow.createCell(2).setCellValue(report.hasta)

        for (i in 0..5) summarySheet.autoSizeColumn(i)
        for (i in 0..2) detailSheet.autoSizeColumn(i)

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()
        return out.toByteArray()
    }
}
