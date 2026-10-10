package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterCustomerRequest(
        @NotBlank String fullName,
        String email,
        String phone,
        @NotBlank String docType,
        @NotBlank String docNumber
) {}
