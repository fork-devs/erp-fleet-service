package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.ValidateCustomerCommand;
import com.erp.fleet.application.dto.result.CustomerResult;

/**
 * Use case to validate customer identity document against the integration hub.
 */
public interface ValidateCustomerUseCase {

    CustomerResult validate(ValidateCustomerCommand command);

    // Compatibility alias
    default CustomerResult validar(ValidateCustomerCommand command) {
        return validate(command);
    }
}
