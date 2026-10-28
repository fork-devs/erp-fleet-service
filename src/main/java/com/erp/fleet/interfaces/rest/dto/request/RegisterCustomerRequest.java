package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterCustomerRequest(
        @NotBlank(message = "{validation.fleet.customer.full-name.not-blank}")
        String fullName,

        String email,

        String phone,

        @NotBlank(message = "{validation.fleet.customer.doc-type.not-blank}")
        String docType,

        @NotBlank(message = "{validation.fleet.customer.doc-number.not-blank}")
        String docNumber
) {}
