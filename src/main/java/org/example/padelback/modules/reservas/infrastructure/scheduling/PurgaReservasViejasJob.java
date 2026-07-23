package org.example.padelback.modules.reservas.infrastructure.scheduling;

import java.time.Clock;
import java.time.LocalDateTime;

import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ReservaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purga (hard delete) de reservas viejas: no hay histórico de turnos (decisión de producto). Borra
 * todo lo que terminó hace más de {@code padel.retencion-reservas-dias} días (default 365).
 *
 * <p>Corre una vez por día de madrugada. Sobre todos los tenants: es tarea de sistema, sin scope por
 * request (el DELETE nativo de {@code deleteByFinBefore} no pasa por el filtro Hibernate de tenant,
 * que solo se activa dentro de un {@code TenantContext}).
 */
@Component
@RequiredArgsConstructor
public class PurgaReservasViejasJob {

    private static final Logger log = LoggerFactory.getLogger(PurgaReservasViejasJob.class);

    private final ReservaJpaRepository reservaRepo;
    private final Clock clock;

    @Value("${padel.retencion-reservas-dias:365}")
    private long retencionDias;

    /** Diario 04:15. El JVM está pineado a UTC, así que {@code now()} es UTC. */
    @Scheduled(cron = "${padel.retencion-reservas.cron:0 15 4 * * *}", zone = "UTC")
    @Transactional
    public void purgar() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retencionDias);
        int borradas = reservaRepo.deleteByFinBefore(cutoff);
        if (borradas > 0) {
            log.info("Purga de reservas viejas: {} reservas con fin anterior a {} eliminadas", borradas, cutoff);
        }
    }
}
