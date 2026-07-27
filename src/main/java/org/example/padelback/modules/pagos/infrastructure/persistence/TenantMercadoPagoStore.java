package org.example.padelback.modules.pagos.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.pagos.domain.model.CredencialMp;
import org.example.padelback.modules.pagos.domain.port.CredencialMpStorePort;
import org.example.padelback.modules.pagos.infrastructure.crypto.TokenCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Credenciales OAuth de MP por tenant en {@code tenant_mercadopago}. JdbcTemplate sin entidad JPA
 * (patrón TenantLogoStore): tabla fuera del filtro de tenant y de ddl-auto=validate. Tokens
 * cifrados en reposo con {@link TokenCipher}.
 */
@Component
@RequiredArgsConstructor
public class TenantMercadoPagoStore implements CredencialMpStorePort {

    private final JdbcTemplate jdbc;
    private final TokenCipher cipher;

    // Método en vez de campo RowMapper: un RowMapper inicializado como campo (lambda que captura
    // `cipher`) rompe la compilación con "variable cipher might not have been initialized", porque
    // Lombok hoistea los inicializadores de campo ANTES de las asignaciones del constructor generado
    // (this.cipher = cipher), y el análisis de definite-assignment de un blank final lo detecta.
    private CredencialMp mapRow(ResultSet rs, int i) throws SQLException {
        return new CredencialMp(
                rs.getLong("tenant_id"),
                rs.getString("mp_user_id"),
                cipher.decrypt(rs.getString("access_token_cif")),
                rs.getString("refresh_token_cif") != null ? cipher.decrypt(rs.getString("refresh_token_cif")) : null,
                rs.getTimestamp("expira_en").toInstant());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CredencialMp> cargar(long tenantId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT tenant_id, mp_user_id, access_token_cif, refresh_token_cif, expira_en "
                            + "FROM tenant_mercadopago WHERE tenant_id = ?", this::mapRow, tenantId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void guardar(CredencialMp c, String mpPublicKey, String scope) {
        jdbc.update(
                "INSERT INTO tenant_mercadopago (tenant_id, mp_user_id, mp_public_key, access_token_cif, "
                        + "refresh_token_cif, scope, expira_en, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE mp_user_id = VALUES(mp_user_id), "
                        + "mp_public_key = VALUES(mp_public_key), access_token_cif = VALUES(access_token_cif), "
                        + "refresh_token_cif = VALUES(refresh_token_cif), scope = VALUES(scope), "
                        + "expira_en = VALUES(expira_en), updated_at = VALUES(updated_at)",
                c.tenantId(), c.mpUserId(), mpPublicKey, cipher.encrypt(c.accessToken()),
                c.refreshToken() != null ? cipher.encrypt(c.refreshToken()) : null, scope,
                Timestamp.from(c.expiraEn()), Timestamp.from(Instant.now()));
    }

    @Override
    @Transactional
    public void eliminar(long tenantId) {
        jdbc.update("DELETE FROM tenant_mercadopago WHERE tenant_id = ?", tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CredencialMp> conVencimientoAntesDe(Instant limite) {
        return jdbc.query(
                "SELECT tenant_id, mp_user_id, access_token_cif, refresh_token_cif, expira_en "
                        + "FROM tenant_mercadopago WHERE expira_en < ?", this::mapRow, Timestamp.from(limite));
    }
}
