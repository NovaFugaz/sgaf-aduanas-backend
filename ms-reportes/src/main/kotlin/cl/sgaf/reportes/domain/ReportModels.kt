package cl.sgaf.reportes.domain

data class FlujoHoraEntry(
    val hora: String,
    val declaracionesSag: Int,
    val autorizacionesMenor: Int,
    val salidasVehiculo: Int,
    val ingresosVehiculo: Int
)

data class FlujoTotales(
    val declaracionesSag: Int,
    val autorizacionesMenor: Int,
    val salidasVehiculo: Int,
    val ingresosVehiculo: Int
)

data class ReporteFlujoDiario(
    val fecha: String,
    val aduana: String,
    val porHora: List<FlujoHoraEntry>,
    val totales: FlujoTotales,
    val tasaAprobacion: Double
)

data class FlujoDiaEntry(
    val fecha: String,
    val dia: String, // e.g. "Lunes"
    val declaracionesSag: Int,
    val autorizacionesMenor: Int,
    val salidasVehiculo: Int,
    val ingresosVehiculo: Int
)

data class ReporteFlujoSemanal(
    val desde: String,
    val hasta: String,
    val aduana: String,
    val porDia: List<FlujoDiaEntry>,
    val totales: FlujoTotales,
    val tasaAprobacion: Double
)

data class VehiculosDiaEntry(
    val fecha: String,
    val dia: String,
    val chilenosEntrada: Int,
    val chilenosSalida: Int,
    val argentinosEntrada: Int,
    val argentinosSalida: Int
)

data class VehiculosTotales(
    val chilenosEntrada: Int,
    val chilenosSalida: Int,
    val argentinosEntrada: Int,
    val argentinosSalida: Int
)

data class ReporteVehiculos(
    val desde: String,
    val hasta: String,
    val aduana: String,
    val porDia: List<VehiculosDiaEntry>,
    val totales: VehiculosTotales
)

data class DashboardMetrics(
    val pasajerosProcesadosHoy: Int,
    val vehiculosEnTransito: Int,
    val alertasPendientes: Int,
    val tramitesPendientes: Int,
    val tasaAprobacionSemana: Double,
    val aduana: String
)
