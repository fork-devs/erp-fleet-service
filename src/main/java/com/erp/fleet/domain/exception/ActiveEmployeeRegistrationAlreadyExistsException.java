package com.erp.fleet.domain.exception;

public class ActiveEmployeeRegistrationAlreadyExistsException extends DomainException {
    public ActiveEmployeeRegistrationAlreadyExistsException(String employeeId, String branchId) {
        super("FLEET_ACTIVE_EMPLOYEE_REGISTRATION_EXISTS",
                "An active or pending employee registration already exists for employee " + employeeId + " in branch " + branchId);
    }
}
