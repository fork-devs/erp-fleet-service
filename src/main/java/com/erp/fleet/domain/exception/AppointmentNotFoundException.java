package com.erp.fleet.domain.exception;

public class AppointmentNotFoundException extends DomainException {
    public AppointmentNotFoundException(String id) {
        super("FLEET_APPOINTMENT_NOT_FOUND", "Cita no encontrada con ID: " + id);
    }
}
