package com.erp.fleet.application.dto.command;

import java.util.UUID;

public record UpdateVehicleCommand(
        UUID vehicleId,
        UUID tenantId,
        String brand,
        String model,
        Integer year,
        String vin
) {}
