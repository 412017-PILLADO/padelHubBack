package org.example.padelback.infrastructure.tenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Activa el {@code tenantFilter} de Hibernate (definido en {@code BaseJpaEntity}) al entrar a
 * cualquier método transaccional de la app, usando el tenant del {@link TenantContext}.
 *
 * <p>Es la red de seguridad de aislamiento por tenant del §9: además de los {@code ...AndTenantId}
 * explícitos, toda lectura dentro de una transacción queda filtrada por {@code tenant_id} aunque la
 * query se haya olvidado del filtro.
 *
 * <p>Corre <b>dentro</b> de la transacción: {@code @EnableTransactionManagement(order = 0)} en
 * {@code JpaConfig} deja el advice de {@code @Transactional} como el más externo, así cuando este
 * aspecto (precedencia default = más interno) se ejecuta, la {@link Session} ya está abierta
 * (con {@code open-in-view: false} la sesión vive solo dentro de la tx).
 *
 * <p>Si no hay tenant en contexto (ej. resolución pública por host, login antes de setear el
 * contexto), no se activa nada — esas rutas usan resolución/filtros explícitos.
 */
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("(@within(org.springframework.transaction.annotation.Transactional)"
            + " || @annotation(org.springframework.transaction.annotation.Transactional))"
            + " && within(org.example.padelback..*)")
    public void enableTenantFilter() {
        TenantContext.getTenantId().ifPresent(tenantId -> {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("tenantFilter") == null) {
                session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
            }
        });
    }
}
