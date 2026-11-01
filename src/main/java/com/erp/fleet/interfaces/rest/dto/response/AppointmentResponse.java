package com.erp.fleet.interfaces.rest.dto.response;

import com.erp.fleet.application.dto.result.AppointmentResult;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID branchId,
        UUID customerId,
        UUID vehicleId,
        Instant scheduledStart,
        Instant scheduledEnd,
        String status,
        String notes,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static AppointmentResponse from(AppointmentResult r) {
        return new AppointmentResponse(
                r.id(),
                r.branchId(),
                r.customerId(),
                r.vehicleId(),
                r.scheduledStart(),
                r.scheduledEnd(),
                r.status(),
                r.notes(),
                r.cancellationReason(),
                r.createdAt(),
                r.updatedAt()
        );
    }
}
