package org.example.padelback.modules.reservas.infrastructure.scheduling;

import java.time.Clock;
import java.time.LocalDateTime;

import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ReservaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Barrido de señas vencidas.
 *
 * <p>Cuando una reserva PENDIENTE pasa su {@code expira_en}, deja de retener la cancha de inmediato:
 * la disponibilidad y el anti-doble-reserva ya la ignoran (expiración perezosa). Este job es solo
 * <b>cosmético</b>: pasa esas pendientes a CANCELADO para que no queden colgadas en el panel y en la DB.
 *
 * <p>Corre sobre todos los tenants: es tarea de sistema, sin scope por request (el UPDATE nativo no
 * pasa por el filtro Hibernate de tenant, que solo se activa dentro de un {@code TenantContext}).
 */
@Component
@RequiredArgsConstructor
public class SenaCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(SenaCleanupJob.class);

    private final ReservaJpaRepository reservaRepo;
    private final Clock clock;

    /** Cada 2 minutos (por defecto). El JVM está pineado a UTC, así que {@code now()} es UTC. */
    @Scheduled(cron = "${padel.sena.cleanup-cron:0 */2 * * * *}", zone = "UTC")
    @Transactional
    public void cancelarSenasVencidas() {
        int canceladas = reservaRepo.cancelarPendientesVencidas(LocalDateTime.now(clock));
        if (canceladas > 0) {
            log.info("Cleanup de señas: {} reservas PENDIENTE vencidas pasadas a CANCELADO", canceladas);
        }
    }
}
