package com.erp.fleet.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateEmployeeRegistrationCommand(
        UUID tenantId,
        UUID branchId,
        UUID employeeId,
        String speciality,
        String specialityName,
        BigDecimal salary,
        String role
) {}
