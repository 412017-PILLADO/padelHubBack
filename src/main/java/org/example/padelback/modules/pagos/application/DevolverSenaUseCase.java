package org.example.padelback.modules.pagos.application;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.pagos.domain.exception.MpNoConectadoException;
import org.example.padelback.modules.pagos.domain.exception.SenaNoDevolvibleException;
import org.example.padelback.modules.pagos.domain.model.CredencialMp;
import org.example.padelback.modules.pagos.domain.model.SenaPago;
import org.example.padelback.modules.pagos.domain.port.CredencialMpStorePort;
import org.example.padelback.modules.pagos.domain.port.MercadoPagoGatewayPort;
import org.example.padelback.modules.pagos.domain.port.SenaPagoStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Devuelve la seña cobrada por MP (reembolso total en la cuenta del club). NO cancela la
 * reserva: si corresponde, el dueño la cancela aparte con el botón de siempre. Cubre el
 * arrepentimiento del cliente y los pagos APROBADO_TARDE.
 */
@Service
@RequiredArgsConstructor
public class DevolverSenaUseCase {

    private final SenaPagoStorePort senaPagos;
    private final CredencialMpStorePort credenciales;
    private final MercadoPagoGatewayPort gateway;
    private final TenantProvider tenantProvider;

    public void ejecutar(long reservaId) {
        long tenantId = tenantProvider.requireTenantId();
        SenaPago sp = senaPagos.cargarPorReserva(tenantId, reservaId)
                .orElseThrow(() -> new SenaNoDevolvibleException("La reserva no tiene pago de seña."));
        if (!SenaPago.APROBADO.equals(sp.estado()) && !SenaPago.APROBADO_TARDE.equals(sp.estado())) {
            throw new SenaNoDevolvibleException("La seña no está en estado devolvible (" + sp.estado() + ").");
        }
        CredencialMp cred = credenciales.cargar(tenantId).orElseThrow(MpNoConectadoException::new);
        // Carrera intencionalmente tolerada: dos llamadas concurrentes a devolver() pasan ambas el
        // chequeo de arriba antes de que cualquiera reembolse. No hay problema porque MP dedupea
        // por X-Idempotency-Key ("refund-" + paymentId, ver MercadoPagoClient): misma key en las
        // dos → un solo reembolso real. Peor caso: ambos callers reciben 204 y registrarPago corre
        // dos veces escribiendo el mismo estado terminal DEVUELTO — no hay doble cobro ni estado
        // inconsistente.
        gateway.reembolsarPago(cred.accessToken(), sp.paymentId());
        senaPagos.registrarPago(tenantId, reservaId, sp.paymentId(), SenaPago.DEVUELTO);
    }
}
