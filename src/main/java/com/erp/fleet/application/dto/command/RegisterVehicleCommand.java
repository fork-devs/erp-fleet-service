package com.erp.fleet.application.dto.command;

import java.util.UUID;

public record RegisterVehicleCommand(
        UUID tenantId,
        UUID customerId,
        String plate,
        String brand,
        String model,
        Integer year,
        String vin
) {}
