package org.example.padelback.modules.reservas.application;

import java.security.SecureRandom;
import java.time.LocalDate;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.exception.SolicitudInvalidaException;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ArrepentimientoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ArrepentimientoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta pública del arrepentimiento (Res. 424/2020): sin registro previo, devuelve el código
 * de revocación al instante (misma vía). El dueño lo gestiona desde el panel.
 */
@Service
@RequiredArgsConstructor
public class RegistrarArrepentimientoUseCase {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final ArrepentimientoJpaRepository repo;
    private final TenantProvider tenantProvider;

    @Transactional
    public String ejecutar(String nombre, String whatsapp, String detalle, LocalDate reservaFecha,
                           String honeypot) {
        if (honeypot != null && !honeypot.isBlank()) {
            throw new SolicitudInvalidaException("Solicitud inválida");
        }
        tenantProvider.requireTenantId(); // 404 vía handler si el tenant no resolvió
        ArrepentimientoJpaEntity a = new ArrepentimientoJpaEntity();
        a.setCodigo(generarCodigo());
        a.setNombre(nombre);
        a.setWhatsapp(whatsapp);
        a.setDetalle(detalle);
        a.setReservaFecha(reservaFecha);
        a.setGestionado(false);
        repo.save(a);
        return a.getCodigo();
    }

    private String generarCodigo() {
        StringBuilder sb = new StringBuilder("ARR-");
        for (int i = 0; i < 6; i++) {
            sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
