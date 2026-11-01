package com.erp.fleet.application.dto.result;

import com.erp.fleet.domain.model.CustomerRegistration;

import java.time.Instant;
import java.util.UUID;

public record CustomerRegistrationResult(
        UUID id,
        UUID tenantId,
        UUID branchId,
        UUID customerId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerRegistrationResult from(CustomerRegistration r) {
        return new CustomerRegistrationResult(
                r.getId().getValue(),
                r.getTenantId().getValue(),
                r.getBranchId().getValue(),
                r.getCustomerId().getValue(),
                r.getStatus().name(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
