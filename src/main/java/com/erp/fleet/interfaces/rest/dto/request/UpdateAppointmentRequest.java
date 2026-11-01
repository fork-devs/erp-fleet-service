package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateAppointmentRequest(
        @NotNull(message = "{validation.appointment.branchId.required:Branch ID is required}")
        UUID branchId,

        @NotNull(message = "{validation.appointment.scheduledStart.required:Scheduled start is required}")
        Instant scheduledStart,

        Instant scheduledEnd,

        @Size(max = 500, message = "{validation.appointment.notes.size:Notes cannot exceed 500 characters}")
        String notes
) {}
