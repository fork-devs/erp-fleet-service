package com.erp.fleet.application.dto.command;

import java.util.UUID;

public record UpdateCustomerRegistrationStatusCommand(
        UUID id,
        UUID tenantId,
        String status
) {}
