package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.command.RescheduleAppointmentCommand;
import com.erp.fleet.application.dto.command.ScheduleAppointmentCommand;
import com.erp.fleet.application.dto.result.AppointmentResult;
import com.erp.fleet.application.usecase.ManageAppointmentUseCase;
import com.erp.fleet.application.usecase.ScheduleAppointmentUseCase;
import com.erp.fleet.interfaces.rest.dto.request.CancelAppointmentRequest;
import com.erp.fleet.interfaces.rest.dto.request.CreateAppointmentRequest;
import com.erp.fleet.interfaces.rest.dto.request.UpdateAppointmentRequest;
import com.erp.fleet.interfaces.rest.dto.response.AppointmentResponse;
import com.erp.fleet.interfaces.rest.support.CurrentTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/fleet/appointments", "/api/v1/appointments"})
@Validated
@Tag(name = "Appointments", description = "Management of workshop vehicle appointments and check-in")
public class AppointmentController {

    private final ScheduleAppointmentUseCase scheduleAppointmentUseCase;
    private final ManageAppointmentUseCase manageAppointmentUseCase;

    public AppointmentController(ScheduleAppointmentUseCase scheduleAppointmentUseCase,
                                 ManageAppointmentUseCase manageAppointmentUseCase) {
        this.scheduleAppointmentUseCase = scheduleAppointmentUseCase;
        this.manageAppointmentUseCase = manageAppointmentUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Schedule appointment", description = "Books a new workshop appointment for customer and vehicle")
    public ResponseEntity<AppointmentResponse> schedule(@Valid @RequestBody CreateAppointmentRequest request) {
        ScheduleAppointmentCommand command = new ScheduleAppointmentCommand(
                CurrentTenant.id(),
                request.branchId(),
                request.customerId(),
                request.vehicleId(),
                request.scheduledStart(),
                request.scheduledEnd(),
                request.notes()
        );
        AppointmentResult result = scheduleAppointmentUseCase.schedule(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentResponse.from(result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    @Operation(summary = "List appointments", description = "Lists workshop appointments filtered by branch, customer, vehicle, or status")
    public ResponseEntity<List<AppointmentResponse>> list(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) String status
    ) {
        List<AppointmentResult> results = manageAppointmentUseCase.list(
                CurrentTenant.id(), branchId, customerId, vehicleId, status
        );
        return ResponseEntity.ok(results.stream().map(AppointmentResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable UUID id) {
        AppointmentResult result = manageAppointmentUseCase.getById(id, CurrentTenant.id());
        return ResponseEntity.ok(AppointmentResponse.from(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Reschedule appointment")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentRequest request
    ) {
        RescheduleAppointmentCommand command = new RescheduleAppointmentCommand(
                id,
                CurrentTenant.id(),
                request.branchId(),
                request.scheduledStart(),
                request.scheduledEnd(),
                request.notes()
        );
        AppointmentResult result = manageAppointmentUseCase.reschedule(command);
        return ResponseEntity.ok(AppointmentResponse.from(result));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Confirm scheduled appointment")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable UUID id) {
        AppointmentResult result = manageAppointmentUseCase.confirm(id, CurrentTenant.id());
        return ResponseEntity.ok(AppointmentResponse.from(result));
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Check in vehicle at workshop arrival")
    public ResponseEntity<AppointmentResponse> checkIn(@PathVariable UUID id) {
        AppointmentResult result = manageAppointmentUseCase.checkIn(id, CurrentTenant.id());
        return ResponseEntity.ok(AppointmentResponse.from(result));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Complete appointment after service delivery")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable UUID id) {
        AppointmentResult result = manageAppointmentUseCase.complete(id, CurrentTenant.id());
        return ResponseEntity.ok(AppointmentResponse.from(result));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Cancel appointment with reason")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelAppointmentRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        AppointmentResult result = manageAppointmentUseCase.cancel(id, CurrentTenant.id(), reason);
        return ResponseEntity.ok(AppointmentResponse.from(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Cancel appointment by DELETE", description = "Cancels active appointment and frees up slot")
    public ResponseEntity<Void> cancelByDelete(@PathVariable UUID id) {
        manageAppointmentUseCase.cancel(id, CurrentTenant.id(), "Cancelled via REST DELETE");
        return ResponseEntity.noContent().build();
    }
}
