package com.erp.fleet.application.dto.command;

import java.util.UUID;

public record CreateCustomerRegistrationCommand(
        UUID tenantId,
        UUID branchId,
        UUID customerId
) {}
