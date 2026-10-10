package com.erp.fleet.interfaces.rest.dto.request;

public record UpdateVehicleRequest(
        String brand,
        String model,
        Integer year,
        String vin
) {}
