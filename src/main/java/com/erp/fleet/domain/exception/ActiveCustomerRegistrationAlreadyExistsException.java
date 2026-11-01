package com.erp.fleet.domain.exception;

public class ActiveCustomerRegistrationAlreadyExistsException extends DomainException {
    public ActiveCustomerRegistrationAlreadyExistsException(String customerId, String branchId) {
        super("FLEET_ACTIVE_CUSTOMER_REGISTRATION_EXISTS",
                "An active customer registration already exists for customer " + customerId + " in branch " + branchId);
    }
}
