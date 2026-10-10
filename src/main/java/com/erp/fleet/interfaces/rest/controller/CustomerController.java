package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.command.RegisterCustomerCommand;
import com.erp.fleet.application.dto.command.UpdateCustomerCommand;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.usecase.DeactivateCustomerUseCase;
import com.erp.fleet.application.usecase.GetCustomerUseCase;
import com.erp.fleet.application.usecase.RegisterCustomerUseCase;
import com.erp.fleet.application.usecase.UpdateCustomerUseCase;
import com.erp.fleet.interfaces.rest.dto.request.RegisterCustomerRequest;
import com.erp.fleet.interfaces.rest.dto.request.UpdateCustomerRequest;
import com.erp.fleet.interfaces.rest.dto.response.CustomerResponse;
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
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Gestión de clientes del taller")
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final DeactivateCustomerUseCase deactivateCustomerUseCase;

    public CustomerController(RegisterCustomerUseCase registerCustomerUseCase,
                               GetCustomerUseCase getCustomerUseCase,
                               UpdateCustomerUseCase updateCustomerUseCase,
                               DeactivateCustomerUseCase deactivateCustomerUseCase) {
        this.registerCustomerUseCase = registerCustomerUseCase;
        this.getCustomerUseCase = getCustomerUseCase;
        this.updateCustomerUseCase = updateCustomerUseCase;
        this.deactivateCustomerUseCase = deactivateCustomerUseCase;
    }

    @PostMapping
    @Operation(summary = "Registrar un nuevo cliente", description = "Crea el cliente con validación asíncrona de documento vía integration-hub")
    public ResponseEntity<CustomerResponse> register(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Valid @RequestBody RegisterCustomerRequest request) {
        CustomerResult result = registerCustomerUseCase.register(
                new RegisterCustomerCommand(tenantId, request.fullName(), request.email(),
                        request.phone(), request.docType(), request.docNumber()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping
    @Operation(summary = "Listar clientes del taller")
    public ResponseEntity<List<CustomerResponse>> listAll(@RequestHeader("X-Tenant-ID") UUID tenantId) {
        return ResponseEntity.ok(getCustomerUseCase.listAll(tenantId).stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por ID")
    public ResponseEntity<CustomerResponse> getById(
            @RequestHeader("X-Tenant-ID") UUID tenantId, @PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(getCustomerUseCase.getById(id, tenantId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos del cliente")
    public ResponseEntity<CustomerResponse> update(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @PathVariable UUID id,
            @RequestBody UpdateCustomerRequest request) {
        CustomerResult result = updateCustomerUseCase.update(
                new UpdateCustomerCommand(id, tenantId, request.fullName(), request.email(), request.phone()));
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar cliente")
    public ResponseEntity<Void> deactivate(
            @RequestHeader("X-Tenant-ID") UUID tenantId, @PathVariable UUID id) {
        deactivateCustomerUseCase.deactivate(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    private CustomerResponse toResponse(CustomerResult r) {
        return new CustomerResponse(r.id(), r.tenantId(), r.fullName(), r.email(), r.phone(),
                r.docType(), r.docNumber(), r.validationStatus(), r.status(), r.createdAt());
    }
}
