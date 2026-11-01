package com.erp.fleet.interfaces.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateEmployeeRegistrationRequest(
        @NotNull(message = "employeeId is required")
        UUID employeeId,

        @NotNull(message = "branchId is required")
        UUID branchId,

        @NotBlank(message = "speciality is required")
        String speciality,

        @NotBlank(message = "specialityName is required")
        String specialityName,

        @NotNull(message = "salary is required")
        @DecimalMin(value = "0.0", message = "salary must be non-negative")
        BigDecimal salary,

        String role
) {}
