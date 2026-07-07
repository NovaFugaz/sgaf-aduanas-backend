package cl.sgaf.tramites.service

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class FolioService(private val entityManager: EntityManager) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    @Transactional
    fun generateFolio(): String {
        val query = entityManager.createNativeQuery("SELECT nextval('tramite_folio_seq')")
        val num = (query.singleResult as Number).toLong()
        val dateStr = LocalDate.now().format(dateFormatter)
        val paddedNum = String.format("%06d", num)
        return "SGF-$dateStr-$paddedNum"
    }
}
