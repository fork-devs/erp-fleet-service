package com.erp.fleet.domain.exception;

/**
 * Error de dominio, con un codigo estable por familia, legible por maquina.
 *
 * <p>El codigo es lo que el cliente consume: el mensaje se puede reescribir,
 * el codigo no cambia sin que sea un cambio incompatible.
 */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String message) {
        this("FLEET_ERROR", message);
    }

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
