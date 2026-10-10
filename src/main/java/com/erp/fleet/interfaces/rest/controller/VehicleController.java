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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @Operation(summary = "Registrar vehículo", description = "Crea el vehículo con validación asíncrona de placa vía integration-hub")
    public ResponseEntity<VehicleResponse> register(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Valid @RequestBody RegisterVehicleRequest request) {
        VehicleResult result = registerVehicleUseCase.register(
                new RegisterVehicleCommand(tenantId, request.customerId(), request.plate(),
                        request.brand(), request.model(), request.year(), request.vin()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping
    @Operation(summary = "Listar vehículos del taller")
    public ResponseEntity<List<VehicleResponse>> listAll(@RequestHeader("X-Tenant-ID") UUID tenantId) {
        return ResponseEntity.ok(getVehicleUseCase.listAll(tenantId).stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener vehículo por ID")
    public ResponseEntity<VehicleResponse> getById(
            @RequestHeader("X-Tenant-ID") UUID tenantId, @PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(getVehicleUseCase.getById(id, tenantId)));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Listar vehículos de un cliente")
    public ResponseEntity<List<VehicleResponse>> listByCustomer(
            @RequestHeader("X-Tenant-ID") UUID tenantId, @PathVariable UUID customerId) {
        return ResponseEntity.ok(getVehicleUseCase.listByCustomer(customerId, tenantId).stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos del vehículo")
    public ResponseEntity<VehicleResponse> update(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id,
            @RequestBody UpdateVehicleRequest request) {
        VehicleResult result = updateVehicleUseCase.update(
                new UpdateVehicleCommand(id, tenantId, request.brand(), request.model(), request.year(), request.vin()));
        return ResponseEntity.ok(toResponse(result));
    }

    private VehicleResponse toResponse(VehicleResult r) {
        return new VehicleResponse(r.id(), r.tenantId(), r.customerId(), r.plate(),
                r.brand(), r.model(), r.year(), r.vin(), r.validationStatus(), r.status(), r.createdAt());
    }
}
