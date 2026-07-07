package cl.sgaf.tramites.domain.metadata

import java.time.LocalDate

data class SalidaVehiculoMetadata(
    val patente: String,
    val marca: String,
    val modelo: String,
    val anio: Int,
    val paisDestino: String,
    val propietarioNombre: String,
    val propietarioRut: String,
    val fechaSalida: LocalDate,
    val documentoGenerado: Boolean = false
)
