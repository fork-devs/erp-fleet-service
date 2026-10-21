package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.command.RegisterVehicleCommand;
import com.erp.fleet.application.dto.command.UpdateVehicleCommand;
import com.erp.fleet.application.dto.result.VehicleResult;
import com.erp.fleet.application.usecase.GetVehicleUseCase;
import com.erp.fleet.application.usecase.RegisterVehicleUseCase;
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
 * Vehiculos del taller.
 *
 * <p>Cada endpoint declara el rol que exige. El taller lo decide el token
 * verificado: la cabecera {@code X-Tenant-ID} es una pista que el filtro
 * contrasta con el token.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Gestión de vehículos del taller")
public class VehicleController {

    private final RegisterVehicleUseCase registerVehicleUseCase;
    private final GetVehicleUseCase getVehicleUseCase;
    private final UpdateVehicleUseCase updateVehicleUseCase;

    public VehicleController(RegisterVehicleUseCase registerVehicleUseCase,
                              GetVehicleUseCase getVehicleUseCase,
                              UpdateVehicleUseCase updateVehicleUseCase) {
        this.registerVehicleUseCase = registerVehicleUseCase;
        this.getVehicleUseCase = getVehicleUseCase;
        this.updateVehicleUseCase = updateVehicleUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Registrar vehículo", description = "Crea el vehículo con validación asíncrona de placa vía integration-hub")
    public ResponseEntity<VehicleResponse> register(
            @Valid @RequestBody RegisterVehicleRequest request) {
        VehicleResult result = registerVehicleUseCase.register(
                new RegisterVehicleCommand(CurrentTenant.id(), request.customerId(), request.plate(),
                        request.brand(), request.model(), request.year(), request.vin()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    @Operation(summary = "Listar vehículos del taller")
    public ResponseEntity<List<VehicleResponse>> listAll() {
        return ResponseEntity.ok(getVehicleUseCase.listAll(CurrentTenant.id()).stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    @Operation(summary = "Obtener vehículo por ID")
    public ResponseEntity<VehicleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(getVehicleUseCase.getById(id, CurrentTenant.id())));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    @Operation(summary = "Listar vehículos de un cliente")
    public ResponseEntity<List<VehicleResponse>> listByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(getVehicleUseCase.listByCustomer(customerId, CurrentTenant.id()).stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Actualizar datos del vehículo")
    public ResponseEntity<VehicleResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateVehicleRequest request) {
        VehicleResult result = updateVehicleUseCase.update(
                new UpdateVehicleCommand(id, CurrentTenant.id(), request.brand(), request.model(), request.year(), request.vin()));
        return ResponseEntity.ok(toResponse(result));
    }

    private VehicleResponse toResponse(VehicleResult r) {
        return new VehicleResponse(r.id(), r.tenantId(), r.customerId(), r.plate(),
                r.brand(), r.model(), r.year(), r.vin(), r.validationStatus(), r.status(), r.createdAt());
    }
}
