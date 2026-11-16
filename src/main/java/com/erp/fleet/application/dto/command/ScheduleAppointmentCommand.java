package com.erp.fleet.application.dto.command;

import com.erp.fleet.domain.model.AppointmentResources;

import java.time.Instant;
import java.util.UUID;

public record ScheduleAppointmentCommand(
        UUID tenantId,
        UUID branchId,
        UUID customerId,
        UUID vehicleId,
        Instant scheduledStart,
        Instant scheduledEnd,
        String notes,
        UUID sourceAlertId,
        AppointmentResources resources
) {
    public ScheduleAppointmentCommand(UUID tenantId, UUID branchId, UUID customerId, UUID vehicleId,
                                      Instant scheduledStart, Instant scheduledEnd, String notes, UUID sourceAlertId) {
        this(tenantId, branchId, customerId, vehicleId, scheduledStart, scheduledEnd, notes, sourceAlertId, null);
    }

    public ScheduleAppointmentCommand(UUID tenantId, UUID branchId, UUID customerId, UUID vehicleId,
                                      Instant scheduledStart, Instant scheduledEnd, String notes) {
        this(tenantId, branchId, customerId, vehicleId, scheduledStart, scheduledEnd, notes, null, null);
    }
}
