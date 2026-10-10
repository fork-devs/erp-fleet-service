package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.result.CustomerResult;

import java.util.List;
import java.util.UUID;

public interface GetCustomerUseCase {
    CustomerResult getById(UUID customerId, UUID tenantId);
    List<CustomerResult> listAll(UUID tenantId);
}
