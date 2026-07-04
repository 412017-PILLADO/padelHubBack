package org.example.padelback.modules.reservas.domain.port;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.reserva.NuevaReserva;
import org.example.padelback.modules.reservas.domain.model.reserva.ReservaCreada;

public interface ReservaCommandPort {

    boolean tenantRequiereTelefono();

    /**
     * Reservas que ocupan la cancha ese día (CONFIRMADO + PENDIENTE vigente), para auto-asignar la
     * cancha menos cargada.
     */
    int reservasQueOcupanEseDia(Long canchaId, LocalDate fecha);

    /** Teléfonos (clienteWhatsapp) de reservas que ocupan (CONFIRMADO + PENDIENTE vigente) con inicio > ahora. */
    List<String> telefonosDeReservasActivasFuturas(LocalDateTime ahora);

    /** Cantidad de reservas creadas desde esa IP (cualquier estado) desde el instante dado. */
    int reservasDesdeIpDesde(String ip, Instant desde);

    /**
     * Crea la reserva de forma atómica: lock pesimista sobre la cancha, re-verifica que no haya
     * solapamiento con otra reserva que ocupe (CONFIRMADA o PENDIENTE vigente) y persiste con el
     * estado/expiración que trae {@link NuevaReserva}. Lanza SlotNoDisponibleException si la cancha
     * fue tomada en la carrera.
     */
    ReservaCreada crearSiLibre(NuevaReserva nueva);
}
