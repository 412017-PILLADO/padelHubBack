package org.example.padelback.modules.auth.domain.port;

import java.util.Optional;

/**
 * Lo que el login necesita saber del club, en una sola pregunta: su slug si está ACTIVE, o nada.
 * Un club INACTIVE no deja entrar a sus owners aunque las credenciales sean válidas, y el slug es
 * lo que el front usa para redirigir al subdominio correcto.
 */
public interface TenantEstadoPort {

    Optional<String> slugSiActivo(Long tenantId);
}
