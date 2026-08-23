package org.example.padelback.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.example.padelback.modules.auth.domain.exception.CredencialesInvalidasException;
import org.example.padelback.modules.auth.domain.model.UsuarioRol;
import org.example.padelback.modules.auth.domain.port.TokenIssuerPort;
import org.example.padelback.modules.auth.infrastructure.security.CodigoIngresoStore;
import org.junit.jupiter.api.Test;

/**
 * El canje es donde el código se convierte en sesión, y donde se verifica lo único que el código
 * por sí solo no garantiza: que el club del código sea el club del host donde se está canjeando.
 * Sin esa comprobación, un código emitido para el club A podría canjearse parado en el host del
 * club B y el JWT quedaría guardado en un origen que no le corresponde.
 */
class CanjearCodigoUseCaseTest {

    /** Emisor de mentira: el JWT real se prueba en los IT; acá sólo interesa QUÉ se firma. */
    private static final TokenIssuerPort ISSUER = new TokenIssuerPort() {
        @Override
        public String emitir(Long tenantId, String email, UsuarioRol rol) {
            return "jwt|" + tenantId + "|" + email + "|" + rol;
        }

        @Override
        public long expiracionMs() {
            return 3_600_000L;
        }
    };

    private final CodigoIngresoStore store = new CodigoIngresoStore();
    private final CanjearCodigoUseCase useCase = new CanjearCodigoUseCase(store, ISSUER);

    @Test
    void codigoDelMismoTenant_seCanjeaPorUnToken() {
        String codigo = store.emitir(1L, "owner@padelhub.com", UsuarioRol.OWNER);

        var result = useCase.ejecutar(codigo, 1L);

        assertThat(result.token()).isEqualTo("jwt|1|owner@padelhub.com|OWNER");
        assertThat(result.expiresIn()).isEqualTo(3_600_000L);
    }

    @Test
    void codigoDeOtroTenant_noSeCanjea() {
        String codigo = store.emitir(1L, "owner@padelhub.com", UsuarioRol.OWNER);

        assertThatThrownBy(() -> useCase.ejecutar(codigo, 2L))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void codigoDeOtroTenant_ademasQuedaQuemado() {
        // El intento fallido igual consumió el código: si sobreviviera, alguien podría probarlo
        // host por host hasta acertar el club.
        String codigo = store.emitir(1L, "owner@padelhub.com", UsuarioRol.OWNER);
        assertThatThrownBy(() -> useCase.ejecutar(codigo, 2L))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThatThrownBy(() -> useCase.ejecutar(codigo, 1L))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void codigoInexistente_noSeCanjea() {
        assertThatThrownBy(() -> useCase.ejecutar("no-existe", 1L))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void elMismoCodigoNoSirveDosVeces() {
        String codigo = store.emitir(1L, "owner@padelhub.com", UsuarioRol.OWNER);
        useCase.ejecutar(codigo, 1L);

        assertThatThrownBy(() -> useCase.ejecutar(codigo, 1L))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
