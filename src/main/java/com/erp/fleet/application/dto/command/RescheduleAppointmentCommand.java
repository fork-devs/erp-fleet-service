package com.erp.fleet.application.dto.command;

import java.time.Instant;
import java.util.UUID;

public record RescheduleAppointmentCommand(
        UUID appointmentId,
        UUID tenantId,
        UUID branchId,
        Instant scheduledStart,
        Instant scheduledEnd,
        String notes
) {}
