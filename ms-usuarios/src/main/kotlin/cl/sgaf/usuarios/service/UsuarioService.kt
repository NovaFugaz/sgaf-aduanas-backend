package cl.sgaf.usuarios.service

import cl.sgaf.usuarios.config.CurrentUser
import cl.sgaf.usuarios.domain.Aduana
import cl.sgaf.usuarios.domain.Rol
import cl.sgaf.usuarios.domain.Usuario
import cl.sgaf.usuarios.dto.ActualizarUsuarioRequest
import cl.sgaf.usuarios.dto.CrearUsuarioRequest
import cl.sgaf.usuarios.exception.*
import cl.sgaf.usuarios.repository.UsuarioRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UsuarioService(
    private val repository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional(readOnly = true)
    fun findUsers(page: Int, size: Int, rol: Rol?, aduana: Aduana?): Page<Usuario> {
        val spec = Specification<Usuario> { root, _, cb ->
            val predicates = mutableListOf<Predicate>()
            
            // Exclude soft-deleted users (RUN ends with or contains "_deleted_")
            predicates.add(cb.notLike(root.get("run"), "%_deleted_%"))

            if (rol != null) {
                predicates.add(cb.equal(root.get<Rol>("rol"), rol))
            }
            if (aduana != null) {
                predicates.add(cb.equal(root.get<Aduana>("aduana"), aduana))
            }

            cb.and(*predicates.toTypedArray())
        }
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        return repository.findAll(spec, pageable)
    }

    @Transactional(readOnly = true)
    fun findById(id: UUID): Usuario {
        val usuario = repository.findById(id).orElseThrow {
            UsuarioNotFoundException("Usuario con ID $id no encontrado")
        }
        if (usuario.run.contains("_deleted_")) {
            throw UsuarioNotFoundException("Usuario con ID $id no encontrado")
        }
        return usuario
    }

    @Transactional
    fun createUsuario(request: CrearUsuarioRequest): Usuario {
        // Validation: aduana required when role is FUNCIONARIO or ADMINISTRADOR
        if ((request.rol == Rol.FUNCIONARIO || request.rol == Rol.ADMINISTRADOR) && request.aduana == null) {
            throw SgafValidationException("aduana", "La aduana es requerida para el rol de FUNCIONARIO o ADMINISTRADOR")
        }

        // Validation: unique RUN
        if (repository.findByRun(request.run).isPresent) {
            throw RunAlreadyExistsException("El RUN ${request.run} ya está registrado")
        }

        // Validation: unique correo
        if (repository.findByCorreo(request.correo).isPresent) {
            throw EmailAlreadyExistsException("El correo ${request.correo} ya está registrado")
        }

        val hashedPassword = passwordEncoder.encode(request.password)

        val usuario = Usuario(
            run = request.run,
            nombre = request.nombre,
            correo = request.correo,
            passwordHash = hashedPassword,
            rol = request.rol,
            aduana = if (request.rol == Rol.PASAJERO) null else request.aduana,
            activo = true
        )

        return repository.save(usuario)
    }

    @Transactional
    fun updateUsuario(id: UUID, request: ActualizarUsuarioRequest, currentUser: CurrentUser): Usuario {
        val existing = findById(id)

        // Validation: cannot change own role or deactivate own account
        if (id == currentUser.id) {
            if (request.rol != existing.rol) {
                throw SgafValidationException("rol", "No puedes cambiar tu propio rol")
            }
            if (!request.activo && existing.activo) {
                throw SgafValidationException("activo", "No puedes desactivar tu propia cuenta")
            }
        }

        // Validation: aduana required when role is FUNCIONARIO or ADMINISTRADOR
        if ((request.rol == Rol.FUNCIONARIO || request.rol == Rol.ADMINISTRADOR) && request.aduana == null) {
            throw SgafValidationException("aduana", "La aduana es requerida para el rol de FUNCIONARIO o ADMINISTRADOR")
        }

        // Validation: unique RUN (excluding own record)
        val byRun = repository.findByRun(request.run)
        if (byRun.isPresent && byRun.get().id != id) {
            throw RunAlreadyExistsException("El RUN ${request.run} ya está registrado por otro usuario")
        }

        // Validation: unique correo (excluding own record)
        val byCorreo = repository.findByCorreo(request.correo)
        if (byCorreo.isPresent && byCorreo.get().id != id) {
            throw EmailAlreadyExistsException("El correo ${request.correo} ya está registrado por otro usuario")
        }

        existing.run = request.run
        existing.nombre = request.nombre
        existing.correo = request.correo
        existing.rol = request.rol
        existing.aduana = if (request.rol == Rol.PASAJERO) null else request.aduana
        existing.activo = request.activo

        return repository.save(existing)
    }

    @Transactional
    fun activarUsuario(id: UUID): Usuario {
        val usuario = findById(id)
        usuario.activo = true
        return repository.save(usuario)
    }

    @Transactional
    fun desactivarUsuario(id: UUID, currentUser: CurrentUser): Usuario {
        val usuario = findById(id)

        if (id == currentUser.id) {
            throw SgafValidationException("id", "No puedes desactivar tu propia cuenta")
        }

        usuario.activo = false
        return repository.save(usuario)
    }

    @Transactional
    fun deleteUsuario(id: UUID, currentUser: CurrentUser): Usuario {
        val usuario = findById(id)

        if (id == currentUser.id) {
            throw SgafValidationException("id", "No puedes eliminar tu propia cuenta")
        }

        // Generate deletion suffix: _del_{seconds} (fits within VARCHAR(20))
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val suffix = "_d$timestamp" // e.g. _d1720311999 (12 characters)
        val maxRunLength = 20 - suffix.length // e.g. 8 characters
        val truncatedRun = if (usuario.run.length > maxRunLength) usuario.run.substring(0, maxRunLength) else usuario.run
        
        usuario.run = truncatedRun + suffix
        usuario.activo = false

        return repository.save(usuario)
    }

    @Transactional(readOnly = true)
    fun findFuncionariosByAduana(aduana: Aduana): List<Usuario> {
        return repository.findByRolAndAduanaAndActivoTrue(Rol.FUNCIONARIO, aduana)
    }
}
