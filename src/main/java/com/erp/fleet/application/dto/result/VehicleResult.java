package com.erp.fleet.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record VehicleResult(
        UUID id,
        UUID tenantId,
        UUID customerId,
        String plate,
        String brand,
        String model,
        Integer year,
        String vin,
        String validationStatus,
        String status,
        Instant createdAt
) {}
