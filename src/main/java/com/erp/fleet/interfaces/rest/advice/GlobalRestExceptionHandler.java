package com.erp.fleet.interfaces.rest.advice;

import com.erp.fleet.domain.exception.DomainException;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.UUID;

/**
 * Manejo global de los errores de la API.
 *
 * <p>El cuerpo del error es un {@code ProblemDetail} (RFC 9457) con un
 * <strong>codigo estable por familia</strong>: el mensaje se puede reescribir,
 * el codigo no cambia sin que sea un cambio incompatible. El error lleva
 * tambien el <strong>taller</strong>, para que el frontend pueda mostrarlo
 * sin adivinar.
 *
 * <p>El error interno no expone ni la clase de la excepcion ni su mensaje: el
 * diagnostico va al registro, que es del operador.
 */
@Hidden
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);

    private static final String TIPO_BASE = "https://erp-automotive.pe/errors/";

    /** La peticion no tiene contexto de taller. */
    @ExceptionHandler(com.erp.fleet.domain.exception.TenantContextMissingException.class)
    public ProblemDetail handleTenantContextMissing(
            com.erp.fleet.domain.exception.TenantContextMissingException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** El documento ya existe en el taller. */
    @ExceptionHandler(com.erp.fleet.domain.exception.DocAlreadyExistsException.class)
    public ProblemDetail handleDocAlreadyExists(
            com.erp.fleet.domain.exception.DocAlreadyExistsException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** La placa ya existe en el taller. */
    @ExceptionHandler(com.erp.fleet.domain.exception.PlateAlreadyExistsException.class)
    public ProblemDetail handlePlateAlreadyExists(
            com.erp.fleet.domain.exception.PlateAlreadyExistsException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** El cliente no existe en el taller. */
    @ExceptionHandler(com.erp.fleet.domain.exception.CustomerNotFoundException.class)
    public ProblemDetail handleCustomerNotFound(
            com.erp.fleet.domain.exception.CustomerNotFoundException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** El vehiculo no existe en el taller. */
    @ExceptionHandler(com.erp.fleet.domain.exception.VehicleNotFoundException.class)
    public ProblemDetail handleVehicleNotFound(
            com.erp.fleet.domain.exception.VehicleNotFoundException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** El cliente tiene vehiculos y no se puede dar de baja. */
    @ExceptionHandler(com.erp.fleet.domain.exception.CustomerWithVehiclesException.class)
    public ProblemDetail handleCustomerWithVehicles(
            com.erp.fleet.domain.exception.CustomerWithVehiclesException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Cualquier otro error de dominio. */
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex, WebRequest request) {
        return problema(ex.getCode(), HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * La denegacion por rol es una decision de autorizacion, no un fallo de la
     * operacion: se responde 403 y no 500. Sin este manejador, el generico
     * la convertiria en un error del servidor, que dice al cliente que
     * reintente lo mismo.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return problema("FLEET_ACCESS_DENIED", HttpStatus.FORBIDDEN,
                "El principal no tiene el rol que exige el endpoint", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, WebRequest request) {
        return problema("FLEET_UNAUTHENTICATED", HttpStatus.UNAUTHORIZED,
                "La peticion requiere un token de acceso verificado", request);
    }

    /**
     * Una ruta que no existe es un 404, no un error del servidor: el cliente
     * ha pedido algo que no esta, y con un 500 pensaria que el servicio esta
     * roto y reintentaria.
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException ex,
                                          WebRequest request) {
        return problema("FLEET_NOT_FOUND", HttpStatus.NOT_FOUND, "El recurso solicitado no existe", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Peticion no valida");
        return problema("FLEET_VALIDATION_ERROR", HttpStatus.BAD_REQUEST, mensaje, request);
    }

    /**
     * Cualquier otra excepcion: se REGISTRA con su traza y se responde con
     * un error generico. La traza va al registro, que es del operador; el
     * cliente no recibe ni la clase de la excepcion ni su mensaje.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex, WebRequest request) {
        log.error("Error no previsto en {} {}", request.getDescription(false), ex.getMessage(), ex);
        return problema("FLEET_INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
                "Se ha producido un error interno", request);
    }

    private ProblemDetail problema(String codigo, HttpStatus estado, String detalle, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(estado, detalle);
        problem.setTitle(estado.getReasonPhrase());
        problem.setType(URI.create(TIPO_BASE + codigo.toLowerCase().replace('_', '-').replace("fleet-", "")));
        problem.setProperty("code", codigo);

        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            problem.setProperty("tenant_id", tenantId.toString());
        }
        return problem;
    }
}
