package com.erp.fleet.infrastructure.persistence.adapter;

import com.erp.fleet.domain.model.Appointment;
import com.erp.fleet.domain.model.AppointmentStatus;
import com.erp.fleet.domain.port.AppointmentRepositoryPort;
import com.erp.fleet.domain.valueobject.*;
import com.erp.fleet.infrastructure.persistence.mapper.AppointmentPersistenceMapper;
import com.erp.fleet.infrastructure.persistence.repository.SpringDataAppointmentRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AppointmentPersistenceAdapter implements AppointmentRepositoryPort {

    private final SpringDataAppointmentRepository repo;

    public AppointmentPersistenceAdapter(SpringDataAppointmentRepository repo) {
        this.repo = repo;
    }

    @Override
    public Appointment save(Appointment appointment) {
        var entity = AppointmentPersistenceMapper.toEntity(appointment);
        var saved = repo.save(entity);
        return AppointmentPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Appointment> findById(AppointmentId id, TenantId tenantId) {
        return repo.findByIdAndTenantId(id.getValue(), tenantId.getValue())
                .map(AppointmentPersistenceMapper::toDomain);
    }

    @Override
    public List<Appointment> findWithFilters(TenantId tenantId, BranchId branchId,
                                            CustomerId customerId, VehicleId vehicleId,
                                            AppointmentStatus status) {
        UUID branchUid = branchId != null ? branchId.getValue() : null;
        UUID customerUid = customerId != null ? customerId.getValue() : null;
        UUID vehicleUid = vehicleId != null ? vehicleId.getValue() : null;
        String statusStr = status != null ? status.name() : null;

        return repo.findWithFilters(tenantId.getValue(), branchUid, customerUid, vehicleUid, statusStr).stream()
                .map(AppointmentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean hasOverlappingAppointment(TenantId tenantId, VehicleId vehicleId,
                                            Instant start, Instant end, AppointmentId excludeId) {
        UUID excludeUid = excludeId != null ? excludeId.getValue() : null;
        return repo.hasOverlappingAppointment(tenantId.getValue(), vehicleId.getValue(), start, end, excludeUid);
    }
}
