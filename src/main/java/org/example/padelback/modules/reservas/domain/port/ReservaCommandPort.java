package org.example.padelback.modules.reservas.domain.port;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.reserva.NuevaReserva;
import org.example.padelback.modules.reservas.domain.model.reserva.ReservaCreada;

public interface ReservaCommandPort {

    boolean tenantRequiereTelefono();

    /** Reservas CONFIRMADAS de la cancha ese día (para auto-asignar la cancha menos cargada). */
    int reservasConfirmadasEseDia(Long canchaId, LocalDate fecha);

    /** Teléfonos (clienteWhatsapp) de reservas CONFIRMADO activas con inicio > ahora, del tenant. */
    List<String> telefonosDeReservasActivasFuturas(LocalDateTime ahora);

    /** Cantidad de reservas creadas desde esa IP (cualquier estado) desde el instante dado. */
    int reservasDesdeIpDesde(String ip, Instant desde);

    /**
     * Crea la reserva de forma atómica: lock pesimista sobre la cancha, re-verifica que no haya
     * solapamiento con otra reserva CONFIRMADA y persiste. Lanza SlotNoDisponibleException si la
     * cancha fue tomada en la carrera.
     */
    ReservaCreada crearSiLibre(NuevaReserva nueva);
}
