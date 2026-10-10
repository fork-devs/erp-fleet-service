package com.erp.fleet.application.usecase;

import java.util.UUID;

public interface DeactivateCustomerUseCase {
    void deactivate(UUID customerId, UUID tenantId);
}
