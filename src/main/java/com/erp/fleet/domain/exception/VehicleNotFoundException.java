package com.erp.fleet.domain.exception;

public class VehicleNotFoundException extends DomainException {
    public VehicleNotFoundException(String id) {
        super("Vehículo no encontrado con ID: " + id);
    }
}
