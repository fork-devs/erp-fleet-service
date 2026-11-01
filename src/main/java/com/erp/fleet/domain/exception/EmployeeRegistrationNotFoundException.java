package com.erp.fleet.domain.exception;

public class EmployeeRegistrationNotFoundException extends DomainException {
    public EmployeeRegistrationNotFoundException(String id) {
        super("FLEET_EMPLOYEE_REGISTRATION_NOT_FOUND", "Employee registration not found with ID: " + id);
    }
}
