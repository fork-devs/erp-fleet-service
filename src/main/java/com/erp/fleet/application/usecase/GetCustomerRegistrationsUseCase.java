package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.result.CustomerRegistrationResult;

import java.util.List;
import java.util.UUID;

public interface GetCustomerRegistrationsUseCase {
    CustomerRegistrationResult getById(UUID id, UUID tenantId);
    List<CustomerRegistrationResult> list(UUID tenantId, UUID branchId, UUID customerId, String status);
}
