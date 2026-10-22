package com.erp.fleet.application.service;

import com.erp.fleet.application.logging.SecurityEventLogger;
import com.erp.fleet.application.dto.command.RegisterCustomerCommand;
import com.erp.fleet.application.dto.command.UpdateCustomerCommand;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.usecase.DeactivateCustomerUseCase;
import com.erp.fleet.application.usecase.GetCustomerUseCase;
import com.erp.fleet.application.usecase.RegisterCustomerUseCase;
import com.erp.fleet.application.usecase.UpdateCustomerUseCase;
import com.erp.fleet.domain.exception.CustomerNotFoundException;
import com.erp.fleet.domain.exception.CustomerWithVehiclesException;
import com.erp.fleet.domain.exception.DocAlreadyExistsException;
import com.erp.fleet.domain.model.Customer;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.DocNumber;
import com.erp.fleet.domain.valueobject.TenantId;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomerManagementService implements
        RegisterCustomerUseCase, GetCustomerUseCase,
        UpdateCustomerUseCase, DeactivateCustomerUseCase {

    private final CustomerRepositoryPort customerRepository;
    private final VehicleRepositoryPort vehicleRepository;
    private final UnitOfWork unitOfWork;

    public CustomerManagementService(CustomerRepositoryPort customerRepository,
                                      VehicleRepositoryPort vehicleRepository,
                                      UnitOfWork unitOfWork) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public CustomerResult register(RegisterCustomerCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            if (customerRepository.existsByDocNumber(cmd.docNumber(), tenantId)) {
                throw new DocAlreadyExistsException(cmd.docNumber());
            }
            DocNumber docNumber = DocNumber.of(cmd.docNumber(), cmd.docType());
            Customer customer = Customer.create(tenantId, cmd.fullName(), cmd.email(), cmd.phone(), docNumber);
            CustomerResult resultado = toResult(customerRepository.save(customer));
            SecurityEventLogger.registrar("CLIENTE_ALTA", tenantId.getValue(), null,
                    org.slf4j.MDC.get("trace_id"), "OK",
                    java.util.Map.of("cliente_id", resultado.id().toString()));
            return resultado;
        });
    }

    @Override
    public CustomerResult getById(UUID customerId, UUID tenantId) {
        return unitOfWork.callInTransaction(() ->
                customerRepository.findById(new CustomerId(customerId), new TenantId(tenantId))
                        .map(this::toResult)
                        .orElseThrow(() -> new CustomerNotFoundException(customerId.toString())));
    }

    @Override
    public List<CustomerResult> listAll(UUID tenantId) {
        return unitOfWork.callInTransaction(() ->
                customerRepository.findAllByTenantId(new TenantId(tenantId))
                        .stream().map(this::toResult).collect(Collectors.toList()));
    }

    @Override
    public CustomerResult update(UpdateCustomerCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            Customer customer = customerRepository.findById(new CustomerId(cmd.customerId()), tenantId)
                    .orElseThrow(() -> new CustomerNotFoundException(cmd.customerId().toString()));
            customer.update(cmd.fullName(), cmd.email(), cmd.phone());
            return toResult(customerRepository.save(customer));
        });
    }

    /**
     * La baja es LOGICA: el registro se conserva. Un cliente con vehiculos
     * asociados no se puede dar de baja: los vehiculos seguirian apuntando a
     * un cliente dado de baja, y la orden de trabajo de un taller no puede
     * quedarse sin propietario.
     */
    @Override
    public void deactivate(UUID customerId, UUID tenantId) {
        unitOfWork.runInTransaction(() -> {
            TenantId taller = new TenantId(tenantId);
            Customer customer = customerRepository.findById(new CustomerId(customerId), taller)
                    .orElseThrow(() -> new CustomerNotFoundException(customerId.toString()));

            if (!vehicleRepository.findByCustomerId(new CustomerId(customerId), taller).isEmpty()) {
                throw new CustomerWithVehiclesException(customerId.toString());
            }

            customer.deactivate();
            customerRepository.save(customer);
            SecurityEventLogger.registrar("CLIENTE_BAJA", taller.getValue(), null,
                    org.slf4j.MDC.get("trace_id"), "OK",
                    java.util.Map.of("cliente_id", customerId.toString()));
        });
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
