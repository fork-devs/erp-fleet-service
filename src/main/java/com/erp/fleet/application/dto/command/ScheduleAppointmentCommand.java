package com.erp.fleet.application.dto.command;

import java.time.Instant;
import java.util.UUID;

public record ScheduleAppointmentCommand(
        UUID tenantId,
        UUID branchId,
        UUID customerId,
        UUID vehicleId,
        Instant scheduledStart,
        Instant scheduledEnd,
        String notes
) {}
