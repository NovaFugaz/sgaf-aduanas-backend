package cl.sgaf.tramites.domain.metadata

import java.time.LocalDate

data class IngresoVehiculoMetadata(
    val patenteArgentina: String,
    val marca: String,
    val modelo: String,
    val anio: Int,
    val titularNombre: String,
    val titularDocumento: String,
    val fechaIngreso: LocalDate,
    val fechaVencimiento: LocalDate,   // ingreso + 180 days
    val respuestaAduanaArg: String?    // raw response from ms-integraciones
)
