package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.CreateCustomerRegistrationCommand;
import com.erp.fleet.application.dto.command.UpdateCustomerRegistrationStatusCommand;
import com.erp.fleet.application.dto.result.CustomerRegistrationResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomerRegistrationServiceTest {

    private CustomerRegistrationRepositoryPort repository;
    private UnitOfWork unitOfWork;
    private CustomerRegistrationService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID registrationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CustomerRegistrationRepositoryPort.class);
        unitOfWork = Mockito.mock(UnitOfWork.class);

        when(unitOfWork.callInTransaction(any())).thenAnswer(invocation -> {
            Supplier<?> s = invocation.getArgument(0);
            return s.get();
        });

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(unitOfWork).runInTransaction(any());

        service = new CustomerRegistrationService(repository, unitOfWork);
    }

    @Test
    @DisplayName("Should successfully register customer to branch when no active registration exists")
    void shouldRegisterCustomerSuccessfully() {
        when(repository.existsActiveRegistration(any(), any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateCustomerRegistrationCommand cmd = new CreateCustomerRegistrationCommand(tenantId, branchId, customerId);
        CustomerRegistrationResult result = service.register(cmd);

        assertNotNull(result);
        assertEquals(tenantId, result.tenantId());
        assertEquals(branchId, result.branchId());
        assertEquals(customerId, result.customerId());
        assertEquals("ACTIVE", result.status());

        verify(repository).save(any(CustomerRegistration.class));
    }

    @Test
    @DisplayName("Should reject registration when active registration already exists in branch")
    void shouldRejectDuplicateActiveRegistration() {
        when(repository.existsActiveRegistration(any(), any(), any())).thenReturn(true);

        CreateCustomerRegistrationCommand cmd = new CreateCustomerRegistrationCommand(tenantId, branchId, customerId);
        assertThrows(ActiveCustomerRegistrationAlreadyExistsException.class, () -> service.register(cmd));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should update status to INACTIVE and deactivate registration")
    void shouldUpdateStatusAndDeactivate() {
        CustomerRegistration reg = CustomerRegistration.create(
                new TenantId(tenantId), new BranchId(branchId), new CustomerId(customerId)
        );
        when(repository.findById(any(), any())).thenReturn(Optional.of(reg));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateCustomerRegistrationStatusCommand cmd = new UpdateCustomerRegistrationStatusCommand(
                registrationId, tenantId, "INACTIVE"
        );
        CustomerRegistrationResult result = service.updateStatus(cmd);

        assertEquals("INACTIVE", result.status());

        service.deactivate(registrationId, tenantId);
        assertEquals(CustomerRegistrationStatus.INACTIVE, reg.getStatus());
    }

    @Test
    @DisplayName("Should list and get by id or throw CustomerRegistrationNotFoundException")
    void shouldListAndGetById() {
        CustomerRegistration reg = CustomerRegistration.create(
                new TenantId(tenantId), new BranchId(branchId), new CustomerId(customerId)
        );
        when(repository.findById(any(), any())).thenReturn(Optional.of(reg));
        when(repository.findByFilters(any(), any(), any(), any())).thenReturn(List.of(reg));

        CustomerRegistrationResult single = service.getById(registrationId, tenantId);
        assertNotNull(single);

        List<CustomerRegistrationResult> list = service.list(tenantId, branchId, customerId, "ACTIVE");
        assertEquals(1, list.size());

        when(repository.findById(any(), any())).thenReturn(Optional.empty());
        assertThrows(CustomerRegistrationNotFoundException.class, () -> service.getById(UUID.randomUUID(), tenantId));
    }
}
