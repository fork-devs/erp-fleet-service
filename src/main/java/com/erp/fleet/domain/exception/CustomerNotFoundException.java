package com.erp.fleet.domain.exception;

public class CustomerNotFoundException extends DomainException {
    public CustomerNotFoundException(String id) {
        super("FLEET_CUSTOMER_NOT_FOUND", "Cliente no encontrado con ID: " + id);
    }
}
