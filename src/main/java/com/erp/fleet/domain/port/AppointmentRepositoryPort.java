package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.Appointment;
import com.erp.fleet.domain.model.AppointmentStatus;
import com.erp.fleet.domain.valueobject.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepositoryPort {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(AppointmentId id, TenantId tenantId);
    List<Appointment> findWithFilters(TenantId tenantId, BranchId branchId, CustomerId customerId, VehicleId vehicleId, AppointmentStatus status);
    boolean hasOverlappingAppointment(TenantId tenantId, VehicleId vehicleId, Instant start, Instant end, AppointmentId excludeId);
}
