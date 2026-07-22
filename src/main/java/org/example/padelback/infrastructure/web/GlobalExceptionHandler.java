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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones de dominio a respuestas HTTP {@code { "error": "..." }}.
 * Las excepciones del módulo de reservas (slot no disponible, límites de abuso, etc.) se
 * agregan acá a medida que se construye ese módulo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    /**
     * A4: catch-all para validaciones "de andar por casa" hechas con {@code throw new
     * IllegalArgumentException(...)} en vez de una excepción de dominio dedicada (ej. la franja
     * from &gt;= to de {@code guardarHorarios}). Como ninguna excepción de dominio extiende
     * {@link IllegalArgumentException}, este handler nunca le saca el caso a uno más específico.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    /** A4: @Valid en @RequestBody que no pasa las anotaciones bean-validation del DTO. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleNotValid(MethodArgumentNotValidException ex) {
        FieldError primero = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String mensaje = primero != null ? primero.getField() + ": " + primero.getDefaultMessage()
                : "Solicitud inválida";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", mensaje));
    }

    /** A4: parámetros validados fuera del body (ej. @Validated en query params) que no cumplen la constraint. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        ConstraintViolation<?> primera = ex.getConstraintViolations().stream().findFirst().orElse(null);
        String mensaje = primera != null ? primera.getPropertyPath() + ": " + primera.getMessage()
                : "Solicitud inválida";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", mensaje));
    }

    /** A4: violaciones de constraint de la base (unique, FK, etc.) — nunca se expone el detalle interno. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Conflicto de integridad de datos", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Conflicto de datos"));
    }

    /** A4: red de seguridad final — cualquier excepción no mapeada no debe filtrar detalles al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenerica(Exception ex) {
        log.error("Error interno no manejado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error interno"));
    }
}
