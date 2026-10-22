package com.erp.fleet.application.dto.command;

import java.util.UUID;

public record UpdateCustomerCommand(
        UUID customerId,
        UUID tenantId,
        String fullName,
        String email,
        String phone
) {}
