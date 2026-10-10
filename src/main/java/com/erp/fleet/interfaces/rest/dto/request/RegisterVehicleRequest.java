package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterVehicleRequest(
        @NotNull UUID customerId,
        @NotBlank String plate,
        String brand,
        String model,
        Integer year,
        String vin
) {}
