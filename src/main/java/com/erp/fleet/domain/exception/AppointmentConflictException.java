package com.erp.fleet.domain.exception;

public class AppointmentConflictException extends DomainException {
    public AppointmentConflictException(String message) {
        super("FLEET_APPOINTMENT_CONFLICT", message);
    }
}
