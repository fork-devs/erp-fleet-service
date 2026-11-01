package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterVehicleRequest(
        @NotNull(message = "{validation.fleet.vehicle.customer-id.not-null}")
        UUID customerId,

        @NotBlank(message = "{validation.fleet.vehicle.plate.not-blank}")
        String plate,

        String brand,

        String model,

        Integer year,

        String vin
) {}
