package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateAlertAppointmentRequest(
        @NotNull UUID branchId,
        @NotNull UUID vehicleId,
        @NotNull Instant scheduledStart,
        @NotNull Instant scheduledEnd,
        @Size(max = 500) String notes,
        @Positive Integer bayNumber,
        UUID technicianId
) {}
