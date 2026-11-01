package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.CreateEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.command.RequestJoinEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.command.UpdateEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.result.EmployeeRegistrationResult;
import com.erp.fleet.domain.exception.ActiveEmployeeRegistrationAlreadyExistsException;
import com.erp.fleet.domain.exception.EmployeeRegistrationNotFoundException;
import com.erp.fleet.domain.model.EmployeeRegistration;
import com.erp.fleet.domain.model.EmployeeRegistrationStatus;
import com.erp.fleet.domain.model.EmployeeRole;
import com.erp.fleet.domain.port.EmployeeRegistrationRepositoryPort;
import com.erp.fleet.domain.port.StaffCareerHistoryRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmployeeRegistrationServiceTest {

    private EmployeeRegistrationRepositoryPort repository;
    private StaffCareerHistoryRepositoryPort careerHistoryRepository;
    private UnitOfWork unitOfWork;
    private EmployeeRegistrationService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID registrationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmployeeRegistrationRepositoryPort.class);
        careerHistoryRepository = Mockito.mock(StaffCareerHistoryRepositoryPort.class);
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

        service = new EmployeeRegistrationService(repository, careerHistoryRepository, unitOfWork);
    }

    @Test
    @DisplayName("Should successfully execute direct staff registration and record career history")
    void shouldRegisterDirectStaff() {
        when(repository.existsActiveOrPendingRegistration(any(), any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateEmployeeRegistrationCommand cmd = new CreateEmployeeRegistrationCommand(
                tenantId, branchId, employeeId, "BRAKES", "Brake Tech",
                new BigDecimal("3000.00"), "ROLE_EMPLOYEE"
        );
        EmployeeRegistrationResult result = service.registerDirect(cmd);

        assertNotNull(result);
        assertEquals("ACTIVE", result.status());
        assertEquals("BRAKES", result.speciality());
        assertEquals(new BigDecimal("3000.00"), result.salary());

        verify(repository).save(any(EmployeeRegistration.class));
        verify(careerHistoryRepository).save(any());
    }

    @Test
    @DisplayName("Should reject direct registration when active or pending registration exists")
    void shouldRejectDuplicateDirectRegistration() {
        when(repository.existsActiveOrPendingRegistration(any(), any(), any())).thenReturn(true);

        CreateEmployeeRegistrationCommand cmd = new CreateEmployeeRegistrationCommand(
                tenantId, branchId, employeeId, "BRAKES", "Brake Tech",
                new BigDecimal("3000.00"), "ROLE_EMPLOYEE"
        );
        assertThrows(ActiveEmployeeRegistrationAlreadyExistsException.class, () -> service.registerDirect(cmd));
        verify(repository, never()).save(any());
        verify(careerHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should submit request-join in PENDING_APPROVAL status without career history entry")
    void shouldHandleRequestJoin() {
        when(repository.existsActiveOrPendingRegistration(any(), any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequestJoinEmployeeRegistrationCommand cmd = new RequestJoinEmployeeRegistrationCommand(
                tenantId, branchId, employeeId, "ENGINE", "Engine Tech",
                new BigDecimal("2600.00")
        );
        EmployeeRegistrationResult result = service.requestJoin(cmd);

        assertNotNull(result);
        assertEquals("PENDING_APPROVAL", result.status());
        verify(repository).save(any(EmployeeRegistration.class));
        verify(careerHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should approve request-join and record career history")
    void shouldApproveRequestJoin() {
        EmployeeRegistration pending = EmployeeRegistration.createRequestJoin(
                new TenantId(tenantId), new BranchId(branchId), new EmployeeId(employeeId),
                "ENGINE", "Engine Tech", new BigDecimal("2600.00")
        );
        when(repository.findById(any(), any())).thenReturn(Optional.of(pending));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeRegistrationResult result = service.approve(registrationId, tenantId);

        assertEquals("ACTIVE", result.status());
        assertNotNull(result.approvedAt());
        verify(careerHistoryRepository).save(any());
    }

    @Test
    @DisplayName("Should reject request-join with reason")
    void shouldRejectRequestJoin() {
        EmployeeRegistration pending = EmployeeRegistration.createRequestJoin(
                new TenantId(tenantId), new BranchId(branchId), new EmployeeId(employeeId),
                "ENGINE", "Engine Tech", new BigDecimal("2600.00")
        );
        when(repository.findById(any(), any())).thenReturn(Optional.of(pending));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeRegistrationResult result = service.reject(registrationId, tenantId, "Not hiring right now");

        assertEquals("REJECTED", result.status());
        assertEquals("Not hiring right now", result.rejectionReason());
    }

    @Test
    @DisplayName("Should update specialty and salary with career history entry")
    void shouldUpdateDetails() {
        EmployeeRegistration reg = EmployeeRegistration.createDirect(
                new TenantId(tenantId), new BranchId(branchId), new EmployeeId(employeeId),
                "BRAKES", "Brake Tech", new BigDecimal("3000.00"), EmployeeRole.ROLE_EMPLOYEE
        );
        when(repository.findById(any(), any())).thenReturn(Optional.of(reg));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateEmployeeRegistrationCommand cmd = new UpdateEmployeeRegistrationCommand(
                registrationId, tenantId, "SUSPENSION", "Suspension Lead", new BigDecimal("3500.00")
        );
        EmployeeRegistrationResult result = service.update(cmd);

        assertEquals("SUSPENSION", result.speciality());
        assertEquals("Suspension Lead", result.specialityName());
        assertEquals(new BigDecimal("3500.00"), result.salary());
        verify(careerHistoryRepository).save(any());
    }

    @Test
    @DisplayName("Should deactivate employee registration")
    void shouldDeactivate() {
        EmployeeRegistration reg = EmployeeRegistration.createDirect(
                new TenantId(tenantId), new BranchId(branchId), new EmployeeId(employeeId),
                "BRAKES", "Brake Tech", new BigDecimal("3000.00"), EmployeeRole.ROLE_EMPLOYEE
        );
        when(repository.findById(any(), any())).thenReturn(Optional.of(reg));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate(registrationId, tenantId);
        assertEquals(EmployeeRegistrationStatus.INACTIVE, reg.getStatus());
        verify(careerHistoryRepository).save(any());
    }

    @Test
    @DisplayName("Should list and get by id or throw EmployeeRegistrationNotFoundException")
    void shouldListAndGetById() {
        EmployeeRegistration reg = EmployeeRegistration.createDirect(
                new TenantId(tenantId), new BranchId(branchId), new EmployeeId(employeeId),
                "BRAKES", "Brake Tech", new BigDecimal("3000.00"), EmployeeRole.ROLE_EMPLOYEE
        );
        when(repository.findById(any(), any())).thenReturn(Optional.of(reg));
        when(repository.findByFilters(any(), any(), any(), any())).thenReturn(List.of(reg));

        EmployeeRegistrationResult single = service.getById(registrationId, tenantId);
        assertNotNull(single);

        List<EmployeeRegistrationResult> list = service.list(tenantId, branchId, employeeId, "ACTIVE");
        assertEquals(1, list.size());

        when(repository.findById(any(), any())).thenReturn(Optional.empty());
        assertThrows(EmployeeRegistrationNotFoundException.class, () -> service.getById(UUID.randomUUID(), tenantId));
    }
}
