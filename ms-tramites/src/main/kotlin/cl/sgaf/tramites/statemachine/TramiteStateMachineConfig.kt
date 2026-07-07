package cl.sgaf.tramites.statemachine

import cl.sgaf.tramites.domain.EstadoTramite
import cl.sgaf.tramites.domain.EventoTramite
import org.springframework.context.annotation.Configuration
import org.springframework.statemachine.config.EnableStateMachineFactory
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer
import java.util.EnumSet

@Configuration
@EnableStateMachineFactory(name = ["tramiteStateMachineFactory"])
class TramiteStateMachineConfig : EnumStateMachineConfigurerAdapter<EstadoTramite, EventoTramite>() {

    override fun configure(states: StateMachineStateConfigurer<EstadoTramite, EventoTramite>) {
        states
            .withStates()
            .initial(EstadoTramite.PENDIENTE)
            .states(EnumSet.allOf(EstadoTramite::class.java))
    }

    override fun configure(transitions: StateMachineTransitionConfigurer<EstadoTramite, EventoTramite>) {
        transitions
            // Transition: PENDIENTE -> EN_REVISION
            .withExternal()
            .source(EstadoTramite.PENDIENTE).target(EstadoTramite.EN_REVISION)
            .event(EventoTramite.INICIAR_REVISION)
            .and()
            // Transition: EN_REVISION -> APROBADO
            .withExternal()
            .source(EstadoTramite.EN_REVISION).target(EstadoTramite.APROBADO)
            .event(EventoTramite.APROBAR)
            .and()
            // Transition: EN_REVISION -> RECHAZADO
            .withExternal()
            .source(EstadoTramite.EN_REVISION).target(EstadoTramite.RECHAZADO)
            .event(EventoTramite.RECHAZAR)
            .and()
            // Transition: PENDIENTE -> APROBADO (auto-approval)
            .withExternal()
            .source(EstadoTramite.PENDIENTE).target(EstadoTramite.APROBADO)
            .event(EventoTramite.APROBAR)
            .and()
            // Transition: PENDIENTE -> RECHAZADO (direct rejection is possible)
            .withExternal()
            .source(EstadoTramite.PENDIENTE).target(EstadoTramite.RECHAZADO)
            .event(EventoTramite.RECHAZAR)
            .and()
            // Transition: APROBADO -> ANULADO
            .withExternal()
            .source(EstadoTramite.APROBADO).target(EstadoTramite.ANULADO)
            .event(EventoTramite.ANULAR)
    }
}
