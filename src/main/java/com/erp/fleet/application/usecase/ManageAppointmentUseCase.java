package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.RescheduleAppointmentCommand;
import com.erp.fleet.application.dto.result.AppointmentResult;

import java.util.List;
import java.util.UUID;

public interface ManageAppointmentUseCase {
    AppointmentResult getById(UUID appointmentId, UUID tenantId);
    List<AppointmentResult> list(UUID tenantId, UUID branchId, UUID customerId, UUID vehicleId, String status);
    AppointmentResult reschedule(RescheduleAppointmentCommand command);
    AppointmentResult confirm(UUID appointmentId, UUID tenantId);
    AppointmentResult checkIn(UUID appointmentId, UUID tenantId);
    AppointmentResult complete(UUID appointmentId, UUID tenantId);
    AppointmentResult cancel(UUID appointmentId, UUID tenantId, String reason);
}
