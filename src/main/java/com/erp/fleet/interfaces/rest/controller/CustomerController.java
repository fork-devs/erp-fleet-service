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
 * Clientes del taller.
 *
 * <p>Cada endpoint declara el rol que exige. El taller lo decide el token
 * verificado: la cabecera {@code X-Tenant-ID} es una pista que el filtro
 * contrasta con el token.
 */
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
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Registrar un nuevo cliente", description = "Crea el cliente con validación asíncrona de documento vía integration-hub")
    public ResponseEntity<CustomerResponse> register(
            @Valid @RequestBody RegisterCustomerRequest request) {
        CustomerResult result = registerCustomerUseCase.register(
                new RegisterCustomerCommand(CurrentTenant.id(), request.fullName(), request.email(),
                        request.phone(), request.docType(), request.docNumber()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    @Operation(summary = "Listar clientes del taller")
    public ResponseEntity<List<CustomerResponse>> listAll() {
        return ResponseEntity.ok(getCustomerUseCase.listAll(CurrentTenant.id()).stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    @Operation(summary = "Obtener cliente por ID")
    public ResponseEntity<CustomerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(getCustomerUseCase.getById(id, CurrentTenant.id())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Actualizar datos de contacto del cliente")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateCustomerRequest request) {
        CustomerResult result = updateCustomerUseCase.update(
                new UpdateCustomerCommand(id, CurrentTenant.id(), request.fullName(), request.email(), request.phone()));
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER')")
    @Operation(summary = "Desactivar cliente", description = "Baja lógica: el registro se conserva")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        deactivateCustomerUseCase.deactivate(id, CurrentTenant.id());
        return ResponseEntity.noContent().build();
    }

    private CustomerResponse toResponse(CustomerResult r) {
        return new CustomerResponse(r.id(), r.tenantId(), r.fullName(), r.email(), r.phone(),
                r.docType(), r.docNumber(), r.validationStatus(), r.status(), r.createdAt());
    }
}
