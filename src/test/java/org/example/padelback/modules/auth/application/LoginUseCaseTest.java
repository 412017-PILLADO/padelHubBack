package org.example.padelback.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.example.padelback.modules.auth.domain.exception.CredencialesInvalidasException;
import org.example.padelback.modules.auth.domain.model.UsuarioAuth;
import org.example.padelback.modules.auth.domain.model.UsuarioRol;
import org.example.padelback.modules.auth.infrastructure.security.CodigoIngresoStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * El login del apex ya no sabe de qué club es quien entra: lo deduce del email. Lo que se prueba
 * acá es esa deducción y, sobre todo, que TODOS los modos de falla salgan por la misma puerta.
 * El apex es un buscador global de emails: si "no existe" respondiera distinto de "contraseña
 * incorrecta", cualquiera podría averiguar qué mails son clientes de Padel-HUB.
 *
 * Fakes escritos a mano (los dos puertos tienen un solo método, así que entran como lambdas), en
 * vez de mocks: el proyecto no usa Mockito.
 */
class LoginUseCaseTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final Map<String, UsuarioAuth> usuarios = new HashMap<>();
    private final Map<Long, String> clubesActivos = new HashMap<>();
    private final CodigoIngresoStore codigos = new CodigoIngresoStore();

    private final LoginUseCase useCase = new LoginUseCase(
            email -> Optional.ofNullable(usuarios.get(email)),
            ENCODER,
            tenantId -> Optional.ofNullable(clubesActivos.get(tenantId)),
            codigos);

    @BeforeEach
    void sembrar() {
        usuarios.put("owner@club.com",
                new UsuarioAuth(10L, 3L, "owner@club.com", ENCODER.encode("padel123"), UsuarioRol.OWNER));
        clubesActivos.put(3L, "riopadel");
    }

    @Test
    void credencialesCorrectas_danElSlugDelClubYUnCodigoCanjeable() {
        var result = useCase.ejecutar("owner@club.com", "padel123");

        assertThat(result.slug()).isEqualTo("riopadel");
        assertThat(codigos.consumir(result.code()))
                .contains(new CodigoIngresoStore.Entrada(3L, "owner@club.com", UsuarioRol.OWNER));
    }

    @Test
    void emailInexistente_credencialesInvalidas() {
        assertThatThrownBy(() -> useCase.ejecutar("nadie@club.com", "padel123"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void passwordIncorrecta_credencialesInvalidas() {
        assertThatThrownBy(() -> useCase.ejecutar("owner@club.com", "otra-cosa"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void clubInactivo_credencialesInvalidas() {
        // Suspendido o dado de baja: mismo mensaje, no se le filtra al cliente el estado del club.
        clubesActivos.remove(3L);

        assertThatThrownBy(() -> useCase.ejecutar("owner@club.com", "padel123"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void emailConEspaciosYMayusculas_entraIgual() {
        // Lo que el dueño escribe en el formulario no tiene por qué coincidir carácter a carácter
        // con lo que quedó guardado el día del alta.
        var result = useCase.ejecutar("  OWNER@Club.com  ", "padel123");

        assertThat(result.slug()).isEqualTo("riopadel");
    }

    @Test
    void emailNulo_credencialesInvalidas() {
        assertThatThrownBy(() -> useCase.ejecutar(null, "padel123"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void emailInexistente_igualPagaElCostoDeUnBcrypt() {
        // Sin el hash de descarte, "no existe" no llega a invocar el encoder y responde más rápido
        // que "contraseña incorrecta" — esa diferencia de latencia es el mismo oráculo que el cuerpo
        // genérico de la respuesta existe para tapar, sólo que por otro canal.
        var encoderContado = new EncoderContado(ENCODER);
        var useCaseConContador = new LoginUseCase(
                email -> Optional.ofNullable(usuarios.get(email)),
                encoderContado,
                tenantId -> Optional.ofNullable(clubesActivos.get(tenantId)),
                codigos);

        assertThatThrownBy(() -> useCaseConContador.ejecutar("nadie@club.com", "cualquiera"))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(encoderContado.invocaciones).isEqualTo(1);
    }

    /** Decorador escrito a mano (nada de Mockito) que cuenta cuántas veces se invoca {@code matches}. */
    private static final class EncoderContado implements PasswordEncoder {
        private final PasswordEncoder delegado;
        private int invocaciones = 0;

        EncoderContado(PasswordEncoder delegado) {
            this.delegado = delegado;
        }

        @Override
        public String encode(CharSequence rawPassword) {
            return delegado.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            invocaciones++;
            return delegado.matches(rawPassword, encodedPassword);
        }
    }
}
