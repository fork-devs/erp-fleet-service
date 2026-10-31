package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.command.RescheduleAppointmentCommand;
import com.erp.fleet.application.dto.command.ScheduleAppointmentCommand;
import com.erp.fleet.application.dto.result.AppointmentResult;
import com.erp.fleet.application.usecase.ManageAppointmentUseCase;
import com.erp.fleet.application.usecase.ScheduleAppointmentUseCase;
import com.erp.fleet.domain.exception.AppointmentConflictException;
import com.erp.fleet.domain.exception.AppointmentNotFoundException;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ScheduleAppointmentUseCase scheduleAppointmentUseCase;

    @Mock
    private ManageAppointmentUseCase manageAppointmentUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(tenantId);
        AppointmentController controller = new AppointmentController(scheduleAppointmentUseCase, manageAppointmentUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("POST /api/v1/fleet/appointments - Successfully schedule appointment")
    void shouldScheduleAppointment() throws Exception {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        AppointmentResult result = new AppointmentResult(
                appointmentId, tenantId, branchId, customerId, vehicleId,
                start, end, "SCHEDULED", "Regular checkup", null, Instant.now(), Instant.now()
        );

        when(scheduleAppointmentUseCase.schedule(any(ScheduleAppointmentCommand.class))).thenReturn(result);

        String payload = String.format("""
            {
                "branchId": "%s",
                "customerId": "%s",
                "vehicleId": "%s",
                "scheduledStart": "%s",
                "scheduledEnd": "%s",
                "notes": "Regular checkup"
            }
            """, branchId, customerId, vehicleId, start, end);

        mockMvc.perform(post("/api/v1/fleet/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.notes").value("Regular checkup"));
    }

    @Test
    @DisplayName("POST /api/v1/fleet/appointments - Conflict on overlap returns 409")
    void shouldReturnConflictOnOverlap() throws Exception {
        Instant start = Instant.now().plusSeconds(3600);

        when(scheduleAppointmentUseCase.schedule(any(ScheduleAppointmentCommand.class)))
                .thenThrow(new AppointmentConflictException("Solapamiento de horario"));

        String payload = String.format("""
            {
                "branchId": "%s",
                "customerId": "%s",
                "vehicleId": "%s",
                "scheduledStart": "%s"
            }
            """, branchId, customerId, vehicleId, start);

        mockMvc.perform(post("/api/v1/fleet/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FLEET_APPOINTMENT_CONFLICT"));
    }

    @Test
    @DisplayName("POST /api/v1/fleet/appointments - Missing branchId returns 400")
    void shouldReturnBadRequestOnValidationFailure() throws Exception {
        String payload = String.format("""
            {
                "customerId": "%s",
                "vehicleId": "%s",
                "scheduledStart": "%s"
            }
            """, customerId, vehicleId, Instant.now().plusSeconds(3600));

        mockMvc.perform(post("/api/v1/fleet/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/fleet/appointments - List appointments")
    void shouldListAppointments() throws Exception {
        Instant start = Instant.now().plusSeconds(3600);
        AppointmentResult result = new AppointmentResult(
                appointmentId, tenantId, branchId, customerId, vehicleId,
                start, start.plusSeconds(3600), "SCHEDULED", "Notes", null, Instant.now(), Instant.now()
        );

        when(manageAppointmentUseCase.list(eq(tenantId), eq(branchId), eq(null), eq(null), eq("SCHEDULED")))
                .thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/fleet/appointments")
                        .param("branchId", branchId.toString())
                        .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(appointmentId.toString()))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("GET /api/v1/fleet/appointments/{id} - Get appointment by ID")
    void shouldGetAppointmentById() throws Exception {
        Instant start = Instant.now().plusSeconds(3600);
        AppointmentResult result = new AppointmentResult(
                appointmentId, tenantId, branchId, customerId, vehicleId,
                start, start.plusSeconds(3600), "CONFIRMED", "Notes", null, Instant.now(), Instant.now()
        );

        when(manageAppointmentUseCase.getById(appointmentId, tenantId)).thenReturn(result);

        mockMvc.perform(get("/api/v1/fleet/appointments/{id}", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/v1/fleet/appointments/{id} - Not found returns 404")
    void shouldReturnNotFoundWhenAppointmentMissing() throws Exception {
        when(manageAppointmentUseCase.getById(appointmentId, tenantId))
                .thenThrow(new AppointmentNotFoundException(appointmentId.toString()));

        mockMvc.perform(get("/api/v1/fleet/appointments/{id}", appointmentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FLEET_APPOINTMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("PUT /api/v1/fleet/appointments/{id} - Reschedule appointment")
    void shouldRescheduleAppointment() throws Exception {
        Instant newStart = Instant.now().plusSeconds(7200);
        Instant newEnd = newStart.plusSeconds(3600);

        AppointmentResult result = new AppointmentResult(
                appointmentId, tenantId, branchId, customerId, vehicleId,
                newStart, newEnd, "SCHEDULED", "Rescheduled", null, Instant.now(), Instant.now()
        );

        when(manageAppointmentUseCase.reschedule(any(RescheduleAppointmentCommand.class))).thenReturn(result);

        String payload = String.format("""
            {
                "branchId": "%s",
                "scheduledStart": "%s",
                "scheduledEnd": "%s",
                "notes": "Rescheduled"
            }
            """, branchId, newStart, newEnd);

        mockMvc.perform(put("/api/v1/fleet/appointments/{id}", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                .andExpect(jsonPath("$.notes").value("Rescheduled"));
    }

    @Test
    @DisplayName("POST /api/v1/fleet/appointments/{id}/confirm - Confirm appointment")
    void shouldConfirmAppointment() throws Exception {
        AppointmentResult result = new AppointmentResult(
                appointmentId, tenantId, branchId, customerId, vehicleId,
                Instant.now(), Instant.now().plusSeconds(3600), "CONFIRMED", null, null, Instant.now(), Instant.now()
        );

        when(manageAppointmentUseCase.confirm(appointmentId, tenantId)).thenReturn(result);

        mockMvc.perform(post("/api/v1/fleet/appointments/{id}/confirm", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/v1/fleet/appointments/{id}/check-in - Check in vehicle")
    void shouldCheckInVehicle() throws Exception {
        AppointmentResult result = new AppointmentResult(
                appointmentId, tenantId, branchId, customerId, vehicleId,
                Instant.now(), Instant.now().plusSeconds(3600), "IN_PROGRESS", null, null, Instant.now(), Instant.now()
        );

        when(manageAppointmentUseCase.checkIn(appointmentId, tenantId)).thenReturn(result);

        mockMvc.perform(post("/api/v1/fleet/appointments/{id}/check-in", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("POST /api/v1/fleet/appointments/{id}/complete - Complete appointment")
    void shouldCompleteAppointment() throws Exception {
        AppointmentResult result = new AppointmentResult(
                appointmentId, tenantId, branchId, customerId, vehicleId,
                Instant.now(), Instant.now().plusSeconds(3600), "COMPLETED", null, null, Instant.now(), Instant.now()
        );

        when(manageAppointmentUseCase.complete(appointmentId, tenantId)).thenReturn(result);

        mockMvc.perform(post("/api/v1/fleet/appointments/{id}/complete", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/fleet/appointments/{id}/cancel - Cancel appointment with reason")
    void shouldCancelAppointment() throws Exception {
        AppointmentResult result = new AppointmentResult(
                appointmentId, tenantId, branchId, customerId, vehicleId,
                Instant.now(), Instant.now().plusSeconds(3600), "CANCELLED", null, "Client requested cancellation", Instant.now(), Instant.now()
        );

        when(manageAppointmentUseCase.cancel(appointmentId, tenantId, "Client requested cancellation")).thenReturn(result);

        mockMvc.perform(post("/api/v1/fleet/appointments/{id}/cancel", appointmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Client requested cancellation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Client requested cancellation"));
    }
}
