package cl.sgaf.tramites.domain.metadata

import cl.sgaf.tramites.domain.TipoTramite
import cl.sgaf.tramites.exception.SgafValidationException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class MetadataValidator(private val objectMapper: ObjectMapper) {

    fun validateAndConvert(tipo: TipoTramite, metadataMap: Map<String, Any>): Any {
        return try {
            when (tipo) {
                TipoTramite.DECLARACION_SAG -> {
                    val metadata = objectMapper.convertValue(metadataMap, SagMetadata::class.java)
                    if (metadata.paisOrigen.isBlank()) {
                        throw SgafValidationException("paisOrigen", "El país de origen es requerido")
                    }
                    metadata
                }
                TipoTramite.AUTORIZACION_MENOR -> {
                    val metadata = objectMapper.convertValue(metadataMap, MenorMetadata::class.java)
                    if (metadata.nombreMenor.isBlank()) {
                        throw SgafValidationException("nombreMenor", "El nombre del menor es requerido")
                    }
                    if (metadata.nombreAdultoResponsable.isBlank()) {
                        throw SgafValidationException("nombreAdultoResponsable", "El nombre del adulto responsable es requerido")
                    }
                    if (metadata.rutAdultoResponsable.isBlank()) {
                        throw SgafValidationException("rutAdultoResponsable", "El RUT del adulto responsable es requerido")
                    }
                    if (metadata.numeroDocumentoAdulto.isBlank()) {
                        throw SgafValidationException("numeroDocumentoAdulto", "El número de documento del adulto es requerido")
                    }
                    if (metadata.tipoDocumentoAdulto != "cedula" && metadata.tipoDocumentoAdulto != "pasaporte") {
                        throw SgafValidationException("tipoDocumentoAdulto", "El tipo de documento debe ser 'cedula' o 'pasaporte'")
                    }
                    if (metadata.relacionConMenor != "padre" && metadata.relacionConMenor != "madre" && 
                        metadata.relacionConMenor != "tutor" && metadata.relacionConMenor != "otro") {
                        throw SgafValidationException("relacionConMenor", "La relación debe ser 'padre', 'madre', 'tutor' o 'otro'")
                    }
                    metadata
                }
                TipoTramite.SALIDA_VEHICULO -> {
                    val metadata = objectMapper.convertValue(metadataMap, SalidaVehiculoMetadata::class.java)
                    if (metadata.patente.isBlank()) {
                        throw SgafValidationException("patente", "La patente del vehículo es requerida")
                    }
                    if (metadata.marca.isBlank()) {
                        throw SgafValidationException("marca", "La marca del vehículo es requerida")
                    }
                    if (metadata.modelo.isBlank()) {
                        throw SgafValidationException("modelo", "El modelo del vehículo es requerido")
                    }
                    if (metadata.propietarioNombre.isBlank()) {
                        throw SgafValidationException("propietarioNombre", "El nombre del propietario es requerido")
                    }
                    if (metadata.propietarioRut.isBlank()) {
                        throw SgafValidationException("propietarioRut", "El RUT del propietario es requerido")
                    }
                    metadata
                }
                TipoTramite.INGRESO_VEHICULO -> {
                    val metadata = objectMapper.convertValue(metadataMap, IngresoVehiculoMetadata::class.java)
                    if (metadata.patenteArgentina.isBlank()) {
                        throw SgafValidationException("patenteArgentina", "La patente argentina es requerida")
                    }
                    if (metadata.marca.isBlank()) {
                        throw SgafValidationException("marca", "La marca del vehículo es requerida")
                    }
                    if (metadata.modelo.isBlank()) {
                        throw SgafValidationException("modelo", "El modelo del vehículo es requerido")
                    }
                    if (metadata.titularNombre.isBlank()) {
                        throw SgafValidationException("titularNombre", "El nombre del titular es requerido")
                    }
                    if (metadata.titularDocumento.isBlank()) {
                        throw SgafValidationException("titularDocumento", "El documento del titular es requerido")
                    }
                    metadata
                }
            }
        } catch (e: SgafValidationException) {
            throw e
        } catch (e: Exception) {
            throw SgafValidationException("metadata", "Estructura de metadatos inválida: ${e.message}")
        }
    }
}
