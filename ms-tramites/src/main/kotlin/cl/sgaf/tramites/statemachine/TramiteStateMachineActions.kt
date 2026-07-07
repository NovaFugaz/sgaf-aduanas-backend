package cl.sgaf.tramites.statemachine

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TramiteStateMachineActions {
    private val log = LoggerFactory.getLogger(TramiteStateMachineActions::class.java)

    fun onTransitionToAprobado() {
        log.info("Ejecutando acción: Trámite Aprobado")
    }

    fun onTransitionToRechazado() {
        log.info("Ejecutando acción: Trámite Rechazado")
    }
}
