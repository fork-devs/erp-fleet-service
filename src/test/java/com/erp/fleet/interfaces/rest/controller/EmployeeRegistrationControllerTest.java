package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.command.CreateEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.command.RequestJoinEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.command.UpdateEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.result.EmployeeRegistrationResult;
import com.erp.fleet.application.usecase.GetEmployeeRegistrationsUseCase;
import com.erp.fleet.application.usecase.ManageEmployeeRegistrationUseCase;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import com.erp.fleet.infrastructure.security.MultiTenancySecurityService;
import com.erp.fleet.interfaces.rest.advice.GlobalRestExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeRegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageEmployeeRegistrationUseCase manageUseCase;

    @Mock
    private GetEmployeeRegistrationsUseCase getUseCase;

    @Mock
    private MultiTenancySecurityService securityService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID registrationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(tenantId);
        EmployeeRegistrationController controller = new EmployeeRegistrationController(
                manageUseCase, getUseCase, securityService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("POST /api/v1/employee-registrations - Direct staff onboarding returns 201")
    void shouldRegisterDirectStaff() throws Exception {
        EmployeeRegistrationResult result = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "BRAKES", "Brake Tech",
                new BigDecimal("2800.00"), "ROLE_EMPLOYEE", "ACTIVE", null, Instant.now(), null, Instant.now()
        );
        when(manageUseCase.registerDirect(any(CreateEmployeeRegistrationCommand.class))).thenReturn(result);

        String payload = """
            {
                "employeeId": "%s",
                "branchId": "%s",
                "speciality": "BRAKES",
                "specialityName": "Brake Tech",
                "salary": 2800.00,
                "role": "ROLE_EMPLOYEE"
            }
        """.formatted(employeeId, branchId);

        mockMvc.perform(post("/api/v1/employee-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(registrationId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("ROLE_EMPLOYEE"));
    }

    @Test
    @DisplayName("POST /api/v1/employee-registrations/request-join - Mechanic join request returns 201")
    void shouldRequestJoin() throws Exception {
        EmployeeRegistrationResult result = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "SUSPENSION", "Suspension Specialist",
                new BigDecimal("2700.00"), "ROLE_EMPLOYEE", "PENDING_APPROVAL", null, Instant.now(), null, Instant.now()
        );
        when(manageUseCase.requestJoin(any(RequestJoinEmployeeRegistrationCommand.class))).thenReturn(result);

        String payload = """
            {
                "employeeId": "%s",
                "branchId": "%s",
                "speciality": "SUSPENSION",
                "specialityName": "Suspension Specialist",
                "salary": 2700.00
            }
        """.formatted(employeeId, branchId);

        mockMvc.perform(post("/api/v1/employee-registrations/request-join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("POST /api/v1/employee-registrations/{id}/approve - Approve join request returns 200")
    void shouldApproveJoinRequest() throws Exception {
        EmployeeRegistrationResult pending = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "SUSPENSION", "Suspension Specialist",
                new BigDecimal("2700.00"), "ROLE_EMPLOYEE", "PENDING_APPROVAL", null, Instant.now(), null, Instant.now()
        );
        EmployeeRegistrationResult approved = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "SUSPENSION", "Suspension Specialist",
                new BigDecimal("2700.00"), "ROLE_EMPLOYEE", "ACTIVE", null, Instant.now(), Instant.now(), Instant.now()
        );
        when(getUseCase.getById(registrationId, tenantId)).thenReturn(pending);
        when(manageUseCase.approve(registrationId, tenantId)).thenReturn(approved);

        mockMvc.perform(post("/api/v1/employee-registrations/{id}/approve", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/employee-registrations/{id}/reject - Reject join request returns 200")
    void shouldRejectJoinRequest() throws Exception {
        EmployeeRegistrationResult pending = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "SUSPENSION", "Suspension Specialist",
                new BigDecimal("2700.00"), "ROLE_EMPLOYEE", "PENDING_APPROVAL", null, Instant.now(), null, Instant.now()
        );
        EmployeeRegistrationResult rejected = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "SUSPENSION", "Suspension Specialist",
                new BigDecimal("2700.00"), "ROLE_EMPLOYEE", "REJECTED", "No vacancies", Instant.now(), null, Instant.now()
        );
        when(getUseCase.getById(registrationId, tenantId)).thenReturn(pending);
        when(securityService.isAuthorizedForBranch(branchId)).thenReturn(true);
        when(manageUseCase.reject(registrationId, tenantId, "No vacancies")).thenReturn(rejected);

        mockMvc.perform(post("/api/v1/employee-registrations/{id}/reject", registrationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"No vacancies\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("No vacancies"));
    }

    @Test
    @DisplayName("GET /api/v1/employee-registrations - List with filters returns 200")
    void shouldListRegistrations() throws Exception {
        when(securityService.isAuthorizedForBranch(branchId)).thenReturn(true);
        EmployeeRegistrationResult result = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "BRAKES", "Brake Tech",
                new BigDecimal("2800.00"), "ROLE_EMPLOYEE", "ACTIVE", null, Instant.now(), null, Instant.now()
        );
        when(getUseCase.list(eq(tenantId), eq(branchId), isNull(), isNull())).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/employee-registrations").param("branchId", branchId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(registrationId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/employee-registrations/{id} - Returns detail by ID")
    void shouldGetById() throws Exception {
        EmployeeRegistrationResult result = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "BRAKES", "Brake Tech",
                new BigDecimal("2800.00"), "ROLE_EMPLOYEE", "ACTIVE", null, Instant.now(), null, Instant.now()
        );
        when(getUseCase.getById(registrationId, tenantId)).thenReturn(result);
        when(securityService.isAuthorizedForBranch(branchId)).thenReturn(true);

        mockMvc.perform(get("/api/v1/employee-registrations/{id}", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(registrationId.toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/employee-registrations/{id} - Update specialty and salary")
    void shouldUpdateSpecialtyAndSalary() throws Exception {
        EmployeeRegistrationResult existing = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "BRAKES", "Brake Tech",
                new BigDecimal("2800.00"), "ROLE_EMPLOYEE", "ACTIVE", null, Instant.now(), null, Instant.now()
        );
        EmployeeRegistrationResult updated = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "SUSPENSION", "Suspension Lead",
                new BigDecimal("3300.00"), "ROLE_EMPLOYEE", "ACTIVE", null, Instant.now(), null, Instant.now()
        );
        when(getUseCase.getById(registrationId, tenantId)).thenReturn(existing);
        when(securityService.isAuthorizedForBranch(branchId)).thenReturn(true);
        when(manageUseCase.update(any(UpdateEmployeeRegistrationCommand.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/employee-registrations/{id}", registrationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"speciality\": \"SUSPENSION\", \"specialityName\": \"Suspension Lead\", \"salary\": 3300.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speciality").value("SUSPENSION"))
                .andExpect(jsonPath("$.salary").value(3300.00));
    }

    @Test
    @DisplayName("DELETE /api/v1/employee-registrations/{id} - Soft deletes employee registration returning 204")
    void shouldDeactivate() throws Exception {
        EmployeeRegistrationResult existing = new EmployeeRegistrationResult(
                registrationId, tenantId, branchId, employeeId, "BRAKES", "Brake Tech",
                new BigDecimal("2800.00"), "ROLE_EMPLOYEE", "ACTIVE", null, Instant.now(), null, Instant.now()
        );
        when(getUseCase.getById(registrationId, tenantId)).thenReturn(existing);
        when(securityService.isAuthorizedForBranch(branchId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/employee-registrations/{id}", registrationId))
                .andExpect(status().isNoContent());

        verify(manageUseCase).deactivate(registrationId, tenantId);
    }
}
