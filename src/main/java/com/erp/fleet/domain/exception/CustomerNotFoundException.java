package com.erp.fleet.domain.exception;

public class CustomerNotFoundException extends DomainException {
    public CustomerNotFoundException(String id) {
        super("Cliente no encontrado con ID: " + id);
    }
}
