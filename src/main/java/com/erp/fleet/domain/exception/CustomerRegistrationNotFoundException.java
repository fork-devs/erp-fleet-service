package com.erp.fleet.domain.exception;

public class CustomerRegistrationNotFoundException extends DomainException {
    public CustomerRegistrationNotFoundException(String id) {
        super("FLEET_CUSTOMER_REGISTRATION_NOT_FOUND", "Customer registration not found with ID: " + id);
    }
}
