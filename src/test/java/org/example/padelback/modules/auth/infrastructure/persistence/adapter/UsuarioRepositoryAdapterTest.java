package org.example.padelback.modules.auth.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.example.padelback.modules.auth.infrastructure.persistence.entity.UsuarioJpaEntity;
import org.junit.jupiter.api.Test;

/**
 * La búsqueda por email es global y el índice único global recién llega en la última tarea del
 * plan (hay que resolver los duplicados de producción antes de poder crearlo). Hasta entonces la
 * consulta PUEDE devolver más de una fila, y con un `Optional` eso sería una
 * `NonUniqueResultException` — un 500 en la cara del usuario en vez del 401 genérico de siempre.
 * Por eso el adapter colapsa una `List`, y por eso esa regla es una función pura con test propio.
 */
class UsuarioRepositoryAdapterTest {

    private static UsuarioJpaEntity usuario(String email) {
        return UsuarioJpaEntity.builder().email(email).build();
    }

    @Test
    void unSoloUsuario_esElQueBusca() {
        var encontrado = UsuarioRepositoryAdapter.unico(List.of(usuario("owner@club.com")));

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEmail()).isEqualTo("owner@club.com");
    }

    @Test
    void ningunUsuario_daVacio() {
        assertThat(UsuarioRepositoryAdapter.unico(List.of())).isEmpty();
    }

    @Test
    void emailRepetidoEnDosClubes_daVacio() {
        var encontrado = UsuarioRepositoryAdapter.unico(
                List.of(usuario("owner@club.com"), usuario("owner@club.com")));

        assertThat(encontrado).isEmpty();
    }
}
