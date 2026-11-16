package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.service.WorkshopAppointmentResources;
import com.erp.fleet.domain.port.WorkshopBayRepositoryPort.Bay;
import com.erp.fleet.infrastructure.client.BranchDirectory;
import com.erp.fleet.interfaces.rest.support.CurrentTenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fleet/appointments/resources")
public class WorkshopAppointmentResourcesController {
    private final WorkshopAppointmentResources resources;
    private final BranchDirectory branches;

    public WorkshopAppointmentResourcesController(WorkshopAppointmentResources resources, BranchDirectory branches) {
        this.resources = resources;
        this.branches = branches;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public WorkshopAppointmentResources.Options list(@RequestParam UUID branchId) {
        return resources.list(CurrentTenant.id(), branchId);
    }

    @PutMapping("/bays/{number}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER')")
    public Bay save(
            @RequestParam UUID branchId, @PathVariable @Positive int number,
            @Valid @RequestBody BayName request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        UUID tenant = CurrentTenant.id();
        branches.verify(tenant, branchId, authorization);
        return resources.saveBay(tenant, branchId, number, request.name());
    }

    public record BayName(@NotBlank @Size(max = 100) String name) {}
}
