package com.erp.fleet.infrastructure.persistence.mapper;

import com.erp.fleet.domain.model.EmployeeRegistrationStatus;
import com.erp.fleet.domain.model.EmployeeRole;
import com.erp.fleet.domain.model.StaffCareerHistory;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.StaffCareerHistoryId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.entity.StaffCareerHistoryJpaEntity;

public final class StaffCareerHistoryPersistenceMapper {

    private StaffCareerHistoryPersistenceMapper() {}

    public static StaffCareerHistoryJpaEntity toEntity(StaffCareerHistory h) {
        return new StaffCareerHistoryJpaEntity(
                h.getId().getValue(),
                h.getTenantId().getValue(),
                h.getBranchId().getValue(),
                h.getEmployeeId().getValue(),
                h.getSpeciality(),
                h.getSpecialityName(),
                h.getSalary(),
                h.getRole().name(),
                h.getStatus().name(),
                h.getChangeReason(),
                h.getChangedAt()
        );
    }

    public static StaffCareerHistory toDomain(StaffCareerHistoryJpaEntity e) {
        return new StaffCareerHistory(
                new StaffCareerHistoryId(e.getId()),
                new TenantId(e.getTenantId()),
                new BranchId(e.getBranchId()),
                new EmployeeId(e.getEmployeeId()),
                e.getSpeciality(),
                e.getSpecialityName(),
                e.getSalary(),
                EmployeeRole.fromString(e.getRole()),
                EmployeeRegistrationStatus.valueOf(e.getStatus()),
                e.getChangeReason(),
                e.getChangedAt()
        );
    }
}
