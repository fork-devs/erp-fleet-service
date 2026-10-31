package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.result.EmployeeRegistrationResult;

import java.util.List;
import java.util.UUID;

public interface GetEmployeeRegistrationsUseCase {
    EmployeeRegistrationResult getById(UUID id, UUID tenantId);
    List<EmployeeRegistrationResult> list(UUID tenantId, UUID branchId, UUID employeeId, String status);
}
