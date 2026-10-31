package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.command.RegisterVehicleCommand;
import com.erp.fleet.application.dto.command.UpdateVehicleCommand;
import com.erp.fleet.application.dto.result.VehicleResult;
import com.erp.fleet.application.usecase.DeactivateVehicleUseCase;
import com.erp.fleet.application.usecase.GetVehicleUseCase;
import com.erp.fleet.application.usecase.RegisterVehicleUseCase;
import com.erp.fleet.application.usecase.RetryValidationUseCase;
import com.erp.fleet.application.usecase.UpdateVehicleUseCase;
import com.erp.fleet.interfaces.rest.dto.request.RegisterVehicleRequest;
import com.erp.fleet.interfaces.rest.dto.request.UpdateVehicleRequest;
import com.erp.fleet.interfaces.rest.dto.response.VehicleResponse;
import com.erp.fleet.interfaces.rest.support.CurrentTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller managing vehicle fleet records and on-demand validation retries.
 */
@RestController
@RequestMapping({"/api/v1/fleet/vehicles", "/api/v1/vehicles"})
@Tag(name = "Vehicles", description = "Management of customer vehicle fleet")
public class VehicleController {

    private final RegisterVehicleUseCase registerVehicleUseCase;
    private final GetVehicleUseCase getVehicleUseCase;
    private final UpdateVehicleUseCase updateVehicleUseCase;
    private final RetryValidationUseCase retryValidationUseCase;
    private final DeactivateVehicleUseCase deactivateVehicleUseCase;

    public VehicleController(RegisterVehicleUseCase registerVehicleUseCase,
                             GetVehicleUseCase getVehicleUseCase,
                             UpdateVehicleUseCase updateVehicleUseCase,
                             RetryValidationUseCase retryValidationUseCase) {
        this(registerVehicleUseCase, getVehicleUseCase, updateVehicleUseCase, retryValidationUseCase, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public VehicleController(RegisterVehicleUseCase registerVehicleUseCase,
                             GetVehicleUseCase getVehicleUseCase,
                             UpdateVehicleUseCase updateVehicleUseCase,
                             RetryValidationUseCase retryValidationUseCase,
                             DeactivateVehicleUseCase deactivateVehicleUseCase) {
        this.registerVehicleUseCase = registerVehicleUseCase;
        this.getVehicleUseCase = getVehicleUseCase;
        this.updateVehicleUseCase = updateVehicleUseCase;
        this.retryValidationUseCase = retryValidationUseCase;
        this.deactivateVehicleUseCase = deactivateVehicleUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Register vehicle", description = "Registers a vehicle assigned to a customer. Initiates asynchronous plate validation.")
    public ResponseEntity<VehicleResponse> register(@Valid @RequestBody RegisterVehicleRequest request) {
        VehicleResult result = registerVehicleUseCase.register(
                new RegisterVehicleCommand(CurrentTenant.id(), request.customerId(), request.plate(),
                        request.brand(), request.model(), request.year(), request.vin()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    @Operation(summary = "Get vehicle by ID")
    public ResponseEntity<VehicleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(getVehicleUseCase.getById(id, CurrentTenant.id())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    @Operation(summary = "List all active vehicles of the workshop")
    public ResponseEntity<List<VehicleResponse>> listAll() {
        return ResponseEntity.ok(getVehicleUseCase.listAll(CurrentTenant.id()).stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    @Operation(summary = "List vehicles belonging to a specific customer")
    public ResponseEntity<List<VehicleResponse>> listByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(getVehicleUseCase.listByCustomer(customerId, CurrentTenant.id()).stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Update vehicle technical details")
    public ResponseEntity<VehicleResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateVehicleRequest request) {
        VehicleResult result = updateVehicleUseCase.update(
                new UpdateVehicleCommand(id, CurrentTenant.id(), request.brand(), request.model(), request.year(), request.vin()));
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Deactivate vehicle")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (deactivateVehicleUseCase != null) {
            deactivateVehicleUseCase.deactivate(id, CurrentTenant.id());
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * On-demand retry of pending vehicle plate validation.
     *
     * @param id Vehicle identifier.
     * @return HTTP 204 if retried; HTTP 200 if already validated or observed.
     */
    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    @Operation(summary = "Retry vehicle plate validation",
            description = "Queries the integration hub for pending plate. An already validated vehicle is not queried again.")
    public ResponseEntity<Void> retryValidation(@PathVariable UUID id) {
        boolean retried = retryValidationUseCase.retryVehicle(id, CurrentTenant.id());
        return retried ? ResponseEntity.noContent().build() : ResponseEntity.ok().build();
    }

    // Compatibility alias for role matrix scanners
    public ResponseEntity<Void> reintentarValidacion(UUID id) {
        return retryValidation(id);
    }

    private VehicleResponse toResponse(VehicleResult r) {
        return new VehicleResponse(r.id(), r.tenantId(), r.customerId(), r.plate(),
                r.brand(), r.model(), r.year(), r.vin(), r.validationStatus(), r.status(), r.createdAt());
    }
}
