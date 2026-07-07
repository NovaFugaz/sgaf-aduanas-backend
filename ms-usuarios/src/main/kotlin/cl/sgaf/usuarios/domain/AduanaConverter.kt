package cl.sgaf.usuarios.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class AduanaConverter : AttributeConverter<Aduana, String> {
    override fun convertToDatabaseColumn(attribute: Aduana?): String? {
        return attribute?.dbValue
    }

    override fun convertToEntityAttribute(dbData: String?): Aduana? {
        if (dbData == null) return null
        return Aduana.fromString(dbData) ?: throw IllegalArgumentException("Valor de aduana inválido: $dbData")
    }
}
