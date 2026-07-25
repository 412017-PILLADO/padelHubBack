package org.example.padelback.modules.pagos.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.example.padelback.modules.pagos.domain.model.SenaPago;
import org.example.padelback.modules.pagos.domain.port.SenaPagoStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pagos de seña por reserva en {@code sena_pagos} (JdbcTemplate, sin entidad). Lee reservas y
 * complejos SOLO para armar el snapshot del link (read-only, tenant_id explícito en el WHERE).
 */
@Component
@RequiredArgsConstructor
public class SenaPagoStore implements SenaPagoStorePort {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional(readOnly = true)
    public Optional<DatosLinkSena> datosParaLink(long tenantId, long reservaId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT r.estado, r.expira_en, c.sena_monto, c.requiere_sena, c.nombre "
                            + "FROM reservas r JOIN complejos c ON c.id = r.complejo_id "
                            + "WHERE r.tenant_id = ? AND r.id = ? AND r.active = 1",
                    (rs, i) -> new DatosLinkSena(
                            rs.getString("estado"),
                            rs.getTimestamp("expira_en") != null ? rs.getTimestamp("expira_en").toLocalDateTime() : null,
                            rs.getBigDecimal("sena_monto"),
                            rs.getBoolean("requiere_sena"),
                            rs.getString("nombre")),
                    tenantId, reservaId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SenaPago> cargarPorReserva(long tenantId, long reservaId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT reserva_id, preference_id, init_point, payment_id, estado, monto "
                            + "FROM sena_pagos WHERE tenant_id = ? AND reserva_id = ?",
                    (rs, i) -> new SenaPago(rs.getLong("reserva_id"), rs.getString("preference_id"),
                            rs.getString("init_point"), rs.getString("payment_id"),
                            rs.getString("estado"), rs.getBigDecimal("monto")),
                    tenantId, reservaId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void guardarPreferencia(long tenantId, long reservaId, String preferenceId,
                                   String initPoint, BigDecimal monto) {
        jdbc.update(
                "INSERT INTO sena_pagos (reserva_id, tenant_id, preference_id, init_point, estado, monto, updated_at) "
                        + "VALUES (?, ?, ?, ?, 'PENDIENTE', ?, ?) "
                        + "ON DUPLICATE KEY UPDATE preference_id = VALUES(preference_id), "
                        + "init_point = VALUES(init_point), updated_at = VALUES(updated_at)",
                reservaId, tenantId, preferenceId, initPoint, monto, Timestamp.from(Instant.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean pagoYaProcesado(String paymentId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sena_pagos WHERE payment_id = ?", Integer.class, paymentId);
        return n != null && n > 0;
    }

    @Override
    @Transactional
    public void registrarPago(long tenantId, long reservaId, String paymentId, String estado) {
        jdbc.update(
                "UPDATE sena_pagos SET payment_id = ?, estado = ?, updated_at = ? "
                        + "WHERE tenant_id = ? AND reserva_id = ?",
                paymentId, estado, Timestamp.from(Instant.now()), tenantId, reservaId);
    }
}
