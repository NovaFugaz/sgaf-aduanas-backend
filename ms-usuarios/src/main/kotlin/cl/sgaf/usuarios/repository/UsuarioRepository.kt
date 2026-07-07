package cl.sgaf.usuarios.repository

import cl.sgaf.usuarios.domain.Aduana
import cl.sgaf.usuarios.domain.Rol
import cl.sgaf.usuarios.domain.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UsuarioRepository : JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {
    fun findByRun(run: String): Optional<Usuario>
    fun findByCorreo(correo: String): Optional<Usuario>
    fun findByRolAndAduanaAndActivoTrue(rol: Rol, aduana: Aduana): List<Usuario>
}
