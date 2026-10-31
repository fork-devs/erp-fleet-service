package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.command.CreateCustomerRegistrationCommand;
import com.erp.fleet.application.dto.command.UpdateCustomerRegistrationStatusCommand;
import com.erp.fleet.application.dto.result.CustomerRegistrationResult;
import com.erp.fleet.application.usecase.GetCustomerRegistrationsUseCase;
import com.erp.fleet.application.usecase.ManageCustomerRegistrationUseCase;
import com.erp.fleet.domain.exception.CustomerRegistrationNotFoundException;
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
class CustomerRegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageCustomerRegistrationUseCase manageUseCase;

    @Mock
    private GetCustomerRegistrationsUseCase getUseCase;

    @Mock
    private MultiTenancySecurityService securityService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID registrationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(tenantId);
        CustomerRegistrationController controller = new CustomerRegistrationController(
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
    @DisplayName("POST /api/v1/customer-registrations - Register customer returns 201")
    void shouldRegisterCustomer() throws Exception {
        CustomerRegistrationResult result = new CustomerRegistrationResult(
                registrationId, tenantId, branchId, customerId, "ACTIVE", Instant.now(), Instant.now()
        );
        when(manageUseCase.register(any(CreateCustomerRegistrationCommand.class))).thenReturn(result);

        String payload = """
            {
                "branchId": "%s",
                "customerId": "%s"
            }
        """.formatted(branchId, customerId);

        mockMvc.perform(post("/api/v1/customer-registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(registrationId.toString()))
                .andExpect(jsonPath("$.branchId").value(branchId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/customer-registrations - Without filters returns 400 Bad Request")
    void shouldRejectListWithoutFilters() throws Exception {
        mockMvc.perform(get("/api/v1/customer-registrations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FLEET_VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/v1/customer-registrations - With branch filter returns 200 OK")
    void shouldListWithFilters() throws Exception {
        when(securityService.isAuthorizedForBranch(branchId)).thenReturn(true);
        CustomerRegistrationResult result = new CustomerRegistrationResult(
                registrationId, tenantId, branchId, customerId, "ACTIVE", Instant.now(), Instant.now()
        );
        when(getUseCase.list(eq(tenantId), eq(branchId), isNull(), isNull())).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/customer-registrations").param("branchId", branchId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(registrationId.toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/customer-registrations/{id} - Returns registration by ID")
    void shouldGetById() throws Exception {
        CustomerRegistrationResult result = new CustomerRegistrationResult(
                registrationId, tenantId, branchId, customerId, "ACTIVE", Instant.now(), Instant.now()
        );
        when(getUseCase.getById(registrationId, tenantId)).thenReturn(result);
        when(securityService.isAuthorizedForBranch(branchId)).thenReturn(true);

        mockMvc.perform(get("/api/v1/customer-registrations/{id}", registrationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(registrationId.toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/customer-registrations/{id} - Updates status")
    void shouldUpdateStatus() throws Exception {
        CustomerRegistrationResult existing = new CustomerRegistrationResult(
                registrationId, tenantId, branchId, customerId, "ACTIVE", Instant.now(), Instant.now()
        );
        CustomerRegistrationResult updated = new CustomerRegistrationResult(
                registrationId, tenantId, branchId, customerId, "INACTIVE", Instant.now(), Instant.now()
        );
        when(getUseCase.getById(registrationId, tenantId)).thenReturn(existing);
        when(securityService.isAuthorizedForBranch(branchId)).thenReturn(true);
        when(manageUseCase.updateStatus(any(UpdateCustomerRegistrationStatusCommand.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/customer-registrations/{id}", registrationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("DELETE /api/v1/customer-registrations/{id} - Soft deletes returning 204")
    void shouldDeactivate() throws Exception {
        CustomerRegistrationResult existing = new CustomerRegistrationResult(
                registrationId, tenantId, branchId, customerId, "ACTIVE", Instant.now(), Instant.now()
        );
        when(getUseCase.getById(registrationId, tenantId)).thenReturn(existing);
        when(securityService.isAuthorizedForBranch(branchId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/customer-registrations/{id}", registrationId))
                .andExpect(status().isNoContent());

        verify(manageUseCase).deactivate(registrationId, tenantId);
    }
}
