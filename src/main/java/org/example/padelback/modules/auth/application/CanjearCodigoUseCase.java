package org.example.padelback.modules.auth.application;

import org.example.padelback.modules.auth.domain.exception.CredencialesInvalidasException;
import org.example.padelback.modules.auth.domain.port.TokenIssuerPort;
import org.example.padelback.modules.auth.infrastructure.security.CodigoIngresoStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Segundo paso del login: el subdominio del club cambia el código que trajo en la URL por el JWT.
 *
 * <p>Todos los modos de falla —código inexistente, vencido, ya usado, o de otro club— salen por la
 * misma {@link CredencialesInvalidasException}. Distinguirlos le diría a quien prueba códigos
 * cuál de sus intentos estuvo cerca.
 */
@Service
@RequiredArgsConstructor
public class CanjearCodigoUseCase {

    private final CodigoIngresoStore codigos;
    private final TokenIssuerPort tokenIssuer;

    public record CanjeResult(String token, long expiresIn) {}

    public CanjeResult ejecutar(String code, Long tenantIdDelHost) {
        CodigoIngresoStore.Entrada entrada = codigos.consumir(code)
                .orElseThrow(CredencialesInvalidasException::new);
        // El código de un club no vale parado en el host de otro. `consumir` ya lo quemó, así que
        // un código que cayó en el host equivocado no se puede reintentar en el correcto.
        if (!entrada.tenantId().equals(tenantIdDelHost)) {
            throw new CredencialesInvalidasException();
        }
        return new CanjeResult(
                tokenIssuer.emitir(entrada.tenantId(), entrada.email(), entrada.rol()),
                tokenIssuer.expiracionMs());
    }
}
