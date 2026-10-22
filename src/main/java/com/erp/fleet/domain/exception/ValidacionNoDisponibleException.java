package com.erp.fleet.domain.exception;

/**
 * Se lanza cuando la validacion no puede realizarse porque el hub no esta
 * disponible.
 *
 * <p>No es un resultado de la validacion: el registro queda pendiente y el
 * intento se reintenta. La diferencia importa: una discrepancia que informa
 * el hub hay que mostrarla al taller, y un hub caido hay que reintentarlo.
 */
public class ValidacionNoDisponibleException extends DomainException {

    public ValidacionNoDisponibleException(String message) {
        super("FLEET_VALIDATION_UNAVAILABLE", message);
    }

    public ValidacionNoDisponibleException(String message, Throwable causa) {
        super("FLEET_VALIDATION_UNAVAILABLE", message);
        initCause(causa);
    }
}
