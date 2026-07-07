package cl.sgaf.tramites.domain.metadata

data class SagMetadata(
    val tieneAlimentos: Boolean,
    val tieneProductosVegetales: Boolean,
    val tieneProductosAnimales: Boolean,
    val tieneMascotas: Boolean,
    val descripcionItems: String?,
    val paisOrigen: String,
    val resultadoValidacion: String? = null
)
