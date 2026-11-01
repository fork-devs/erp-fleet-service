package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.CreateCustomerRegistrationCommand;
import com.erp.fleet.application.dto.command.UpdateCustomerRegistrationStatusCommand;
import com.erp.fleet.application.dto.result.CustomerRegistrationResult;
import com.erp.fleet.application.usecase.GetCustomerRegistrationsUseCase;
import com.erp.fleet.application.usecase.ManageCustomerRegistrationUseCase;
import com.erp.fleet.domain.exception.ActiveCustomerRegistrationAlreadyExistsException;
import com.erp.fleet.domain.exception.CustomerRegistrationNotFoundException;
import com.erp.fleet.domain.model.CustomerRegistration;
import com.erp.fleet.domain.model.CustomerRegistrationStatus;
import com.erp.fleet.domain.port.CustomerRegistrationRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.CustomerRegistrationId;
import com.erp.fleet.domain.valueobject.TenantId;

import java.util.List;
import java.util.UUID;

public class CustomerRegistrationService implements ManageCustomerRegistrationUseCase, GetCustomerRegistrationsUseCase {

    private final CustomerRegistrationRepositoryPort repository;
    private final UnitOfWork unitOfWork;

    public CustomerRegistrationService(CustomerRegistrationRepositoryPort repository, UnitOfWork unitOfWork) {
        this.repository = repository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public CustomerRegistrationResult register(CreateCustomerRegistrationCommand command) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(command.tenantId());
            BranchId branchId = new BranchId(command.branchId());
            CustomerId customerId = new CustomerId(command.customerId());

            if (repository.existsActiveRegistration(tenantId, branchId, customerId)) {
                throw new ActiveCustomerRegistrationAlreadyExistsException(
                        customerId.toString(), branchId.toString()
                );
            }

            CustomerRegistration registration = CustomerRegistration.create(tenantId, branchId, customerId);
            CustomerRegistration saved = repository.save(registration);
            return CustomerRegistrationResult.from(saved);
        });
    }

    @Override
    public CustomerRegistrationResult updateStatus(UpdateCustomerRegistrationStatusCommand command) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(command.tenantId());
            CustomerRegistrationId id = new CustomerRegistrationId(command.id());

            CustomerRegistration registration = repository.findById(id, tenantId)
                    .orElseThrow(() -> new CustomerRegistrationNotFoundException(id.toString()));

            CustomerRegistrationStatus targetStatus = CustomerRegistrationStatus.valueOf(command.status().trim().toUpperCase());
            if (targetStatus == CustomerRegistrationStatus.ACTIVE) {
                if (repository.existsActiveRegistration(tenantId, registration.getBranchId(), registration.getCustomerId())) {
                    throw new ActiveCustomerRegistrationAlreadyExistsException(
                            registration.getCustomerId().toString(), registration.getBranchId().toString()
                    );
                }
                registration.activate();
            } else {
                registration.deactivate();
            }

            CustomerRegistration saved = repository.save(registration);
            return CustomerRegistrationResult.from(saved);
        });
    }

    @Override
    public void deactivate(UUID id, UUID tenantId) {
        unitOfWork.runInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            CustomerRegistrationId rId = new CustomerRegistrationId(id);

            CustomerRegistration registration = repository.findById(rId, tId)
                    .orElseThrow(() -> new CustomerRegistrationNotFoundException(rId.toString()));

            registration.deactivate();
            repository.save(registration);
        });
    }

    @Override
    public CustomerRegistrationResult getById(UUID id, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            CustomerRegistration registration = repository.findById(new CustomerRegistrationId(id), new TenantId(tenantId))
                    .orElseThrow(() -> new CustomerRegistrationNotFoundException(id.toString()));
            return CustomerRegistrationResult.from(registration);
        });
    }

    @Override
    public List<CustomerRegistrationResult> list(UUID tenantId, UUID branchId, UUID customerId, String status) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            BranchId bId = branchId != null ? new BranchId(branchId) : null;
            CustomerId cId = customerId != null ? new CustomerId(customerId) : null;
            CustomerRegistrationStatus st = (status != null && !status.trim().isEmpty())
                    ? CustomerRegistrationStatus.valueOf(status.trim().toUpperCase()) : null;

            return repository.findByFilters(tId, bId, cId, st).stream()
                    .map(CustomerRegistrationResult::from)
                    .toList();
        });
    }
}
