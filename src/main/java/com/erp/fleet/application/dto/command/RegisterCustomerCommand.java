package com.erp.fleet.application.dto.command;

import java.util.UUID;

public record RegisterCustomerCommand(
        UUID tenantId,
        String fullName,
        String email,
        String phone,
        String docType,
        String docNumber
) {}
