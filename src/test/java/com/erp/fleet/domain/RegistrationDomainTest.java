package com.erp.fleet.domain;

import com.erp.fleet.domain.model.*;
import com.erp.fleet.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationDomainTest {

    private final TenantId tenantId = new TenantId(UUID.randomUUID());
    private final BranchId branchId = new BranchId(UUID.randomUUID());
    private final CustomerId customerId = new CustomerId(UUID.randomUUID());
    private final EmployeeId employeeId = new EmployeeId(UUID.randomUUID());

    @Test
    @DisplayName("Should create CustomerRegistration in ACTIVE status and handle deactivation/reactivation")
    void shouldHandleCustomerRegistrationLifecycle() {
        CustomerRegistration reg = CustomerRegistration.create(tenantId, branchId, customerId);

        assertNotNull(reg.getId());
        assertEquals(tenantId, reg.getTenantId());
        assertEquals(branchId, reg.getBranchId());
        assertEquals(customerId, reg.getCustomerId());
        assertEquals(CustomerRegistrationStatus.ACTIVE, reg.getStatus());
        assertNotNull(reg.getCreatedAt());
        assertNotNull(reg.getUpdatedAt());

        reg.deactivate();
        assertEquals(CustomerRegistrationStatus.INACTIVE, reg.getStatus());

        reg.activate();
        assertEquals(CustomerRegistrationStatus.ACTIVE, reg.getStatus());
    }

    @Test
    @DisplayName("Should validate required fields in CustomerRegistration")
    void shouldValidateCustomerRegistrationInvariants() {
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerRegistration(null, tenantId, branchId, customerId, CustomerRegistrationStatus.ACTIVE, null, null));
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerRegistration(CustomerRegistrationId.generate(), null, branchId, customerId, CustomerRegistrationStatus.ACTIVE, null, null));
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerRegistration(CustomerRegistrationId.generate(), tenantId, null, customerId, CustomerRegistrationStatus.ACTIVE, null, null));
        assertThrows(IllegalArgumentException.class, () ->
                new CustomerRegistration(CustomerRegistrationId.generate(), tenantId, branchId, null, CustomerRegistrationStatus.ACTIVE, null, null));
    }

    @Test
    @DisplayName("Should create EmployeeRegistration via direct onboarding with ACTIVE status")
    void shouldHandleDirectEmployeeRegistration() {
        EmployeeRegistration reg = EmployeeRegistration.createDirect(
                tenantId, branchId, employeeId, "BRAKES", "Brake Specialist",
                new BigDecimal("2800.00"), EmployeeRole.ROLE_EMPLOYEE
        );

        assertNotNull(reg.getId());
        assertEquals(EmployeeRegistrationStatus.ACTIVE, reg.getStatus());
        assertEquals("BRAKES", reg.getSpeciality());
        assertEquals("Brake Specialist", reg.getSpecialityName());
        assertEquals(new BigDecimal("2800.00"), reg.getSalary());
        assertEquals(EmployeeRole.ROLE_EMPLOYEE, reg.getRole());
        assertNull(reg.getApprovedAt());
        assertNull(reg.getRejectionReason());

        reg.updateDetails("SUSPENSION", "Suspension Expert", new BigDecimal("3200.00"));
        assertEquals("SUSPENSION", reg.getSpeciality());
        assertEquals("Suspension Expert", reg.getSpecialityName());
        assertEquals(new BigDecimal("3200.00"), reg.getSalary());

        reg.deactivate();
        assertEquals(EmployeeRegistrationStatus.INACTIVE, reg.getStatus());
    }

    @Test
    @DisplayName("Should handle request-join lifecycle: pending, approval, rejection")
    void shouldHandleRequestJoinLifecycle() {
        EmployeeRegistration reg = EmployeeRegistration.createRequestJoin(
                tenantId, branchId, employeeId, "ENGINE_DIESEL", "Diesel Mechanic",
                new BigDecimal("2500.00")
        );

        assertEquals(EmployeeRegistrationStatus.PENDING_APPROVAL, reg.getStatus());
        assertNull(reg.getApprovedAt());

        // Approve
        reg.approve();
        assertEquals(EmployeeRegistrationStatus.ACTIVE, reg.getStatus());
        assertNotNull(reg.getApprovedAt());

        // Cannot approve again
        assertThrows(IllegalStateException.class, reg::approve);
        assertThrows(IllegalStateException.class, () -> reg.reject("No reason"));

        // New request to test rejection
        EmployeeRegistration reg2 = EmployeeRegistration.createRequestJoin(
                tenantId, branchId, employeeId, "ELECTRICIAN", "Auto Electrician",
                new BigDecimal("2700.00")
        );
        reg2.reject("Branch quota is full");
        assertEquals(EmployeeRegistrationStatus.REJECTED, reg2.getStatus());
        assertEquals("Branch quota is full", reg2.getRejectionReason());

        // Cannot reject or approve again
        assertThrows(IllegalStateException.class, () -> reg2.reject("Another reason"));
        assertThrows(IllegalStateException.class, reg2::approve);
    }

    @Test
    @DisplayName("Should record staff career history entries accurately")
    void shouldRecordStaffCareerHistory() {
        StaffCareerHistory history = StaffCareerHistory.record(
                tenantId, branchId, employeeId, "BRAKES", "Brakes Tech",
                new BigDecimal("2900.00"), EmployeeRole.ROLE_EMPLOYEE,
                EmployeeRegistrationStatus.ACTIVE, "Annual salary review"
        );

        assertNotNull(history.getId());
        assertEquals(tenantId, history.getTenantId());
        assertEquals(branchId, history.getBranchId());
        assertEquals(employeeId, history.getEmployeeId());
        assertEquals("BRAKES", history.getSpeciality());
        assertEquals("Brakes Tech", history.getSpecialityName());
        assertEquals(new BigDecimal("2900.00"), history.getSalary());
        assertEquals(EmployeeRole.ROLE_EMPLOYEE, history.getRole());
        assertEquals(EmployeeRegistrationStatus.ACTIVE, history.getStatus());
        assertEquals("Annual salary review", history.getChangeReason());
        assertNotNull(history.getChangedAt());
    }

    @Test
    @DisplayName("Should parse employee role string correctly with or without ROLE_ prefix")
    void shouldParseEmployeeRoles() {
        assertEquals(EmployeeRole.ROLE_EMPLOYEE, EmployeeRole.fromString("ROLE_EMPLOYEE"));
        assertEquals(EmployeeRole.ROLE_EMPLOYEE, EmployeeRole.fromString("EMPLOYEE"));
        assertEquals(EmployeeRole.ROLE_EMPLOYEE, EmployeeRole.fromString("employee"));
        assertEquals(EmployeeRole.ROLE_BRANCH_MANAGER, EmployeeRole.fromString("BRANCH_MANAGER"));
        assertEquals(EmployeeRole.ROLE_BRANCH_MANAGER, EmployeeRole.fromString("ROLE_BRANCH_MANAGER"));
        assertEquals(EmployeeRole.ROLE_ASSISTANT, EmployeeRole.fromString("ASSISTANT"));
        assertEquals(EmployeeRole.ROLE_ASSISTANT, EmployeeRole.fromString("ROLE_ASSISTANT"));
        assertEquals(EmployeeRole.ROLE_EMPLOYEE, EmployeeRole.fromString(null));
        assertEquals(EmployeeRole.ROLE_EMPLOYEE, EmployeeRole.fromString(""));

        assertThrows(IllegalArgumentException.class, () -> EmployeeRole.fromString("SUPERADMIN"));
        assertThrows(IllegalArgumentException.class, () -> EmployeeRole.fromString("ROLE_OWNER"));
    }

    @Test
    @DisplayName("Should verify equals and hashcode for registration value objects")
    void shouldVerifyValueObjects() {
        UUID u1 = UUID.randomUUID();
        CustomerRegistrationId cr1 = new CustomerRegistrationId(u1);
        CustomerRegistrationId cr2 = new CustomerRegistrationId(u1);
        assertEquals(cr1, cr2);
        assertEquals(cr1.hashCode(), cr2.hashCode());
        assertEquals(u1.toString(), cr1.toString());

        EmployeeRegistrationId er1 = new EmployeeRegistrationId(u1);
        EmployeeRegistrationId er2 = new EmployeeRegistrationId(u1);
        assertEquals(er1, er2);
        assertEquals(er1.hashCode(), er2.hashCode());
        assertEquals(u1.toString(), er1.toString());

        EmployeeId e1 = new EmployeeId(u1);
        EmployeeId e2 = new EmployeeId(u1);
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertEquals(u1.toString(), e1.toString());

        StaffCareerHistoryId sch1 = new StaffCareerHistoryId(u1);
        StaffCareerHistoryId sch2 = new StaffCareerHistoryId(u1);
        assertEquals(sch1, sch2);
        assertEquals(sch1.hashCode(), sch2.hashCode());
        assertEquals(u1.toString(), sch1.toString());
    }
}
