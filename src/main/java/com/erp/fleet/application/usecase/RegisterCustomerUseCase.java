package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.RegisterCustomerCommand;
import com.erp.fleet.application.dto.result.CustomerResult;

public interface RegisterCustomerUseCase {
    CustomerResult register(RegisterCustomerCommand command);
}
