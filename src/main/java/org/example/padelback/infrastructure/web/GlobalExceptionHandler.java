package org.example.padelback.infrastructure.web;

import java.util.Map;

import org.example.padelback.domain.exception.TenantNotResolvedException;
import org.example.padelback.modules.auth.domain.exception.CredencialesInvalidasException;
import org.example.padelback.modules.reservas.domain.exception.BloqueoNoEncontradoException;
import org.example.padelback.modules.reservas.domain.exception.CanchaInvalidaException;
import org.example.padelback.modules.reservas.domain.exception.CanchaNoEncontradaException;
import org.example.padelback.modules.reservas.domain.exception.ComplejoNoResueltoException;
import org.example.padelback.modules.reservas.domain.exception.DuracionInvalidaException;
import org.example.padelback.modules.reservas.domain.exception.LimiteReservasPorIpException;
import org.example.padelback.modules.reservas.domain.exception.LimiteTurnosPorTelefonoException;
import org.example.padelback.modules.reservas.domain.exception.SenaNoValidableException;
import org.example.padelback.modules.reservas.domain.exception.SlotNoDisponibleException;
import org.example.padelback.modules.reservas.domain.exception.SolicitudInvalidaException;
import org.example.padelback.modules.reservas.domain.exception.TelefonoRequeridoException;
import org.example.padelback.modules.reservas.domain.exception.TurnoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones de dominio a respuestas HTTP {@code { "error": "..." }}.
 * Las excepciones del módulo de reservas (slot no disponible, límites de abuso, etc.) se
 * agregan acá a medida que se construye ese módulo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TenantNotResolvedException.class)
    public ResponseEntity<Map<String, String>> handleTenant(TenantNotResolvedException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "tenant no resuelto"));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, String>> handleCredenciales(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ComplejoNoResueltoException.class)
    public ResponseEntity<Map<String, String>> handleComplejo(ComplejoNoResueltoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TurnoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleTurno(TurnoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BloqueoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleBloqueo(BloqueoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CanchaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleCancha(CanchaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CanchaInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleCanchaInvalida(CanchaInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DuracionInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleDuracion(DuracionInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SlotNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> handleSlot(SlotNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SenaNoValidableException.class)
    public ResponseEntity<Map<String, String>> handleSena(SenaNoValidableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TelefonoRequeridoException.class)
    public ResponseEntity<Map<String, String>> handleTelefono(TelefonoRequeridoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleSolicitud(SolicitudInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Solicitud inválida"));
    }

    @ExceptionHandler(LimiteTurnosPorTelefonoException.class)
    public ResponseEntity<Map<String, String>> handleLimiteTel(LimiteTurnosPorTelefonoException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(LimiteReservasPorIpException.class)
    public ResponseEntity<Map<String, String>> handleLimiteIp(LimiteReservasPorIpException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", ex.getMessage()));
    }
}
