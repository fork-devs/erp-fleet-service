package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateCustomerRegistrationRequest(
        @NotBlank(message = "status is required")
        String status
) {}
