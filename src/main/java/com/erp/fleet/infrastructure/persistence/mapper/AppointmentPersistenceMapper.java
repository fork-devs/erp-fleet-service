package com.erp.fleet.infrastructure.persistence.mapper;

import com.erp.fleet.domain.model.Appointment;
import com.erp.fleet.domain.model.AppointmentStatus;
import com.erp.fleet.domain.valueobject.*;
import com.erp.fleet.infrastructure.persistence.entity.AppointmentJpaEntity;

public final class AppointmentPersistenceMapper {

    private AppointmentPersistenceMapper() {}

    public static AppointmentJpaEntity toEntity(Appointment a) {
        return new AppointmentJpaEntity(
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
                a.getUpdatedAt()
        );
    }

    public static Appointment toDomain(AppointmentJpaEntity e) {
        return new Appointment(
                new AppointmentId(e.getId()),
                new TenantId(e.getTenantId()),
                new BranchId(e.getBranchId()),
                new CustomerId(e.getCustomerId()),
                new VehicleId(e.getVehicleId()),
                e.getScheduledStart(),
                e.getScheduledEnd(),
                AppointmentStatus.valueOf(e.getStatus()),
                e.getNotes(),
                e.getCancellationReason(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
