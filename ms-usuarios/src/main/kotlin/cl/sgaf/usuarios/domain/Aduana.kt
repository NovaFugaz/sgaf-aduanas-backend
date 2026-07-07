package cl.sgaf.usuarios.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class Aduana(val dbValue: String) {
    LOS_LIBERTADORES("Los Libertadores"),
    PINO_HACHADO("Pino Hachado"),
    CARDENAL_SAMORE("Cardenal Samoré");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromString(value: String?): Aduana? {
            if (value.isNullOrBlank()) return null
            return entries.find { 
                it.name.equals(value, ignoreCase = true) || 
                it.dbValue.equals(value, ignoreCase = true) ||
                it.name.replace("_", "").equals(value.replace(" ", "").replace("_", ""), ignoreCase = true)
            }
        }
    }

    @JsonValue
    fun toJson(): String = name
}
