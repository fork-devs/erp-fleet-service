package com.erp.fleet.infrastructure.security;

import com.erp.fleet.domain.exception.StaffHierarchyViolationException;
import com.erp.fleet.infrastructure.persistence.repository.SpringDataEmployeeRegistrationRepository;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class MultiTenancySecurityServiceTest {

    private SpringDataEmployeeRegistrationRepository employeeRepo;
    private MultiTenancySecurityService securityService;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        employeeRepo = Mockito.mock(SpringDataEmployeeRegistrationRepository.class);
        securityService = new MultiTenancySecurityService(employeeRepo);
        TenantContextHolder.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    private void authenticate(String userId, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("SUPERADMIN should have full access to any branch and can manage any role")
    void superadminPermissions() {
        authenticate(userId.toString(), "ROLE_SUPERADMIN");

        assertTrue(securityService.isAuthorizedForBranch(branchId));
        assertTrue(securityService.canManageStaff("ROLE_BRANCH_MANAGER", branchId));
        assertTrue(securityService.canManageStaff("ROLE_ASSISTANT", branchId));
        assertTrue(securityService.canManageStaff("ROLE_EMPLOYEE", branchId));
        assertDoesNotThrow(() -> securityService.validateStaffManagement("EMPLOYEE", branchId));
    }

    @Test
    @DisplayName("OWNER should be authorized for branch and can manage branch manager, assistant, employee")
    void ownerPermissions() {
        authenticate(userId.toString(), "ROLE_OWNER");

        assertTrue(securityService.isAuthorizedForBranch(branchId));
        assertTrue(securityService.canManageStaff("ROLE_BRANCH_MANAGER", branchId));
        assertTrue(securityService.canManageStaff("ROLE_ASSISTANT", branchId));
        assertTrue(securityService.canManageStaff("ROLE_EMPLOYEE", branchId));
    }

    @Test
    @DisplayName("BRANCH_MANAGER can manage assistant and employee, but not another branch manager")
    void branchManagerPermissions() {
        authenticate(userId.toString(), "ROLE_BRANCH_MANAGER");
        when(employeeRepo.existsByTenantIdAndBranchIdAndEmployeeIdAndStatusIn(
                eq(tenantId), eq(branchId), eq(userId), anySet()
        )).thenReturn(true);

        assertTrue(securityService.isAuthorizedForBranch(branchId));
        assertFalse(securityService.canManageStaff("ROLE_BRANCH_MANAGER", branchId));
        assertTrue(securityService.canManageStaff("ROLE_ASSISTANT", branchId));
        assertTrue(securityService.canManageStaff("ROLE_EMPLOYEE", branchId));

        assertThrows(StaffHierarchyViolationException.class,
                () -> securityService.validateStaffManagement("ROLE_BRANCH_MANAGER", branchId));
    }

    @Test
    @DisplayName("ASSISTANT can only manage employee, not assistant or branch manager")
    void assistantPermissions() {
        authenticate(userId.toString(), "ROLE_ASSISTANT");
        when(employeeRepo.existsByTenantIdAndBranchIdAndEmployeeIdAndStatusIn(
                eq(tenantId), eq(branchId), eq(userId), anySet()
        )).thenReturn(true);

        assertTrue(securityService.isAuthorizedForBranch(branchId));
        assertFalse(securityService.canManageStaff("ROLE_BRANCH_MANAGER", branchId));
        assertFalse(securityService.canManageStaff("ROLE_ASSISTANT", branchId));
        assertTrue(securityService.canManageStaff("ROLE_EMPLOYEE", branchId));
    }

    @Test
    @DisplayName("EMPLOYEE cannot manage anyone")
    void employeePermissions() {
        authenticate(userId.toString(), "ROLE_EMPLOYEE");
        when(employeeRepo.existsByTenantIdAndBranchIdAndEmployeeIdAndStatusIn(
                eq(tenantId), eq(branchId), eq(userId), anySet()
        )).thenReturn(true);

        assertTrue(securityService.isAuthorizedForBranch(branchId));
        assertFalse(securityService.canManageStaff("ROLE_EMPLOYEE", branchId));
        assertFalse(securityService.canManageStaff("ROLE_ASSISTANT", branchId));
        assertFalse(securityService.canManageStaff("ROLE_BRANCH_MANAGER", branchId));

        assertThrows(StaffHierarchyViolationException.class,
                () -> securityService.validateStaffManagement("ROLE_EMPLOYEE", branchId));
    }

    @Test
    @DisplayName("Target role ADMIN or OWNER cannot be managed through staff onboarding")
    void rejectAdminOrOwnerAsTargetRole() {
        authenticate(userId.toString(), "ROLE_SUPERADMIN");

        assertFalse(securityService.canManageStaff("ROLE_ADMIN", branchId));
        assertFalse(securityService.canManageStaff("ROLE_OWNER", branchId));
    }
}
