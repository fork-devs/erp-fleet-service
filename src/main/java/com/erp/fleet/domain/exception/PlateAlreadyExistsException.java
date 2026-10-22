package com.erp.fleet.domain.exception;

public class PlateAlreadyExistsException extends DomainException {
    public PlateAlreadyExistsException(String plate) {
        super("FLEET_PLATE_DUPLICADO", "Ya existe un vehículo registrado con la placa: " + plate);
    }
}
