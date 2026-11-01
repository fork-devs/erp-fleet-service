package com.erp.fleet.interfaces.rest.dto.response;

import com.erp.fleet.application.dto.result.EmployeeRegistrationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EmployeeRegistrationResponse(
        UUID id,
        UUID employeeId,
        UUID branchId,
        String speciality,
        String specialityName,
        BigDecimal salary,
        String role,
        String status,
        String rejectionReason,
        Instant createdAt,
        Instant approvedAt
) {
    public static EmployeeRegistrationResponse from(EmployeeRegistrationResult r) {
        return new EmployeeRegistrationResponse(
                r.id(),
                r.employeeId(),
                r.branchId(),
                r.speciality(),
                r.specialityName(),
                r.salary(),
                r.role(),
                r.status(),
                r.rejectionReason(),
                r.createdAt(),
                r.approvedAt()
        );
    }
}
