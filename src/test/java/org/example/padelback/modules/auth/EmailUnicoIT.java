package org.example.padelback.modules.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * El login resuelve el club por email, así que un email en dos clubes no autenticaría a nadie:
 * `UsuarioRepositoryAdapter.unico` lo colapsa a vacío y el dueño recibe un 401 que no entiende. La
 * garantía tiene que estar en la base, no en la convención — si vive sólo en el código, el primer
 * alta de tenant hecha por SQL a mano la rompe en silencio.
 */
class EmailUnicoIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void elMismoEmailNoPuedeExistirEnDosClubes() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO usuarios (tenant_id, email, password_hash, rol, estado, active, "
                        + "created_at, updated_at, created_by, updated_by, version) "
                        + "VALUES (999, ?, 'x', 'OWNER', 'ACTIVO', TRUE, NOW(6), NOW(6), 'test', 'test', 0)",
                OWNER_EMAIL))
                .isInstanceOf(DataAccessException.class);
    }
}
