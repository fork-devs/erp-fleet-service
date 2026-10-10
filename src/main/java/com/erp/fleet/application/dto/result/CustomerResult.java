package com.erp.fleet.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record CustomerResult(
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
