package cl.sgaf.usuarios.dto

import cl.sgaf.usuarios.domain.Aduana
import cl.sgaf.usuarios.domain.Rol
import cl.sgaf.usuarios.domain.Usuario
import java.util.UUID

data class UsuarioResponse(
    val id: UUID,
    val run: String,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    val aduana: Aduana?,
    val activo: Boolean
) {
    companion object {
        fun fromEntity(usuario: Usuario): UsuarioResponse {
            return UsuarioResponse(
                id = usuario.id,
                run = usuario.run,
                nombre = usuario.nombre,
                correo = usuario.correo,
                rol = usuario.rol,
                aduana = usuario.aduana,
                activo = usuario.activo
            )
        }
    }
}
