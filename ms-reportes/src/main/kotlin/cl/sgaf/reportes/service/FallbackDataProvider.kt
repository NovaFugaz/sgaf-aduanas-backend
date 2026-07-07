package cl.sgaf.reportes.service

import cl.sgaf.reportes.domain.*
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class FallbackDataProvider {

    fun getFlujoDiario(fecha: LocalDate, aduana: String?): ReporteFlujoDiario {
        val aduanaName = aduana ?: "Los Libertadores"
        val entries = (0..23).map { hour ->
            val horaStr = String.format("%02d:00", hour)
            // Generate some mock values based on hour of the day
            val activityMultiplier = when (hour) {
                in 8..18 -> 3
                in 19..22 -> 2
                else -> 0
            }
            FlujoHoraEntry(
                hora = horaStr,
                declaracionesSag = (2..5).random() * activityMultiplier,
                autorizacionesMenor = (0..1).random() * activityMultiplier,
                salidasVehiculo = (1..3).random() * activityMultiplier,
                ingresosVehiculo = (2..4).random() * activityMultiplier
            )
        }

        val totalSag = entries.sumOf { it.declaracionesSag }
        val totalMenor = entries.sumOf { it.autorizacionesMenor }
        val totalSalida = entries.sumOf { it.salidasVehiculo }
        val totalIngreso = entries.sumOf { it.ingresosVehiculo }

        return ReporteFlujoDiario(
            fecha = fecha.toString(),
            aduana = aduanaName,
            porHora = entries,
            totales = FlujoTotales(totalSag, totalMenor, totalSalida, totalIngreso),
            tasaAprobacion = 0.87
        )
    }

    fun getFlujoSemanal(desde: LocalDate, hasta: LocalDate, aduana: String?): ReporteFlujoSemanal {
        val aduanaName = aduana ?: "Los Libertadores"
        val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("es-CL"))
        
        val dates = mutableListOf<LocalDate>()
        var curr = desde
        while (!curr.isAfter(hasta)) {
            dates.add(curr)
            curr = curr.plusDays(1)
        }

        val entries = dates.map { date ->
            val diaName = date.format(dayFormatter).replaceFirstChar { it.uppercase() }
            val isWeekend = date.dayOfWeek.value >= 6
            val multiplier = if (isWeekend) 2 else 1
            
            FlujoDiaEntry(
                fecha = date.toString(),
                dia = diaName,
                declaracionesSag = (20..40).random() * multiplier,
                autorizacionesMenor = (5..15).random() * multiplier,
                salidasVehiculo = (15..30).random() * multiplier,
                ingresosVehiculo = (25..45).random() * multiplier
            )
        }

        val totalSag = entries.sumOf { it.declaracionesSag }
        val totalMenor = entries.sumOf { it.autorizacionesMenor }
        val totalSalida = entries.sumOf { it.salidasVehiculo }
        val totalIngreso = entries.sumOf { it.ingresosVehiculo }

        return ReporteFlujoSemanal(
            desde = desde.toString(),
            hasta = hasta.toString(),
            aduana = aduanaName,
            porDia = entries,
            totales = FlujoTotales(totalSag, totalMenor, totalSalida, totalIngreso),
            tasaAprobacion = 0.91
        )
    }

    fun getVehiculos(desde: LocalDate, hasta: LocalDate, aduana: String?): ReporteVehiculos {
        val aduanaName = aduana ?: "Los Libertadores"
        val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("es-CL"))
        
        val dates = mutableListOf<LocalDate>()
        var curr = desde
        while (!curr.isAfter(hasta)) {
            dates.add(curr)
            curr = curr.plusDays(1)
        }

        val entries = dates.map { date ->
            val diaName = date.format(dayFormatter).replaceFirstChar { it.uppercase() }
            val isWeekend = date.dayOfWeek.value >= 6
            val mult = if (isWeekend) 2 else 1

            VehiculosDiaEntry(
                fecha = date.toString(),
                dia = diaName,
                chilenosEntrada = (10..25).random() * mult,
                chilenosSalida = (8..22).random() * mult,
                argentinosEntrada = (12..30).random() * mult,
                argentinosSalida = (15..35).random() * mult
            )
        }

        val totalChEntrada = entries.sumOf { it.chilenosEntrada }
        val totalChSalida = entries.sumOf { it.chilenosSalida }
        val totalArgEntrada = entries.sumOf { it.argentinosEntrada }
        val totalArgSalida = entries.sumOf { it.argentinosSalida }

        return ReporteVehiculos(
            desde = desde.toString(),
            hasta = hasta.toString(),
            aduana = aduanaName,
            porDia = entries,
            totales = VehiculosTotales(totalChEntrada, totalChSalida, totalArgEntrada, totalArgSalida)
        )
    }

    fun getDashboard(aduana: String?): DashboardMetrics {
        val aduanaName = aduana ?: "Los Libertadores"
        return DashboardMetrics(
            pasajerosProcesadosHoy = 342,
            vehiculosEnTransito = 28,
            alertasPendientes = 3,
            tramitesPendientes = 7,
            tasaAprobacionSemana = 0.91,
            aduana = aduanaName
        )
    }
}
