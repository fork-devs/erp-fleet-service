package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull(message = "{validation.appointment.branchId.required:Branch ID is required}")
        UUID branchId,

        @NotNull(message = "{validation.appointment.customerId.required:Customer ID is required}")
        UUID customerId,

        @NotNull(message = "{validation.appointment.vehicleId.required:Vehicle ID is required}")
        UUID vehicleId,

        @NotNull(message = "{validation.appointment.scheduledStart.required:Scheduled start is required}")
        @FutureOrPresent(message = "{validation.appointment.scheduledStart.future:Scheduled start must be in present or future}")
        Instant scheduledStart,

        Instant scheduledEnd,

        @Size(max = 500, message = "{validation.appointment.notes.size:Notes cannot exceed 500 characters}")
        String notes
) {}
