package com.erp.fleet.infrastructure.persistence.mapper;

import com.erp.fleet.domain.model.EmployeeRegistration;
import com.erp.fleet.domain.model.EmployeeRegistrationStatus;
import com.erp.fleet.domain.model.EmployeeRole;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.EmployeeRegistrationId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.entity.EmployeeRegistrationJpaEntity;

public final class EmployeeRegistrationPersistenceMapper {

    private EmployeeRegistrationPersistenceMapper() {}

    public static EmployeeRegistrationJpaEntity toEntity(EmployeeRegistration r) {
        return new EmployeeRegistrationJpaEntity(
                r.getId().getValue(),
                r.getTenantId().getValue(),
                r.getBranchId().getValue(),
                r.getEmployeeId().getValue(),
                r.getSpeciality(),
                r.getSpecialityName(),
                r.getSalary(),
                r.getRole().name(),
                r.getStatus().name(),
                r.getRejectionReason(),
                r.getCreatedAt(),
                r.getApprovedAt(),
                r.getUpdatedAt()
        );
    }

    public static EmployeeRegistration toDomain(EmployeeRegistrationJpaEntity e) {
        return new EmployeeRegistration(
                new EmployeeRegistrationId(e.getId()),
                new TenantId(e.getTenantId()),
                new BranchId(e.getBranchId()),
                new EmployeeId(e.getEmployeeId()),
                e.getSpeciality(),
                e.getSpecialityName(),
                e.getSalary(),
                EmployeeRole.fromString(e.getRole()),
                EmployeeRegistrationStatus.valueOf(e.getStatus()),
                e.getRejectionReason(),
                e.getCreatedAt(),
                e.getApprovedAt(),
                e.getUpdatedAt()
        );
    }
}
