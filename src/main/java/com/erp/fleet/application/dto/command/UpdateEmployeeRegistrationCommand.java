package com.erp.fleet.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateEmployeeRegistrationCommand(
        UUID id,
        UUID tenantId,
        String speciality,
        String specialityName,
        BigDecimal salary
) {}
