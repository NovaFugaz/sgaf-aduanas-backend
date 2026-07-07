package cl.sgaf.usuarios.dto

import cl.sgaf.usuarios.domain.Aduana
import cl.sgaf.usuarios.domain.Rol
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ActualizarUsuarioRequest(
    @field:NotBlank(message = "El RUN no puede estar en blanco")
    @field:Pattern(regexp = "^\\d{7,8}-[\\dkK]$", message = "El RUN debe tener un formato chileno válido (ej. 12345678-9)")
    val run: String,

    @field:NotBlank(message = "El nombre no puede estar en blanco")
    @field:Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    val nombre: String,

    @field:NotBlank(message = "El correo no puede estar en blanco")
    @field:Email(message = "El correo debe tener un formato de email válido")
    val correo: String,

    @field:NotNull(message = "El rol es requerido")
    val rol: Rol,

    val aduana: Aduana?,

    @field:NotNull(message = "El estado activo es requerido")
    val activo: Boolean
)
