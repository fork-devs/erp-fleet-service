package com.erp.fleet.application.dto.result;

import com.erp.fleet.domain.model.StaffCareerHistory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StaffCareerHistoryResult(
        UUID id,
        UUID tenantId,
        UUID branchId,
        UUID employeeId,
        String speciality,
        String specialityName,
        BigDecimal salary,
        String role,
        String status,
        String changeReason,
        Instant changedAt
) {
    public static StaffCareerHistoryResult from(StaffCareerHistory h) {
        return new StaffCareerHistoryResult(
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
}
