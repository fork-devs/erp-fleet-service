package com.erp.fleet.domain.exception;

public class InvalidSpecialtyException extends DomainException {
    public InvalidSpecialtyException(String specialty) {
        super("FLEET_INVALID_SPECIALTY", "Specialty '" + specialty + "' does not exist in workshop active catalog");
    }
}
