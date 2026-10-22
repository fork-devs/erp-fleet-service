package com.erp.fleet.domain.exception;

public class VehicleNotFoundException extends DomainException {
    public VehicleNotFoundException(String id) {
        super("FLEET_VEHICLE_NOT_FOUND", "Vehículo no encontrado con ID: " + id);
    }
}
