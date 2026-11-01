package com.erp.fleet.interfaces.rest;

import com.erp.fleet.application.dto.command.RegisterCustomerCommand;
import com.erp.fleet.application.dto.command.RegisterVehicleCommand;
import com.erp.fleet.application.dto.command.ScheduleAppointmentCommand;
import com.erp.fleet.application.dto.result.AppointmentResult;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.dto.result.VehicleResult;
import com.erp.fleet.application.usecase.RegisterCustomerUseCase;
import com.erp.fleet.application.usecase.RegisterVehicleUseCase;
import com.erp.fleet.application.usecase.ScheduleAppointmentUseCase;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import com.erp.fleet.support.AbstractPostgresIntegrationTest;
import com.erp.fleet.support.TestJwksServer;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FleetRegistrationsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisterCustomerUseCase registerCustomerUseCase;

    @Autowired
    private RegisterVehicleUseCase registerVehicleUseCase;

    @Autowired
    private ScheduleAppointmentUseCase scheduleAppointmentUseCase;

    private UUID tenantId;
    private UUID branchId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private String adminToken() {
        return TestJwksServer.signToken(tenantId, List.of("ADMIN_TALLER", "WORKSHOP_ADMIN"));
    }

    private String mechanicToken() {
        return TestJwksServer.signToken(tenantId, List.of("EMPLOYEE", "MECANICO"));
    }

    @Test
    @DisplayName("End-to-end integration: customer registrations, staff onboarding, career history and appointment deletion")
    void testFleetRegistrationsAndAppointments() throws Exception {
        TenantContextHolder.setTenantId(tenantId);
        CustomerResult customer = registerCustomerUseCase.register(new RegisterCustomerCommand(
                tenantId, "Carlos Mendoza", "carlos@example.com", "987654321", "DNI", "45678912"
        ));
        VehicleResult vehicle = registerVehicleUseCase.register(new RegisterVehicleCommand(
                tenantId, customer.id(), "XYZ-123", "Toyota", "Hilux", 2022, null
        ));
        TenantContextHolder.clear();

        // 1. Customer registration at branch
        String custRegPayload = """
            {
                "branchId": "%s",
                "customerId": "%s"
            }
        """.formatted(branchId, customer.id());

        MvcResult custRegResult = mockMvc.perform(post("/api/v1/customer-registrations")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(custRegPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String custRegId = JsonPath.read(custRegResult.getResponse().getContentAsString(), "$.id");

        // 2. Query customer registrations with branch filter
        mockMvc.perform(get("/api/v1/customer-registrations")
                        .header("Authorization", "Bearer " + adminToken())
                        .param("branchId", branchId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(custRegId));

        // 3. Direct employee onboarding by admin
        String empRegPayload = """
            {
                "employeeId": "%s",
                "branchId": "%s",
                "speciality": "BRAKES",
                "specialityName": "Brake Systems Specialist",
                "salary": 3200.00,
                "role": "ROLE_EMPLOYEE"
            }
        """.formatted(employeeId, branchId);

        MvcResult empRegResult = mockMvc.perform(post("/api/v1/employee-registrations")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(empRegPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String empRegId = JsonPath.read(empRegResult.getResponse().getContentAsString(), "$.id");

        // 4. Update employee specialty and salary
        String updateEmpPayload = """
            {
                "speciality": "SUSPENSION",
                "specialityName": "Master Brake & Suspension Tech",
                "salary": 3600.00
            }
        """;

        mockMvc.perform(put("/api/v1/employee-registrations/{id}", empRegId)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateEmpPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speciality").value("SUSPENSION"))
                .andExpect(jsonPath("$.salary").value(3600.00));

        // 5. Query career history for the employee in branch
        mockMvc.perform(get("/api/v1/branches/{branchId}/employees/{employeeId}/history", branchId, employeeId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].speciality").value("BRAKES"))
                .andExpect(jsonPath("$[1].speciality").value("SUSPENSION"));

        // 6. Request-join by another mechanic
        UUID applicantId = UUID.randomUUID();
        String requestJoinPayload = """
            {
                "employeeId": "%s",
                "branchId": "%s",
                "speciality": "ELECTRICIAN",
                "specialityName": "Auto Electrician",
                "salary": 2900.00
            }
        """.formatted(applicantId, branchId);

        MvcResult joinResult = mockMvc.perform(post("/api/v1/employee-registrations/request-join")
                        .header("Authorization", "Bearer " + mechanicToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJoinPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn();

        String applicantRegId = JsonPath.read(joinResult.getResponse().getContentAsString(), "$.id");

        // 7. Approve mechanic join request
        mockMvc.perform(post("/api/v1/employee-registrations/{id}/approve", applicantRegId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 8. Schedule an appointment and delete via DELETE /api/v1/fleet/appointments/{id}
        TenantContextHolder.setTenantId(tenantId);
        AppointmentResult appointment = scheduleAppointmentUseCase.schedule(new ScheduleAppointmentCommand(
                tenantId, branchId, customer.id(), vehicle.id(),
                Instant.now().plusSeconds(7200), Instant.now().plusSeconds(10800), "Brake check"
        ));
        TenantContextHolder.clear();

        mockMvc.perform(delete("/api/v1/fleet/appointments/{id}", appointment.id())
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNoContent());

        // 9. Deactivate customer registration
        mockMvc.perform(delete("/api/v1/customer-registrations/{id}", custRegId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNoContent());
    }
}
