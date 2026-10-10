package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.UpdateCustomerCommand;
import com.erp.fleet.application.dto.result.CustomerResult;

public interface UpdateCustomerUseCase {
    CustomerResult update(UpdateCustomerCommand command);
}
