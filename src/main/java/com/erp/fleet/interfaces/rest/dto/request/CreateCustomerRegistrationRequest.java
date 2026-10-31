package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCustomerRegistrationRequest(
        @NotNull(message = "branchId is required")
        UUID branchId,

        @NotNull(message = "customerId is required")
        UUID customerId
) {}
