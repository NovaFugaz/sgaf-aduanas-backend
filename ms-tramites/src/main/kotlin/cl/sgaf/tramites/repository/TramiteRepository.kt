package cl.sgaf.tramites.repository

import cl.sgaf.tramites.domain.Tramite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TramiteRepository : JpaRepository<Tramite, UUID>, JpaSpecificationExecutor<Tramite>
