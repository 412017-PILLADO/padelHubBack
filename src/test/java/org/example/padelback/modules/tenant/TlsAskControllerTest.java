package org.example.padelback.modules.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.example.padelback.modules.tenant.domain.model.TenantStatus;
import org.example.padelback.modules.tenant.infrastructure.HostTenantResolver;
import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantJpaEntity;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import org.example.padelback.modules.tenant.presentation.TlsAskController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * El portero de la emisión de certificados. Caddy le pregunta a este endpoint antes de pedirle uno a
 * Let's Encrypt, así que un "sí" de más no es un bug cosmético: cualquiera que apunte su dominio al
 * IP del servidor consumiría la cuota semanal de emisiones y dejaría a los clubes reales sin poder
 * renovar el suyo.
 */
class TlsAskControllerTest {

    private TenantJpaRepository tenantRepo;
    private HostTenantResolver hostResolver;
    private TlsAskController controller;

    @BeforeEach
    void setUp() {
        tenantRepo = mock(TenantJpaRepository.class);
        hostResolver = mock(HostTenantResolver.class);
        controller = new TlsAskController(tenantRepo, hostResolver);
        ReflectionTestUtils.setField(controller, "baseDomain", "padel-hub.com.ar");
        // Por defecto nada resuelve: cada test habilita lo suyo.
        lenient().when(hostResolver.resolve(any())).thenReturn(Optional.empty());
        lenient().when(tenantRepo.findBySlugAndStatus(any(), any())).thenReturn(Optional.empty());
    }

    private boolean autoriza(String domain) {
        return controller.existe(domain).getStatusCode().is2xxSuccessful();
    }

    private void clubActivo(String slug) {
        when(tenantRepo.findBySlugAndStatus(slug, TenantStatus.ACTIVE))
                .thenReturn(Optional.of(new TenantJpaEntity()));
    }

    @Test
    void autorizaElApexYSuWww() {
        assertThat(autoriza("padel-hub.com.ar")).isTrue();
        assertThat(autoriza("www.padel-hub.com.ar")).isTrue();
    }

    @Test
    void autorizaElSubdominioDeUnClubActivo() {
        clubActivo("laplata");
        assertThat(autoriza("laplata.padel-hub.com.ar")).isTrue();
    }

    @Test
    void rechazaUnClubQueNoExiste() {
        assertThat(autoriza("inventado.padel-hub.com.ar")).isFalse();
    }

    @Test
    void rechazaUnDominioAjeno() {
        // El caso que motiva todo: alguien apunta su dominio al IP del servidor.
        clubActivo("laplata");
        assertThat(autoriza("sitio-de-otro.com")).isFalse();
        assertThat(autoriza("laplata.padel-hub.com.ar.attacker.net")).isFalse();
    }

    @Test
    void rechazaSubdominiosAnidados() {
        // `a.b.padel-hub.com.ar` no es el club "a.b": sólo se acepta un nivel.
        clubActivo("a.b");
        assertThat(autoriza("a.b.padel-hub.com.ar")).isFalse();
    }

    @Test
    void autorizaUnDominioPropioCargadoPorElClub() {
        when(hostResolver.resolve("canchas-del-parque.com.ar")).thenReturn(Optional.of(7L));
        assertThat(autoriza("canchas-del-parque.com.ar")).isTrue();
    }

    @Test
    void rechazaVacioONulo() {
        assertThat(autoriza("")).isFalse();
        assertThat(autoriza(null)).isFalse();
    }
}
