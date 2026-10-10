package com.erp.fleet.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        UUID tenantId,
        String fullName,
        String email,
        String phone,
        String docType,
        String docNumber,
        String validationStatus,
        String status,
        Instant createdAt
) {}
