package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.result.StaffCareerHistoryResult;
import com.erp.fleet.application.usecase.GetStaffCareerHistoryUseCase;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import com.erp.fleet.interfaces.rest.advice.GlobalRestExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StaffCareerHistoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetStaffCareerHistoryUseCase getUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID historyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(tenantId);
        StaffCareerHistoryController controller = new StaffCareerHistoryController(getUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("GET /api/v1/branches/{branchId}/employees/{employeeId}/history - Returns career history list")
    void shouldReturnCareerHistory() throws Exception {
        StaffCareerHistoryResult history = new StaffCareerHistoryResult(
                historyId, tenantId, branchId, employeeId, "BRAKES", "Brake Tech",
                new BigDecimal("2900.00"), "ROLE_EMPLOYEE", "ACTIVE", "Promotion to Senior", Instant.now()
        );
        when(getUseCase.getHistory(eq(tenantId), eq(branchId), eq(employeeId))).thenReturn(List.of(history));

        mockMvc.perform(get("/api/v1/branches/{branchId}/employees/{employeeId}/history", branchId, employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(historyId.toString()))
                .andExpect(jsonPath("$[0].speciality").value("BRAKES"))
                .andExpect(jsonPath("$[0].salary").value(2900.00))
                .andExpect(jsonPath("$[0].changeReason").value("Promotion to Senior"));
    }
}
