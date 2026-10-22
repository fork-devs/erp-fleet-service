package com.erp.fleet.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record VehicleResponse(
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
