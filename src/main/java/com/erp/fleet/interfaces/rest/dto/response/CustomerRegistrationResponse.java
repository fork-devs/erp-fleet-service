package com.erp.fleet.interfaces.rest.dto.response;

import com.erp.fleet.application.dto.result.CustomerRegistrationResult;

import java.time.Instant;
import java.util.UUID;

public record CustomerRegistrationResponse(
        UUID id,
        UUID branchId,
        UUID customerId,
        String status,
        Instant createdAt
) {
    public static CustomerRegistrationResponse from(CustomerRegistrationResult r) {
        return new CustomerRegistrationResponse(
                r.id(),
                r.branchId(),
                r.customerId(),
                r.status(),
                r.createdAt()
        );
    }
}
