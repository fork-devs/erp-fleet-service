package com.erp.fleet.interfaces.rest.dto.request;

import java.math.BigDecimal;

public record UpdateEmployeeRegistrationRequest(
        String speciality,
        String specialityName,
        BigDecimal salary
) {}
