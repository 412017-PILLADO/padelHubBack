package org.example.padelback.modules.auth.domain.port;

/**
 * Consulta mínima del estado del tenant que necesita el login: si está INACTIVE, ningún owner puede
 * autenticarse aunque sus credenciales sean válidas. Puerto chico para no atar el dominio de auth a
 * la persistencia del módulo tenant (la implementación vive en infrastructure).
 */
public interface TenantEstadoPort {

    boolean estaActivo(Long tenantId);
}
