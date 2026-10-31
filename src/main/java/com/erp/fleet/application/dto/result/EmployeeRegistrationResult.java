package com.erp.fleet.application.dto.result;

import com.erp.fleet.domain.model.EmployeeRegistration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EmployeeRegistrationResult(
        UUID id,
        UUID tenantId,
        UUID branchId,
        UUID employeeId,
        String speciality,
        String specialityName,
        BigDecimal salary,
        String role,
        String status,
        String rejectionReason,
        Instant createdAt,
        Instant approvedAt,
        Instant updatedAt
) {
    public static EmployeeRegistrationResult from(EmployeeRegistration r) {
        return new EmployeeRegistrationResult(
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
}
