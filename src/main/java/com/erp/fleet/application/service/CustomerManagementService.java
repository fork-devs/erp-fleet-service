package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.RegisterCustomerCommand;
import com.erp.fleet.application.dto.command.UpdateCustomerCommand;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.usecase.DeactivateCustomerUseCase;
import com.erp.fleet.application.usecase.GetCustomerUseCase;
import com.erp.fleet.application.usecase.RegisterCustomerUseCase;
import com.erp.fleet.application.usecase.UpdateCustomerUseCase;
import com.erp.fleet.domain.exception.CustomerNotFoundException;
import com.erp.fleet.domain.exception.DocAlreadyExistsException;
import com.erp.fleet.domain.model.Customer;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.DocNumber;
import com.erp.fleet.domain.valueobject.TenantId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerManagementService implements
        RegisterCustomerUseCase, GetCustomerUseCase,
        UpdateCustomerUseCase, DeactivateCustomerUseCase {

    private final CustomerRepositoryPort customerRepository;

    public CustomerManagementService(CustomerRepositoryPort customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerResult register(RegisterCustomerCommand cmd) {
        TenantId tenantId = new TenantId(cmd.tenantId());
        if (customerRepository.existsByDocNumber(cmd.docNumber(), tenantId))
            throw new DocAlreadyExistsException(cmd.docNumber());
        DocNumber docNumber = DocNumber.of(cmd.docNumber(), cmd.docType());
        Customer customer = Customer.create(tenantId, cmd.fullName(), cmd.email(), cmd.phone(), docNumber);
        return toResult(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResult getById(UUID customerId, UUID tenantId) {
        return customerRepository.findById(new CustomerId(customerId), new TenantId(tenantId))
                .map(this::toResult)
                .orElseThrow(() -> new CustomerNotFoundException(customerId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResult> listAll(UUID tenantId) {
        return customerRepository.findAllByTenantId(new TenantId(tenantId))
                .stream().map(this::toResult).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerResult update(UpdateCustomerCommand cmd) {
        TenantId tenantId = new TenantId(cmd.tenantId());
        Customer customer = customerRepository.findById(new CustomerId(cmd.customerId()), tenantId)
                .orElseThrow(() -> new CustomerNotFoundException(cmd.customerId().toString()));
        customer.update(cmd.fullName(), cmd.email(), cmd.phone());
        return toResult(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void deactivate(UUID customerId, UUID tenantId) {
        Customer customer = customerRepository.findById(new CustomerId(customerId), new TenantId(tenantId))
                .orElseThrow(() -> new CustomerNotFoundException(customerId.toString()));
        customer.deactivate();
        customerRepository.save(customer);
    }

    private CustomerResult toResult(Customer c) {
        return new CustomerResult(
                c.getId().getValue(), c.getTenantId().getValue(),
                c.getFullName(), c.getEmail(), c.getPhone(),
                c.getDocNumber().getDocType(), c.getDocNumber().getValue(),
                c.getValidationStatus().name(), c.getStatus().name(), c.getCreatedAt()
        );
    }
}
