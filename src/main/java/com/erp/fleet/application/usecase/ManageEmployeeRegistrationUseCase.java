package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.CreateEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.command.RequestJoinEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.command.UpdateEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.result.EmployeeRegistrationResult;

import java.util.UUID;

public interface ManageEmployeeRegistrationUseCase {
    EmployeeRegistrationResult registerDirect(CreateEmployeeRegistrationCommand command);
    EmployeeRegistrationResult requestJoin(RequestJoinEmployeeRegistrationCommand command);
    EmployeeRegistrationResult approve(UUID id, UUID tenantId);
    EmployeeRegistrationResult reject(UUID id, UUID tenantId, String reason);
    EmployeeRegistrationResult update(UpdateEmployeeRegistrationCommand command);
    void deactivate(UUID id, UUID tenantId);
}
