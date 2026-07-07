package cl.sgaf.tramites.domain.metadata

import java.time.LocalDate

data class MenorMetadata(
    val nombreMenor: String,
    val rutMenor: String?,
    val pasaporteMenor: String?,
    val fechaNacimientoMenor: LocalDate,
    val nombreAdultoResponsable: String,
    val rutAdultoResponsable: String,
    val tipoDocumentoAdulto: String,   // "cedula" | "pasaporte"
    val numeroDocumentoAdulto: String,
    val relacionConMenor: String       // "padre" | "madre" | "tutor" | "otro"
)
