package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.CreateCustomerRegistrationCommand;
import com.erp.fleet.application.dto.command.UpdateCustomerRegistrationStatusCommand;
import com.erp.fleet.application.dto.result.CustomerRegistrationResult;

import java.util.UUID;

public interface ManageCustomerRegistrationUseCase {
    CustomerRegistrationResult register(CreateCustomerRegistrationCommand command);
    CustomerRegistrationResult updateStatus(UpdateCustomerRegistrationStatusCommand command);
    void deactivate(UUID id, UUID tenantId);
}
