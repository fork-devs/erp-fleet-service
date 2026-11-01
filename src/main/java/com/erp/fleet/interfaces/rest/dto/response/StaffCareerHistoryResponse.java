package com.erp.fleet.interfaces.rest.dto.response;

import com.erp.fleet.application.dto.result.StaffCareerHistoryResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StaffCareerHistoryResponse(
        UUID id,
        UUID employeeId,
        UUID branchId,
        String speciality,
        String specialityName,
        BigDecimal salary,
        String role,
        String status,
        Instant changedAt,
        String changeReason
) {
    public static StaffCareerHistoryResponse from(StaffCareerHistoryResult r) {
        return new StaffCareerHistoryResponse(
                r.id(),
                r.employeeId(),
                r.branchId(),
                r.speciality(),
                r.specialityName(),
                r.salary(),
                r.role(),
                r.status(),
                r.changedAt(),
                r.changeReason()
        );
    }
}
