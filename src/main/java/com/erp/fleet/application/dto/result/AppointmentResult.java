package com.erp.fleet.application.dto.result;

import com.erp.fleet.domain.model.Appointment;
import com.erp.fleet.domain.model.AppointmentResources;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResult(
        UUID id,
        UUID tenantId,
        UUID branchId,
        UUID customerId,
        UUID vehicleId,
        Instant scheduledStart,
        Instant scheduledEnd,
        String status,
        String notes,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt,
        AppointmentResources resources
) {
    public AppointmentResult(UUID id, UUID tenantId, UUID branchId, UUID customerId, UUID vehicleId,
                             Instant scheduledStart, Instant scheduledEnd, String status, String notes,
                             String cancellationReason, Instant createdAt, Instant updatedAt) {
        this(id, tenantId, branchId, customerId, vehicleId, scheduledStart, scheduledEnd,
                status, notes, cancellationReason, createdAt, updatedAt, null);
    }

    public static AppointmentResult from(Appointment a) {
        return new AppointmentResult(
                a.getId().getValue(),
                a.getTenantId().getValue(),
                a.getBranchId().getValue(),
                a.getCustomerId().getValue(),
                a.getVehicleId().getValue(),
                a.getScheduledStart(),
                a.getScheduledEnd(),
                a.getStatus().name(),
                a.getNotes(),
                a.getCancellationReason(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getResources()
        );
    }
}
